package com.deeplearningbasic.autograder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class AssignmentRequestDto {
    @NotBlank
    private String title;

    @NotBlank
    private String description;

    // 선택값 (null이면 서비스에서 false로 처리)
    private Boolean leaderboardHidden;
}