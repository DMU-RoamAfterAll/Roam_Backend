package com.cnwv.game_server.controller;

import com.cnwv.game_server.Entity.UserChoice;
import com.cnwv.game_server.dto.UserChoiceDto;
import com.cnwv.game_server.service.UserChoiceService;
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
@RequestMapping("/api/choices")
@Tag(name = "User Choice API", description = "선택지 플래그 삽입/조회 API (업서트 + 동일값 무시)")
@SecurityRequirement(name = "bearerAuth")
public class UserChoiceController {

    private final UserChoiceService choiceService;

    @PutMapping
    @Operation(summary = "선택지 설정(업서트)", description = "존재하지 않으면 삽입, 존재하면 수정. 동일 값이면 변경 없이 성공 처리합니다.")
    public ResponseEntity<?> setChoice(
            @RequestParam String username,
            @RequestParam String choiceCode,
            @RequestParam boolean condition
    ) {
        boolean success = choiceService.setChoice(username, choiceCode, condition);
        return success ? ResponseEntity.ok("✅ 선택지 설정 완료")
                : ResponseEntity.badRequest().body("❌ 실패");
    }

    @GetMapping
    @Operation(summary = "전체 선택지 조회 (DTO 응답)", description = "필요한 필드만 반환합니다.")
    public ResponseEntity<List<UserChoiceDto>> getChoices(@RequestParam String username) {
        List<UserChoice> list = choiceService.getChoices(username);
        List<UserChoiceDto> dtos = list.stream()
                .map(c -> new UserChoiceDto(
                        c.getId().getChoiceCode(),
                        c.isCondition(),
                        c.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/condition")
    @Operation(summary = "특정 선택지 상태 조회", description = "특정 choiceCode의 condition 값을 반환합니다.")
    public ResponseEntity<?> getCondition(
            @RequestParam String username,
            @RequestParam String choiceCode
    ) {
        Boolean result = choiceService.getCondition(username, choiceCode);
        return (result != null)
                ? ResponseEntity.ok(Map.of("condition", result))
                : ResponseEntity.badRequest().body("❌ 존재하지 않음");
    }
}
