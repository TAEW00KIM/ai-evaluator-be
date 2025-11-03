package com.deeplearningbasic.autograder.service;

import com.deeplearningbasic.autograder.domain.Assignment;
import com.deeplearningbasic.autograder.domain.Role;
import com.deeplearningbasic.autograder.domain.Submission;
import com.deeplearningbasic.autograder.domain.User;
import com.deeplearningbasic.autograder.dto.*;
import com.deeplearningbasic.autograder.exception.ResourceNotFoundException;
import com.deeplearningbasic.autograder.repository.AssignmentRepository;
import com.deeplearningbasic.autograder.repository.SubmissionRepository;
import com.deeplearningbasic.autograder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
    private final UserRepository userRepository;
    private final WebClient.Builder webClientBuilder;
    private final AssignmentRepository assignmentRepository;

    @Value("${external.python-api-url}")
    private String pythonApiUrl;

    @Transactional
    public Long createSubmission(SubmissionRequestDto requestDto, OAuth2User oAuth2User) throws IOException {
        User loggedInUser = findUserByOauth2User(oAuth2User);
        if (!loggedInUser.getId().equals(requestDto.getStudentId())) {
            throw new AccessDeniedException("자신의 이름으로만 과제를 제출할 수 있습니다.");
        }

        Assignment assignment = assignmentRepository.findById(requestDto.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("과제를 찾을 수 없습니다. ID: " + requestDto.getAssignmentId()));

        if (assignment.isSubmissionsClosed()) {
            throw new AccessDeniedException("이 과제는 현재 제출이 마감되었습니다.");
        }

        // 1. 파일 저장
        String originalFileName = requestDto.getFile().getOriginalFilename();
        String storedFileName = UUID.randomUUID() + "_" + originalFileName;
        Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
        Files.copy(requestDto.getFile().getInputStream(), targetLocation);

        // 2. Submission 엔티티 생성 및 DB 저장
        Submission submission = Submission.builder()
                .studentId(requestDto.getStudentId())
                .assignment(assignment)
                .filePath(targetLocation.toString())
                .build();

        Submission savedSubmission = submissionRepository.save(submission);

        // 3. 비동기로 Python 채점 서버 API 호출
        requestEvaluation(savedSubmission.getId(), targetLocation.toString());

        return savedSubmission.getId();
    }

    public Submission findSubmissionByIdAndCheckPermission(Long submissionId, OAuth2User oAuth2User) {
        User loggedInUser = findUserByOauth2User(oAuth2User);
        Submission submission = findById(submissionId);

        // 보안 검증: 관리자이거나, 제출물의 주인이 맞는지 확인
        boolean isAdmin = loggedInUser.getRole() == Role.ADMIN;
        boolean isOwner = submission.getStudentId().equals(loggedInUser.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("이 제출물을 조회할 권한이 없습니다.");
        }

        return submission;
    }

    @Async // 이 메서드를 별도의 스레드에서 비동기로 실행
    public void requestEvaluation(Long submissionId, String filePath) {
        WebClient webClient = webClientBuilder.baseUrl(pythonApiUrl).build();

        // Python 서버로 보낼 요청 본문(Body)
        Map<String, Object> body = Map.of(
                "submissionId", submissionId,
                "filePath", filePath
        );

        webClient.post()
                .uri("/evaluate")
                .bodyValue(body)
                .retrieve() // 응답을 받음
                .bodyToMono(Void.class) // 응답 본문은 무시
                .doOnError(error -> {
                    // Python 서버 호출 실패 시 에러 처리
                    updateSubmissionResult(submissionId, new EvaluationResultDto(0.0, "채점 서버 호출에 실패했습니다: " + error.getMessage()));
                })
                .subscribe(); // 실제 요청 실행
    }

    @Transactional
    public void updateSubmissionToRunning(Long submissionId) {
        Submission submission = findById(submissionId);
        submission.running();
        // submissionRepository.save(submission); // @Transactional 어노테이션에 의해 자동 저장됨
    }

    @Transactional
    public void updateSubmissionResult(Long submissionId, EvaluationResultDto resultDto) {
        Submission submission = findById(submissionId);
        if (resultDto.getLog().contains("오류 발생")) {
            submission.error(resultDto.getLog());
        } else {
            submission.complete(resultDto.getScore(), resultDto.getLog());
        }
        submissionRepository.save(submission);
    }

    public List<SubmissionResponseDto> findMySubmissions(OAuth2User oAuth2User) {
        User loggedInUser = findUserByOauth2User(oAuth2User);
        return submissionRepository.findAllByStudentIdOrderBySubmissionTimeDesc(loggedInUser.getId())
                .stream()
                .map(SubmissionResponseDto::new)
                .collect(Collectors.toList());
    }


    private User findUserByOauth2User(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("로그인한 사용자를 찾을 수 없습니다."));
    }

    public Submission findById(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("제출물을 찾을 수 없습니다. ID: " + submissionId));
    }
    public List<AdminSubmissionDto> findAllSubmissionsForAdmin() {
        return submissionRepository.findAll().stream()
                .map(submission -> {
                    // 각 제출(submission)에 해당하는 사용자(user) 정보를 찾음
                    User user = userRepository.findById(submission.getStudentId())
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + submission.getStudentId()));
                    return new AdminSubmissionDto(submission, user);
                })
                .collect(Collectors.toList());
    }

    public Assignment findAssignmentById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
    }
}
