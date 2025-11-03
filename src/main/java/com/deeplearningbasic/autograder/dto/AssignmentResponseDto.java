package com.deeplearningbasic.autograder.dto;

import com.deeplearningbasic.autograder.domain.Assignment;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class AssignmentResponseDto {
    private final Long id;
    private final String title;
    private final String description;
    private final LocalDateTime createdAt;
    private final boolean leaderboardHidden;

    public AssignmentResponseDto(Assignment assignment) {
        this.id = assignment.getId();
        this.title = assignment.getTitle();
        this.description = assignment.getDescription();
        this.createdAt = assignment.getCreatedAt();
        this.leaderboardHidden = assignment.isLeaderboardHidden();
    }
}