package com.deeplearningbasic.autograder.service;

import com.deeplearningbasic.autograder.domain.Assignment;
import com.deeplearningbasic.autograder.dto.AssignmentResponseDto;
import com.deeplearningbasic.autograder.exception.ResourceNotFoundException;
import com.deeplearningbasic.autograder.repository.AssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository repo;

    @Transactional(readOnly = true)
    public boolean isLeaderboardHidden(Long assignmentId) {
        return repo.findById(assignmentId)
                .map(Assignment::isLeaderboardHidden)
                .orElse(false); // 과제가 없으면 기본적으로 false (안 숨김)
    }

    @Transactional
    public void setLeaderboardHidden(Long id, boolean hidden) {
        Assignment a = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No assignment"));
        a.setLeaderboardHidden(hidden);
        repo.save(a);
    }

    @Transactional(readOnly = true)
    public List<Assignment> listAll() { return repo.findAll(); }

    @Transactional
    public AssignmentResponseDto setSubmissionsClosed(Long assignmentId, boolean closed) {
        Assignment a = repo.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        a.setSubmissionsClosed(closed);
        return new AssignmentResponseDto(a);
    }
}
