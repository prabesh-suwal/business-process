package com.enterprise.memo.service;

import com.enterprise.memo.dto.CreateCategoryRequest;
import com.enterprise.memo.dto.CreateTopicRequest;
import com.enterprise.memo.entity.MemoCategory;
import com.enterprise.memo.entity.MemoTopic;
import com.enterprise.memo.repository.MemoCategoryRepository;
import com.enterprise.memo.repository.MemoTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MemoConfigurationService {

    private final MemoCategoryRepository categoryRepository;
    private final MemoTopicRepository topicRepository;

    public MemoCategory createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Category code already exists: " + request.getCode());
        }

        MemoCategory category = MemoCategory.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .accessPolicy(request.getAccessPolicy())
                .build();

        return categoryRepository.save(category);
    }

    public List<MemoCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    public MemoTopic createTopic(CreateTopicRequest request) {
        MemoCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (topicRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Topic code already exists: " + request.getCode());
        }

        MemoTopic topic = MemoTopic.builder()
                .category(category)
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .workflowTemplateId(request.getWorkflowTemplateId())
                .formDefinitionId(request.getFormDefinitionId())
                .contentTemplate(request.getContentTemplate())
                .numberingPattern(request.getNumberingPattern())
                .build();

        return topicRepository.save(topic);
    }

    public List<MemoTopic> getTopicsByCategory(String categoryId) {
        return topicRepository.findByCategoryId(java.util.UUID.fromString(categoryId));
    }
}
