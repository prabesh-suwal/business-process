package com.enterprise.memo.service;

import com.enterprise.memo.dto.CreateMemoRequest;
import com.enterprise.memo.dto.MemoDTO;
import com.enterprise.memo.dto.UpdateMemoRequest;
import com.enterprise.memo.entity.Memo;
import com.enterprise.memo.entity.MemoStatus;
import com.enterprise.memo.entity.MemoTopic;
import com.enterprise.memo.repository.MemoCategoryRepository;
import com.enterprise.memo.repository.MemoRepository;
import com.enterprise.memo.repository.MemoTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class MemoService {

    private final MemoRepository memoRepository;
    private final MemoTopicRepository topicRepository;
    private final MemoNumberingService numberingService;
    private final WebClient.Builder webClientBuilder;

    @Value("${services.workflow-service.url}")
    private String workflowServiceUrl;

    // Mapper could be MapStruct, but implementing manual for now to save
    // time/complexity
    private MemoDTO toDTO(Memo memo) {
        MemoDTO dto = new MemoDTO();
        dto.setId(memo.getId());
        dto.setMemoNumber(memo.getMemoNumber());
        dto.setSubject(memo.getSubject());
        dto.setStatus(memo.getStatus());
        dto.setPriority(memo.getPriority());
        dto.setContent(memo.getContent());
        dto.setFormData(memo.getFormData());

        dto.setTopicId(memo.getTopic().getId());
        dto.setTopicName(memo.getTopic().getName());
        dto.setCategoryId(memo.getCategory().getId());
        dto.setCategoryName(memo.getCategory().getName());

        dto.setCreatedBy(memo.getCreatedBy());
        dto.setCreatedAt(memo.getCreatedAt());
        dto.setUpdatedAt(memo.getUpdatedAt());
        return dto;
    }

    @Transactional
    public MemoDTO createDraft(CreateMemoRequest request, UUID userId) {
        MemoTopic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new RuntimeException("Topic not found"));

        String memoNumber = numberingService.generateMemoNumber(topic);

        Memo memo = new Memo();
        memo.setMemoNumber(memoNumber);
        memo.setSubject(request.getSubject());
        memo.setTopic(topic);
        memo.setCategory(topic.getCategory());
        memo.setPriority(request.getPriority());
        memo.setStatus(MemoStatus.DRAFT);
        memo.setCreatedBy(userId);

        // Initialize content from template if available
        if (topic.getContentTemplate() != null) {
            memo.setContent(topic.getContentTemplate());
        }

        memo = memoRepository.save(memo);
        return toDTO(memo);
    }

    @Transactional(readOnly = true)
    public MemoDTO getMemo(UUID id) {
        return memoRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Memo not found"));
    }

    @Transactional(readOnly = true)
    public List<MemoDTO> getMyMemos(UUID userId) {
        return memoRepository.findByCreatedBy(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public MemoDTO updateMemo(UUID id, UpdateMemoRequest request) {
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Memo not found"));

        if (memo.getStatus() != MemoStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT memos can be edited");
        }

        if (request.getSubject() != null) {
            memo.setSubject(request.getSubject());
        }
        if (request.getPriority() != null) {
            memo.setPriority(request.getPriority());
        }
        if (request.getContent() != null) {
            memo.setContent(request.getContent());
        }
        if (request.getFormData() != null) {
            memo.setFormData(request.getFormData());
        }

        memo = memoRepository.save(memo);
        return toDTO(memo);
    }

    @Transactional
    public MemoDTO submitMemo(UUID id, UUID userId) {
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Memo not found"));

        if (memo.getStatus() != MemoStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT memos can be submitted");
        }

        // Start Workflow
        try {
            java.util.Map<String, Object> variables = java.util.Map.of(
                    "memoId", memo.getId().toString(),
                    "memoNumber", memo.getMemoNumber(),
                    "subject", memo.getSubject(),
                    "manager", "admin" // Fixed manager for MVP
            );

            java.util.Map<String, Object> request = java.util.Map.of(
                    "processDefinitionKey", "memo_approval_process",
                    "businessKey", memo.getId().toString(),
                    "variables", variables);

            java.util.Map response = webClientBuilder.build()
                    .post()
                    .uri(workflowServiceUrl + "/api/process-instances/start-by-key")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(java.util.Map.class)
                    .block();

            if (response != null && response.containsKey("id")) {
                memo.setProcessInstanceId((String) response.get("id"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to start workflow: " + e.getMessage(), e);
        }

        memo.setStatus(MemoStatus.SUBMITTED); // Or IN_REVIEW depending on process
        memo.setCurrentStage("Manager Approval");

        memo = memoRepository.save(memo);
        return toDTO(memo);
    }

    @Transactional
    public void updateMemoStatus(UUID id, String statusStr) {
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Memo not found"));

        try {
            MemoStatus status = MemoStatus.valueOf(statusStr.toUpperCase());
            memo.setStatus(status);
            memo.setCurrentStage("Completed"); // Or handle strictly based on status
            memoRepository.save(memo);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + statusStr);
        }
    }
}
