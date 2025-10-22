package com.cnwv.game_server.controller;

import com.cnwv.game_server.dto.SkillDataResponse;
import com.cnwv.game_server.service.PlayerSkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
@Tag(name = "Player Skill API", description = "스킬 삽입/조회/수정/삭제")
@SecurityRequirement(name = "bearerAuth")
public class PlayerSkillController {

    private final PlayerSkillService service;

    @PostMapping
    @Operation(summary = "스킬 추가/증가(업서트)",
            description = "존재하지 않으면 생성, 존재하면 skill_level += delta")
    @ApiResponse(responseCode = "200", description = "성공/실패 문자열")
    public ResponseEntity<?> addOrIncrease(
            @Parameter(description = "유저명", example = "cnwvid")
            @RequestParam String username,
            @Parameter(description = "스킬 코드", example = "skill_code001")
            @RequestParam String skillCode,
            @Parameter(description = "증가치(양수)", example = "1")
            @RequestParam int delta
    ) {
        boolean ok = service.addOrIncrease(username, skillCode, delta);
        return ok ? ResponseEntity.ok("✅ 스킬 추가/증가 완료")
                : ResponseEntity.badRequest().body("❌ 실패");
    }

    @GetMapping
    @Operation(summary = "내 스킬 목록 조회",
            description = "{ skillCode, skillLevel }만 반환")
    @ApiResponse(responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SkillDataResponse.class))))
    public ResponseEntity<List<SkillDataResponse>> list(
            @Parameter(description = "유저명", example = "cnwvid")
            @RequestParam String username
    ) {
        return ResponseEntity.ok(service.getSkills(username));
    }

    @PutMapping
    @Operation(summary = "스킬 레벨 절대값 설정",
            description = "존재할 때만 동작, 0 이상만 허용")
    public ResponseEntity<?> setLevel(
            @RequestParam String username,
            @RequestParam String skillCode,
            @RequestParam int level
    ) {
        boolean ok = service.setLevel(username, skillCode, level);
        return ok ? ResponseEntity.ok("✅ 스킬 레벨 설정 완료")
                : ResponseEntity.badRequest().body("❌ 수정 실패");
    }

    @DeleteMapping
    @Operation(summary = "스킬 레벨 차감/삭제",
            description = "level -= delta; 0 이하이면 삭제")
    public ResponseEntity<?> decrease(
            @RequestParam String username,
            @RequestParam String skillCode,
            @RequestParam int delta
    ) {
        boolean ok = service.decreaseOrDelete(username, skillCode, delta);
        return ok ? ResponseEntity.ok("✅ 스킬 차감/삭제 처리 완료")
                : ResponseEntity.badRequest().body("❌ 삭제 실패");
    }
}
