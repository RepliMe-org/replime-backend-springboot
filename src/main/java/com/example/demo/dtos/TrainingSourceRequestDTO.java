package com.example.demo.dtos;

import com.example.demo.entities.utils.SourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
import java.net.URL;
import java.net.MalformedURLException;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingSourceRequestDTO {
    private String sourceValue;
    private SourceType sourceType;
    @Min(0)
    private Integer last_n;

    @AssertTrue(message = "Invalid sourceValue based on sourceType")
    public boolean isSourceValueValidForType() {
        if (sourceType == SourceType.LAST_N) {
            if (last_n == null || last_n <= 0) {
                return false;
            }
        } else if (sourceType == SourceType.VIDEO) {
            if (sourceValue == null || sourceValue.trim().isEmpty()) {
                return false;
            }
            try {
                new URL(sourceValue);
                return true;
            } catch (MalformedURLException e) {
                return false;
            }
        }else if (sourceType == SourceType.PLAYLIST) {
            if (sourceValue == null || sourceValue.trim().isEmpty()) {
                return false;
            }
            try {
                new URL(sourceValue);
                return true;
            } catch (MalformedURLException e) {
                return false;
            }
        }
        return true;
    }
}
