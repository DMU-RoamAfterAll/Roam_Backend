package com.cnwv.game_server.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_stats")
@Getter
@Setter
@NoArgsConstructor
public class PlayerStats {

    @Id
    @Column(name = "user_id")
    private Long userId; // PK = FK(users.id)

    // (선택) 양방향이 꼭 필요없으면 매핑 생략해도 됨
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "hp", nullable = false)
    private int hp = 100;

    @Column(name = "atk", nullable = false)
    private int atk = 10;

    @Column(name = "spd", nullable = false)
    private int spd = 10;

    @Column(name = "hit_rate", nullable = false)
    private int hitRate = 70;

    @Column(name = "evasion_rate", nullable = false)
    private int evasionRate = 12;

    @Column(name = "counter_rate", nullable = false)
    private int counterRate = 3;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 낙관적 락 */
    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
