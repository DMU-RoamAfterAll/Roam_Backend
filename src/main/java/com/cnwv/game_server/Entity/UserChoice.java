package com.cnwv.game_server.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_choices")
@Getter
@Setter
@NoArgsConstructor
public class UserChoice {

    @EmbeddedId
    private UserChoiceId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 예약어 매핑: 반드시 백틱으로 감싸서 사용
    @Column(name = "`condition`", nullable = false)
    private boolean condition;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 편의 메서드 (선택)
    public boolean isCondition() {
        return condition;
    }
}
