package com.cnwv.game_server.controller;

import com.cnwv.game_server.dto.FlagDataResponse;
import com.cnwv.game_server.service.FlagDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flags")
@Tag(name = "Flag Data API", description = "선택지 플래그 삽입/조회 API (업서트 + 동일값 무시)")
@SecurityRequirement(name = "bearerAuth")
public class FlagDataController {

    private final FlagDataService flagService;

    @PutMapping
    @Operation(summary = "플래그 설정", description = "존재하지 않으면 삽입, 존재하면 수정. 동일 값이면 변경 없이 성공 처리합니다.")
    public ResponseEntity<?> setFlag(
            @RequestParam String username,
            @RequestParam String flagCode,   // ✅ 파라미터 네이밍도 flagCode/flagState로 통일 가능
            @RequestParam boolean flagState
    ) {
        boolean success = flagService.setFlag(username, flagCode, flagState);
        return success ? ResponseEntity.ok("✅ 플래그 설정 완료")
                : ResponseEntity.badRequest().body("❌ 실패");
    }

    @GetMapping
    @Operation(summary = "전체 플래그 조회", description = "{ flagCode, flagState }만 반환")
    public ResponseEntity<List<FlagDataResponse>> getFlags(@RequestParam String username) {
        return ResponseEntity.ok(flagService.getFlags(username));
    }

    @GetMapping("/flagState")
    @Operation(summary = "특정 플래그 상태 조회", description = "특정 flagCode의 flagState 값을 반환합니다.")
    public ResponseEntity<?> getFlagState(
            @RequestParam String username,
            @RequestParam String flagCode
    ) {
        Boolean result = flagService.getFlagState(username, flagCode);
        return (result != null)
                ? ResponseEntity.ok(new FlagDataResponse(flagCode, result))
                : ResponseEntity.badRequest().body("❌ 존재하지 않음");
    }

    @DeleteMapping
    @Operation(summary = "플래그 삭제", description = "특정 flagCode의 플래그를 삭제합니다.")
    public ResponseEntity<?> deleteFlag(
            @RequestParam String username,
            @RequestParam String flagCode
    ) {
        boolean success = flagService.deleteFlag(username, flagCode);
        return success ? ResponseEntity.ok("✅ 플래그 삭제 완료")
                : ResponseEntity.badRequest().body("❌ 삭제 실패");
    }
}
