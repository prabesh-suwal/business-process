package com.enterprise.memo.controller;

import com.enterprise.memo.dto.CreateCategoryRequest;
import com.enterprise.memo.dto.CreateTopicRequest;
import com.enterprise.memo.entity.MemoCategory;
import com.enterprise.memo.entity.MemoTopic;
import com.enterprise.memo.service.MemoConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class MemoConfigurationController {

    private final MemoConfigurationService configurationService;

    @PostMapping("/categories")
    public ResponseEntity<MemoCategory> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(configurationService.createCategory(request));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<MemoCategory>> getAllCategories() {
        return ResponseEntity.ok(configurationService.getAllCategories());
    }

    @PostMapping("/topics")
    public ResponseEntity<MemoTopic> createTopic(@Valid @RequestBody CreateTopicRequest request) {
        return ResponseEntity.ok(configurationService.createTopic(request));
    }

    @GetMapping("/categories/{categoryId}/topics")
    public ResponseEntity<List<MemoTopic>> getTopicsByCategory(@PathVariable String categoryId) {
        return ResponseEntity.ok(configurationService.getTopicsByCategory(categoryId));
    }
}
