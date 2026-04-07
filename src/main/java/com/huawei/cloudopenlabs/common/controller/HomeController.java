package com.huawei.cloudopenlabs.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 首页控制器
 *
 * @author GNEEC LIVE
 * @version 27.0.1.1
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> index() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", "AI Studio Service");
        result.put("version", "0.0.1-SNAPSHOT");
        result.put("status", "running");
        result.put("time", LocalDateTime.now());
        result.put("message", "Welcome to AI Studio Service API");
        result.put("apiDocs", "/swagger-ui.html");
        result.put("endpoints", new String[]{
            "/api/agent",
            "/api/chat",
            "/api/dictionary",
            "/api/skill",
            "/api/workflow"
        });
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("time", LocalDateTime.now());
        return ResponseEntity.ok(result);
    }
}
