package com.deeplearningbasic.autograder.controller;

import com.deeplearningbasic.autograder.dto.AdminSubmissionDto;
import com.deeplearningbasic.autograder.dto.ApiResponse;
import com.deeplearningbasic.autograder.dto.AssignmentResponseDto;
import com.deeplearningbasic.autograder.dto.AssignmentToggleRequest;
import com.deeplearningbasic.autograder.service.AssignmentService;
import com.deeplearningbasic.autograder.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin", description = "관리자 전용 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final SubmissionService submissionService;
    private final AssignmentService assignmentService;

    @Operation(summary = "전체 제출 목록 조회", description = "관리자가 모든 학생의 제출 기록을 조회합니다.")
    @GetMapping("/submissions")
    public ResponseEntity<ApiResponse<List<AdminSubmissionDto>>> getAllSubmissions() {
        List<AdminSubmissionDto> submissions = submissionService.findAllSubmissionsForAdmin();
        return ResponseEntity.ok(ApiResponse.success(submissions, "전체 제출 목록 조회 성공"));
    }

    @PatchMapping("/assignments/{id}/submissions")
    public ResponseEntity<AssignmentResponseDto> setSubmissionsClosed(
            @PathVariable Long id,
            @RequestBody AssignmentToggleRequest req) {
        return ResponseEntity.ok(assignmentService.setSubmissionsClosed(id, req.isClosed()));
    }

    // TODO: 특정 제출물의 상세 정보 조회 및 코드 다운로드 API 추가 예정
}