package com.ru.scraper.store.converter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ru.scraper.data.response.ResponseMenu;

public class ResponseMenuConverter implements DynamoDBTypeConverter<String, ResponseMenu> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convert(ResponseMenu object) {
        try {
            System.out.println("DEBUG converter called, object null: " + (object == null));
            if (object == null) {
                return null;
            }
            String json = objectMapper.writeValueAsString(object);
            System.out.println("DEBUG serialized to: " + json);
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert ResponseMenu to String: " + e.getMessage(), e);
        }
    }

    @Override
    public ResponseMenu unconvert(String dynamoValue) {
        try {
            if (dynamoValue == null || dynamoValue.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(dynamoValue, ResponseMenu.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert String to ResponseMenu: " + e.getMessage(), e);
        }
    }
}

