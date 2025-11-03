package com.deeplearningbasic.autograder.controller;

import com.deeplearningbasic.autograder.domain.Assignment;
import com.deeplearningbasic.autograder.dto.ApiResponse;
import com.deeplearningbasic.autograder.dto.AssignmentRequestDto;
import com.deeplearningbasic.autograder.dto.AssignmentResponseDto;
import com.deeplearningbasic.autograder.dto.LeaderboardToggleHidden;
import com.deeplearningbasic.autograder.repository.AssignmentRepository;
import com.deeplearningbasic.autograder.service.AssignmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "Assignments", description = "과제 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AssignmentController {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentService assignmentService;

    @PostMapping("/admin/assignments") // 관리자만 접근 가능
    public ResponseEntity<ApiResponse<Long>> createAssignment(@RequestBody AssignmentRequestDto requestDto) {
        Assignment assignment = Assignment.builder()
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .build();
        Assignment savedAssignment = assignmentRepository.save(assignment);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(savedAssignment.getId(), "과제가 성공적으로 생성되었습니다."));
    }

    @GetMapping("/assignments") // 모든 사용자가 접근 가능
    public ResponseEntity<ApiResponse<List<AssignmentResponseDto>>> getAllAssignments() {
        List<AssignmentResponseDto> assignments = assignmentRepository.findAll().stream()
                .map(AssignmentResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(assignments, "과제 목록 조회 성공"));
    }

    @DeleteMapping("/admin/assignments/{id}") // 관리자만 접근 가능
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(@PathVariable Long id) {
        // 해당 ID의 과제가 존재하는지 확인
        if (!assignmentRepository.existsById(id)) {
            // 존재하지 않으면 404 Not Found 응답
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("ID " + id + "에 해당하는 과제를 찾을 수 없습니다."));
        }
        // 과제 삭제
        assignmentRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "과제가 성공적으로 삭제되었습니다."));
    }

    @PatchMapping("/admin/assignments/{id}/leaderboard")
    public ResponseEntity<AssignmentResponseDto> toggle(
            @PathVariable Long id,
            @RequestBody LeaderboardToggleHidden req
    ) {
        assignmentService.setLeaderboardHidden(id, req.hidden());
        Assignment updated = assignmentRepository.findById(id).orElseThrow();
        return ResponseEntity.ok(new AssignmentResponseDto(updated));
    }
}