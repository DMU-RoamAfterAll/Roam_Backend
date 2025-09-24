package com.cnwv.game_server.controller;

import com.cnwv.game_server.dto.FlagDataResponse;
import com.cnwv.game_server.service.FlagDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flags")
@Tag(name = "Flag Data API", description = "선택지 플래그 삽입/조회/삭제 + 전역 조회(UNION ALL)")
@SecurityRequirement(name = "bearerAuth")
public class FlagDataController {

    private final FlagDataService flagService;
    private final JdbcTemplate jdbcTemplate; // 전역 조회(UNION ALL)용

    // ---------- 단일 유저(샤드 라우팅) ----------
    @PutMapping
    @Operation(summary = "플래그 설정", description = "존재하지 않으면 삽입, 존재하면 수정. 동일 값이면 변경 없이 성공")
    public ResponseEntity<?> setFlag(
            @RequestParam String username,
            @RequestParam String flagCode,
            @RequestParam boolean flagState
    ) {
        boolean success = flagService.setFlag(username, flagCode, flagState);
        return success ? ResponseEntity.ok("✅ 플래그 설정 완료")
                : ResponseEntity.badRequest().body("❌ 실패");
    }

    @GetMapping
    @Operation(summary = "전체 플래그 조회(내 데이터)", description = "{ flagCode, flagState }만 반환")
    public ResponseEntity<List<FlagDataResponse>> getFlags(@RequestParam String username) {
        return ResponseEntity.ok(flagService.getFlags(username));
    }

    @GetMapping("/flagState")
    @Operation(summary = "특정 플래그 상태 조회(내 데이터)",
            description = "특정 flagCode의 flagState 값을 반환")
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
    @Operation(summary = "플래그 삭제(내 데이터)")
    public ResponseEntity<?> deleteFlag(
            @RequestParam String username,
            @RequestParam String flagCode
    ) {
        boolean success = flagService.deleteFlag(username, flagCode);
        return success ? ResponseEntity.ok("✅ 플래그 삭제 완료")
                : ResponseEntity.badRequest().body("❌ 삭제 실패");
    }

    // ---------- 전역 조회(UNION ALL) ----------
    @GetMapping("/global/search")
    @Operation(summary = "전역 플래그 검색(UNION ALL)",
            description = "flagCode + (optional) flagState 필터, created_at DESC 정렬, username 포함 반환")
    public ResponseEntity<List<Map<String, Object>>> searchFlagsGlobal(
            @RequestParam String flagCode,
            @RequestParam(required = false) Boolean flagState,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        limit = Math.max(1, Math.min(limit, 200));
        offset = Math.max(0, offset);

        String cond = (flagState == null) ? "" : " AND f.`condition` = ? ";
        String base =
                "SELECT ? AS shard, f.user_id, u.username, f.flag_code, f.`condition` AS flag_state, f.created_at " +
                        "  FROM %s.flag_data f " +
                        "  JOIN %s.users u ON u.id = f.user_id " +
                        " WHERE f.flag_code = ? " + cond;

        String sql =
                String.format(base, "cnwvdb_s0", "cnwvdb_s0") +
                        "UNION ALL " +
                        String.format(base, "cnwvdb_s1", "cnwvdb_s1") +
                        "ORDER BY created_at DESC " +
                        "LIMIT ? OFFSET ?";

        List<Map<String, Object>> rows;
        if (flagState == null) {
            rows = jdbcTemplate.queryForList(
                    sql,
                    "s0", flagCode,
                    "s1", flagCode,
                    limit, offset
            );
        } else {
            rows = jdbcTemplate.queryForList(
                    sql,
                    "s0", flagCode, flagState,
                    "s1", flagCode, flagState,
                    limit, offset
            );
        }
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/global/count")
    @Operation(summary = "전역 플래그 상태 카운트(샤드 합산)",
            description = "flagCode별 true/false 합계를 샤드 합산으로 반환")
    public ResponseEntity<Map<String, Object>> countFlagStateGlobal(
            @RequestParam String flagCode
    ) {
        String sql =
                "SELECT SUM(CASE WHEN flag_state = 1 THEN c ELSE 0 END) AS true_count, " +
                        "       SUM(CASE WHEN flag_state = 0 THEN c ELSE 0 END) AS false_count " +
                        "FROM ( " +
                        "  SELECT f.`condition` AS flag_state, COUNT(*) AS c " +
                        "    FROM cnwvdb_s0.flag_data f WHERE f.flag_code = ? GROUP BY f.`condition` " +
                        "  UNION ALL " +
                        "  SELECT f.`condition` AS flag_state, COUNT(*) AS c " +
                        "    FROM cnwvdb_s1.flag_data f WHERE f.flag_code = ? GROUP BY f.`condition` " +
                        ") t";

        return ResponseEntity.ok(jdbcTemplate.queryForMap(sql, flagCode, flagCode));
    }
}
