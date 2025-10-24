package com.cnwv.game_server.jpa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter(autoApply = false)
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper om = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        try {
            if (attribute == null) return "[]";
            return om.writeValueAsString(attribute);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) return Collections.emptyList();
            return om.readValue(dbData, TYPE);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
