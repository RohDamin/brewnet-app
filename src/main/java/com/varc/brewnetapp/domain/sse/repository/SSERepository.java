package com.varc.brewnetapp.domain.sse.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Repository
public class SSERepository {

    private final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter findById(Integer loginId) {
        return emitters.get(loginId);
    }

    public SseEmitter save(Integer memberCode, SseEmitter sseEmitter) {
        emitters.put(memberCode, sseEmitter);
        return emitters.get(memberCode);
    }

    public void deleteById(Integer memberCode) {
        emitters.remove(memberCode);
    }
}
