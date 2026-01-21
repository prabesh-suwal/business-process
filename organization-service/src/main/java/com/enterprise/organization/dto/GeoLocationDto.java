package com.enterprise.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocationDto {
    private UUID id;
    private String countryCode;
    private String code;
    private String name;
    private String localName;
    private String typeCode;
    private String typeName;
    private UUID parentId;
    private String parentName;
    private String fullPath;
    private String status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String countryCode;
        private String code;
        private String name;
        private String localName;
        private String typeCode;
        private UUID parentId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String name;
        private String localName;
        private String status;
    }
}
