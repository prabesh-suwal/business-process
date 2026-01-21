package com.enterprise.memo.service;

import com.enterprise.memo.entity.MemoTopic;
import com.enterprise.memo.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MemoNumberingService {

    private final MemoRepository memoRepository;

    @Transactional(readOnly = true)
    public String generateMemoNumber(MemoTopic topic) {
        String pattern = topic.getNumberingPattern();
        if (pattern == null || pattern.isBlank()) {
            return "MEMO-" + System.currentTimeMillis();
        }

        // Replace %FY% with current fiscal year (e.g., 2025-26 or just 2025)
        // For simplicity, using calendar year for now.
        int year = LocalDate.now().getYear();
        String fy = String.valueOf(year);

        String number = pattern.replace("%FY%", fy);

        // Replace %SEQ% with sequence number
        if (number.contains("%SEQ%")) {
            long count = memoRepository.countByTopicId(topic.getId());
            long nextSeq = count + 1;
            // Pad sequence to 4 digits
            String seqStr = String.format("%04d", nextSeq);
            number = number.replace("%SEQ%", seqStr);
        }

        return number;
    }
}
