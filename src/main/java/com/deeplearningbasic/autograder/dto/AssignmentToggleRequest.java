package com.deeplearningbasic.autograder.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignmentToggleRequest {
    private boolean submissionsClosed;
}
