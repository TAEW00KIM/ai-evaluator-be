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
    private final boolean submissionsClosed;
    private final boolean leaderboardHidden;

    public AssignmentResponseDto(Assignment a) {
        this.id = a.getId();
        this.title = a.getTitle();
        this.description = a.getDescription();
        this.createdAt = a.getCreatedAt();
        this.submissionsClosed = a.isSubmissionsClosed();
        this.leaderboardHidden = a.isLeaderboardHidden();
    }
}