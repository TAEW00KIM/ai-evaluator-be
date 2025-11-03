package com.deeplearningbasic.autograder.controller;

import com.deeplearningbasic.autograder.domain.Assignment;
import com.deeplearningbasic.autograder.domain.Submission;
import com.deeplearningbasic.autograder.dto.ApiResponse;
import com.deeplearningbasic.autograder.dto.EvaluationResultDto;
import com.deeplearningbasic.autograder.dto.SubmissionRequestDto;
import com.deeplearningbasic.autograder.dto.SubmissionResponseDto;
import com.deeplearningbasic.autograder.service.SubmissionService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Tag(name = "Submissions", description = "학생 과제 제출 및 결과 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SubmissionController {

    private final SubmissionService submissionService;

    @Operation(summary = "학생 과제 제출", description = "학생이 과제 코드가 담긴 zip 파일을 업로드합니다. 요청이 성공하면 채점 대기열에 등록됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (예: 파일 없음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음 (타인 이름으로 제출 시도)")
    })
    @PostMapping("/submissions")
    public ResponseEntity<ApiResponse<Long>> submitAssignment(
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oAuth2User, // 현재 로그인한 사용자 정보
            @Parameter(description = "학생 ID, 과제 ID, 제출 파일(zip)을 포함하는 DTO")
            @ModelAttribute SubmissionRequestDto requestDto) throws IOException {

        Long submissionId = submissionService.createSubmission(requestDto, oAuth2User);
        Assignment assignment = submissionService.findAssignmentById(requestDto.getAssignmentId());
        if (assignment.isSubmissionsClosed()) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("해당 과제는 제출이 마감되었습니다."));
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(submissionId, "과제 제출에 성공했으며, 채점 대기 중입니다."));
    }

    @Operation(summary = "제출 결과 조회", description = "학생 본인이 제출한 과제의 채점 상태 및 결과를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음 (타인의 제출물 조회 시도)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 제출 ID")
    })
    @GetMapping("/submissions/{id}")
    public ResponseEntity<ApiResponse<SubmissionResponseDto>> getSubmissionResult(
            @Parameter(hidden = true) @AuthenticationPrincipal OAuth2User oAuth2User, // 현재 로그인한 사용자 정보
            @Parameter(description = "조회할 제출물의 ID", required = true, example = "1")
            @PathVariable Long id) {

        Submission submission = submissionService.findSubmissionByIdAndCheckPermission(id, oAuth2User);
        SubmissionResponseDto responseDto = new SubmissionResponseDto(submission);

        return ResponseEntity.ok(ApiResponse.success(responseDto, "제출 결과 조회에 성공했습니다."));
    }

    @Hidden
    @PostMapping("/internal/submissions/{id}/running")
    public ResponseEntity<ApiResponse<String>> updateSubmissionToRunning(@PathVariable Long id) {
        submissionService.updateSubmissionToRunning(id);
        return ResponseEntity.ok(ApiResponse.success("OK", "상태를 RUNNING으로 변경했습니다."));
    }

    @Hidden
    @PostMapping("/internal/submissions/{id}/complete")
    public ResponseEntity<ApiResponse<String>> updateSubmissionResult(
            @PathVariable Long id,
            @RequestBody EvaluationResultDto resultDto) {
        submissionService.updateSubmissionResult(id, resultDto);
        return ResponseEntity.ok(ApiResponse.success("OK", "결과 처리에 성공했습니다."));
    }

    @Operation(summary = "내 제출 목록 조회", description = "로그인한 사용자의 모든 제출 기록을 조회합니다.")
    @GetMapping("/submissions/me")
    public ResponseEntity<ApiResponse<List<SubmissionResponseDto>>> getMySubmissions(@AuthenticationPrincipal OAuth2User oAuth2User) {
        List<SubmissionResponseDto> mySubmissions = submissionService.findMySubmissions(oAuth2User);
        return ResponseEntity.ok(ApiResponse.success(mySubmissions, "내 제출 목록 조회 성공"));
    }
}