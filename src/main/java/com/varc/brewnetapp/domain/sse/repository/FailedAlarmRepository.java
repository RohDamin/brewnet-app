package com.varc.brewnetapp.domain.sse.repository;

import com.varc.brewnetapp.domain.sse.dto.AlarmDTO;
import com.varc.brewnetapp.domain.sse.dto.RedisAlarmDTO;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FailedAlarmRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public FailedAlarmRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String getKey(Integer memberCode) {
        return "failed_alarms:" + memberCode;
    }

    // 알람 저장
    public void saveFailedAlarm(Integer memberCode, RedisAlarmDTO alarmData) {
        redisTemplate.opsForList().rightPush(getKey(memberCode), alarmData);
    }

    // 실패한 알람 조회
    public List<RedisAlarmDTO> getFailedAlarms(Integer memberCode) {
        // Redis에서 저장된 알림 데이터를 가져옴
        List<Object> alarmList = redisTemplate.opsForList().range(getKey(memberCode), 0, -1);

        // AlarmData로 변환하여 반환
        return alarmList.stream()
            .map(obj -> (RedisAlarmDTO) obj) // 역직렬화
            .collect(Collectors.toList());
    }

    // 특정 회원의 실패한 알람 삭제
    public void clearFailedAlarms(Integer memberCode) {
        redisTemplate.delete(getKey(memberCode));
    }
}


