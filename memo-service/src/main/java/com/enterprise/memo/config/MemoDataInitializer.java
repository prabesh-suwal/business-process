package com.enterprise.memo.config;

import com.enterprise.memo.entity.MemoCategory;
import com.enterprise.memo.entity.MemoTopic;
import com.enterprise.memo.repository.MemoCategoryRepository;
import com.enterprise.memo.repository.MemoTopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MemoDataInitializer {

    private final MemoCategoryRepository categoryRepository;
    private final MemoTopicRepository topicRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (categoryRepository.count() == 0) {
                log.info("Initializing Memo Categories...");

                // 1. Finance
                MemoCategory finance = MemoCategory.builder()
                        .code("FINANCE")
                        .name("Finance & Accounts")
                        .description("Financial approvals and expense requests")
                        .accessPolicy("department == 'FINANCE'")
                        .build();
                finance = categoryRepository.save(finance);

                // 2. HR
                MemoCategory hr = MemoCategory.builder()
                        .code("HR")
                        .name("Human Resources")
                        .description("Hiring, Leave, and Personnel requests")
                        .accessPolicy("department == 'HR'")
                        .build();
                hr = categoryRepository.save(hr);

                log.info("Initializing Memo Topics...");

                // Topic: Capital Expense
                MemoTopic capex = MemoTopic.builder()
                        .category(finance)
                        .code("CAPEX")
                        .name("Capital Expense Request")
                        .description("Request validation for asset purchase > $1000")
                        .numberingPattern("CPX-%FY%-%SEQ%")
                        .contentTemplate(Map.of(
                                "type", "doc",
                                "content", java.util.List.of(
                                        Map.of(
                                                "type", "paragraph",
                                                "content", java.util.List.of(
                                                        Map.of("type", "text", "text",
                                                                "Dear Approver, I request approval for the purchase of the following assets:"))))))
                        .build();
                topicRepository.save(capex);

                // Topic: New Hire
                MemoTopic newHire = MemoTopic.builder()
                        .category(hr)
                        .code("NEW_HIRE")
                        .name("New Hire Requisition")
                        .numberingPattern("HR-%SEQ%")
                        .contentTemplate(Map.of("type", "doc", "content", java.util.List.of()))
                        .build();
                topicRepository.save(newHire);

                log.info("Memo System Seeding Complete.");
            }
        };
    }
}
