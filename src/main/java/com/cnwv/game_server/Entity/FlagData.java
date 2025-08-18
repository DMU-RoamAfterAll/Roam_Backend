package com.cnwv.game_server.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "flag_data")
@Getter
@Setter
@NoArgsConstructor
public class FlagData {

    @EmbeddedId
    private FlagDataId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore // 응답 직렬화 시 User 상세 노출 방지
    private User user;

    // 예약어 매핑: 백틱 필수
    @Column(name = "`condition`", nullable = false)
    private boolean flagState; // =

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isCondition() { return flagState; }
}
