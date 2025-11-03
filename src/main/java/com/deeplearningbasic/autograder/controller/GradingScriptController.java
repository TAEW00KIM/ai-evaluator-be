package com.deeplearningbasic.autograder.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

@Slf4j
@Tag(name = "GradingScript", description = "채점 스크립트 관리 API")
@RestController
@RequestMapping("/api/admin/grading-script")
public class GradingScriptController {

    @Value("${evaluator.script.path:/data/class/ta/twkk0819/evaluator-py/Evaluator-Python/grading_script.py}")
    private String scriptPath;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> uploadGradingScript(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty() || !file.getOriginalFilename().endsWith(".py")) {
                return ResponseEntity.badRequest().body(".py 파일만 업로드 가능합니다.");
            }

            Path target = Path.of(scriptPath); // e.g. /data/.../grading_script.py
            Path dir = target.getParent();
            if (dir == null) return ResponseEntity.internalServerError().body("잘못된 scriptPath");

            // 기존 파일 백업
            if (Files.exists(target)) {
                Path bak = dir.resolve("grading_script.py.bak");
                Files.copy(target, bak, REPLACE_EXISTING);
            }

            // 임시 파일로 저장 후 원자적 교체
            Path tmp = dir.resolve("grading_script.py.uploading");
            Files.copy(file.getInputStream(), tmp, REPLACE_EXISTING);

            // 퍼미션(리눅스인 경우) – 읽기필수
            try {
                Files.setPosixFilePermissions(tmp,
                        EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                                PosixFilePermission.GROUP_READ));
            } catch (UnsupportedOperationException ignore) { /* 윈도/비-POSIX면 생략 */ }

            Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING);

            log.info("✅ Grading script updated: {}", target.toAbsolutePath());
            return ResponseEntity.ok("채점 스크립트 업데이트 완료");
        } catch (Exception e) {
            log.error("❌ Failed to update grading script", e);
            return ResponseEntity.internalServerError().body("오류: " + e.getMessage());
        }
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> download() throws IOException {
        Path target = Path.of(scriptPath);
        if (!Files.exists(target)) return ResponseEntity.notFound().build();
        ByteArrayResource r = new ByteArrayResource(Files.readAllBytes(target));
        return ResponseEntity.ok().header("Content-Disposition","attachment; filename=grading_script.py").body(r);
    }
}