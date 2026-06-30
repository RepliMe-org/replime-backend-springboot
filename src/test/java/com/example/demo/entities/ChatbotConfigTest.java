package com.example.demo.entities;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChatbotConfigTest {

    @Test
    void aiGeneratedDescriptionIsMappedAsText() throws NoSuchFieldException {
        Field field = ChatbotConfig.class.getDeclaredField("aiGeneratedDescription");

        Column column = field.getAnnotation(Column.class);

        assertNotNull(column);
        assertEquals("TEXT", column.columnDefinition());
    }
}
