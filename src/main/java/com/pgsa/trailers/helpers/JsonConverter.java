package com.pgsa.trailers.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Component
@Converter(autoApply = true)
public class JsonConverter implements AttributeConverter<Map<String, Object>, Object> {

    private static ObjectMapper objectMapper;

    @Autowired
    public void setObjectMapper(ObjectMapper mapper) {
        JsonConverter.objectMapper = mapper;
    }

    @Override
    public Object convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(attribute);
            
            PGobject pgObject = new PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue(json);
            return pgObject;
            
        } catch (SQLException | JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting to JSON", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return new HashMap<>();
        }

        try {
            String json;

            if (dbData instanceof PGobject) {
                json = ((PGobject) dbData).getValue();
            } else if (dbData instanceof String) {
                json = (String) dbData;
            } else if (dbData instanceof Map) {
                return (Map<String, Object>) dbData;
            } else {
                throw new IllegalArgumentException("Unsupported database type: " + dbData.getClass());
            }

            if (json == null || json.isEmpty() || "null".equals(json)) {
                return new HashMap<>();
            }

            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting JSON to object", e);
        }
    }
}
