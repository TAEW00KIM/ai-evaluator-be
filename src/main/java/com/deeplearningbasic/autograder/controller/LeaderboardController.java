package com.deeplearningbasic.autograder.controller;

import com.deeplearningbasic.autograder.dto.LeaderboardRowDto;
import com.deeplearningbasic.autograder.service.AssignmentService;
import com.deeplearningbasic.autograder.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final AssignmentService assignmentService;

    @GetMapping("/{assignmentId}")
    public List<LeaderboardRowDto> getLeaderboard(
            @PathVariable Long assignmentId,
            Authentication authentication) {
        boolean isAdmin = authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && assignmentService.isLeaderboardHidden(assignmentId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "이 과제의 리더보드는 현재 열람이 제한되어 있습니다."
            );
        }

        return leaderboardService.getLeaderboard(assignmentId);
    }
}
