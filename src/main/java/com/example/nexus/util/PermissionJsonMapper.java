package com.example.nexus.util;

import com.example.nexus.dto.PackageDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;


@Component
public class PermissionJsonMapper {

    private final ObjectMapper objectMapper;

    public PermissionJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(List<PackageDto.Category> categories) {
        try {
            return objectMapper.writeValueAsString(categories == null ? Collections.emptyList() : categories);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize package permissions", e);
        }
    }

    public List<PackageDto.Category> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<PackageDto.Category>>() {
            });
        } catch (JsonProcessingException e) {
            System.err.println("Failed to parse permissions JSON, returning empty list: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
