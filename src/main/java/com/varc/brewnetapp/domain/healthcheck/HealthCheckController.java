package com.varc.brewnetapp.domain.healthcheck;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class HealthCheckController {
    @GetMapping
    @Operation(summary = "배포 테스트 처리 API")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("good!!!");
    }
}
