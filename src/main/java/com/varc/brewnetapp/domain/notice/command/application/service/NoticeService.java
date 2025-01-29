package com.varc.brewnetapp.domain.notice.command.application.service;

import com.varc.brewnetapp.common.S3ImageService;
import com.varc.brewnetapp.domain.member.command.domain.repository.MemberRepository;
import com.varc.brewnetapp.domain.notice.command.application.dto.CreateNoticeRequestDTO;
import com.varc.brewnetapp.domain.notice.command.application.dto.DeleteNoticeRequestDTO;
import com.varc.brewnetapp.domain.notice.command.application.dto.NoticeAlarmDTO;
import com.varc.brewnetapp.domain.notice.command.application.dto.UpdateNoticeRequestDTO;
import com.varc.brewnetapp.domain.notice.command.domain.aggregate.entity.Notice;
import com.varc.brewnetapp.domain.notice.command.domain.aggregate.entity.NoticeImage;
import com.varc.brewnetapp.domain.notice.command.domain.repository.NoticeImageRepository;
import com.varc.brewnetapp.domain.notice.command.domain.repository.NoticeRepositiory;
import com.varc.brewnetapp.domain.sse.service.SSEService;
import com.varc.brewnetapp.exception.InvalidDataException;
import com.varc.brewnetapp.exception.MemberNotFoundException;
import com.varc.brewnetapp.security.utility.JwtUtil;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service(value = "commandNoticeService")
public class NoticeService {

    private final NoticeRepositiory noticeRepositiory;
    private final NoticeImageRepository noticeImageRepository;
    private final JwtUtil jwtUtil;
    private final S3ImageService s3ImageService;
    private final MemberRepository memberRepository;
    private final SSEService sseService;

    @Autowired
    public NoticeService(NoticeRepositiory noticeRepositiory,
        NoticeImageRepository noticeImageRepository, JwtUtil jwtUtil, S3ImageService s3ImageService,
        MemberRepository memberRepository, SSEService sseService) {
        this.noticeRepositiory = noticeRepositiory;
        this.noticeImageRepository = noticeImageRepository;
        this.jwtUtil = jwtUtil;
        this.s3ImageService = s3ImageService;
        this.memberRepository = memberRepository;
        this.sseService = sseService;
    }


    @Transactional
    public void createNotice(CreateNoticeRequestDTO createNoticeRequestDTO,
        List<MultipartFile> image, String accessToken) {

        String loginId = jwtUtil.getLoginId(accessToken.replace("Bearer ", ""));

        Integer memberCode = memberRepository.findById(loginId)
            .orElseThrow(() -> new MemberNotFoundException("공지 작성자의 회원 정보가 없습니다.")).getMemberCode();

        Notice notice = Notice.builder()
            .title(createNoticeRequestDTO.getTitle())
            .content(createNoticeRequestDTO.getContent())
            .createdAt(LocalDateTime.now())
            .memberCode(memberCode)
            .active(true)
            .build();

        notice = noticeRepositiory.save(notice);

        if (image != null && !image.isEmpty() && image.size() > 0){
            for(MultipartFile file : image) {

                String s3Url = null;
                try {
                    s3Url = s3ImageService.upload(file);
                }catch (InvalidDataException e) {
                    throw new InvalidDataException("이미지를 저장할 수 없습니다");
                }

                if(s3Url == null)
                    throw new InvalidDataException("이미지가 저장되지 않았습니다");

                NoticeImage noticeImage = NoticeImage.builder()
                    .noticeCode(notice.getNoticeCode())
                    .imageUrl(s3Url)
                    .build();

                noticeImageRepository.save(noticeImage);
            }
        }

        sseService.sendToHq(memberCode, "공지", notice.getTitle() + " 공지를 올렸으니 확인바랍니다");

    }

    @Transactional
    public void updateNotice(UpdateNoticeRequestDTO updateNoticeRequestDTO,
        List<MultipartFile> image, String accessToken) {

        Notice notice = noticeRepositiory.findById(updateNoticeRequestDTO.getNoticeCode())
            .orElseThrow(() -> new InvalidDataException("공지사항 코드를 잘못 입력했습니다"));

        if(updateNoticeRequestDTO.getTitle() != null && !updateNoticeRequestDTO.getTitle().isEmpty())
            notice.setTitle(updateNoticeRequestDTO.getTitle());

        if (updateNoticeRequestDTO.getContent() != null && !updateNoticeRequestDTO.getContent().isEmpty())
            notice.setContent(updateNoticeRequestDTO.getContent());

        if (image != null && !image.isEmpty() && image.size() > 0){
            List<NoticeImage> imageList = noticeImageRepository.findByNoticeCode(notice.getNoticeCode());

            for(NoticeImage noticeImage : imageList)
                noticeImageRepository.delete(noticeImage);

            for(MultipartFile file : image) {

                String s3Url = null;
                try {
                    s3Url = s3ImageService.upload(file);
                }catch (InvalidDataException e) {
                    throw new InvalidDataException("이미지를 저장할 수 없습니다");
                }

                if(s3Url == null)
                    throw new InvalidDataException("이미지가 저장되지 않았습니다");

                NoticeImage noticeImage = NoticeImage.builder()
                    .noticeCode(notice.getNoticeCode())
                    .imageUrl(s3Url)
                    .build();

                noticeImageRepository.save(noticeImage);
            }
        }
    }

    @Transactional
    public void deleteNotice(DeleteNoticeRequestDTO deleteNoticeRequestDTO, String accessToken) {
        Notice notice = noticeRepositiory.findById(deleteNoticeRequestDTO.getNoticeCode())
            .orElseThrow(() -> new InvalidDataException("공지사항 코드를 잘못 입력했습니다"));

        notice.setActive(false);

        noticeRepositiory.save(notice);
    }


}
