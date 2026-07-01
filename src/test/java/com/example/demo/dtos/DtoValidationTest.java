package com.example.demo.dtos;

import com.example.demo.dtos.internal.UpdateVideoStatusRequestDTO;
import com.example.demo.entities.utils.Formality;
import com.example.demo.entities.utils.SourceType;
import com.example.demo.entities.utils.Tone;
import com.example.demo.entities.utils.Verbosity;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void signupRequestValidatesNameEmailAndPassword() {
        SignupRequestDTO request = new SignupRequestDTO();
        request.setName("A");
        request.setEmail("not-email");
        request.setPassword("short");

        Set<ConstraintViolation<SignupRequestDTO>> violations = validator.validate(request);

        assertEquals(3, violations.size());
        assertTrue(hasViolationOn(violations, "name"));
        assertTrue(hasViolationOn(violations, "email"));
        assertTrue(hasViolationOn(violations, "password"));
    }

    @Test
    void loginRequestAcceptsValidEmailAndLongPassword() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void chatbotConfigRequestRequiresNameGreetingTalkLikeMeAndFetchChannel() {
        ChatbotConfigRequestDTO request = new ChatbotConfigRequestDTO();
        request.setDescription("description");
        request.setTone(Tone.FRIENDLY);
        request.setVerbosity(Verbosity.BALANCED);
        request.setFormality(Formality.CASUAL);

        Set<ConstraintViolation<ChatbotConfigRequestDTO>> violations = validator.validate(request);

        assertEquals(4, violations.size());
        assertTrue(hasViolationOn(violations, "name"));
        assertTrue(hasViolationOn(violations, "greetingMessage"));
        assertTrue(hasViolationOn(violations, "talkLikeMe"));
        assertTrue(hasViolationOn(violations, "fetchChannel"));
    }

    @Test
    void trainingSourceRequestValidatesLastNRequiresPositiveCount() {
        TrainingSourceRequestDTO request = TrainingSourceRequestDTO.builder()
                .sourceType(SourceType.LAST_N)
                .last_n(0)
                .build();

        Set<ConstraintViolation<TrainingSourceRequestDTO>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(hasViolationOn(violations, "sourceValueValidForType"));
    }

    @Test
    void trainingSourceRequestValidatesVideoUrl() {
        TrainingSourceRequestDTO invalid = TrainingSourceRequestDTO.builder()
                .sourceType(SourceType.VIDEO)
                .sourceValue("not-a-url")
                .build();
        TrainingSourceRequestDTO valid = TrainingSourceRequestDTO.builder()
                .sourceType(SourceType.VIDEO)
                .sourceValue("https://youtube.com/watch?v=abc")
                .build();

        assertFalse(validator.validate(invalid).isEmpty());
        assertTrue(validator.validate(valid).isEmpty());
    }

    @Test
    void updateVideoStatusRequestRequiresStatus() {
        UpdateVideoStatusRequestDTO request = new UpdateVideoStatusRequestDTO();

        Set<ConstraintViolation<UpdateVideoStatusRequestDTO>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertTrue(hasViolationOn(violations, "status"));
    }

    private static boolean hasViolationOn(Set<? extends ConstraintViolation<?>> violations, String property) {
        return violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(property));
    }
}
