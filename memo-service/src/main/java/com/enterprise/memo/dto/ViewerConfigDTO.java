package com.enterprise.memo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for viewer configuration.
 * Used for both memo-wide and step-specific viewer configs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewerConfigDTO {

    private List<ViewerDTO> viewers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ViewerDTO {
        private String type; // USER, ROLE, DEPARTMENT
        private String userId; // For USER type
        private String role; // For ROLE type
        private String departmentId; // For DEPARTMENT type
    }
}
