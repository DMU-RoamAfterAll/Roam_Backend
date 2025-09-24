package com.cnwv.game_server.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Embeddable
@EqualsAndHashCode
public class InventoryItemId implements Serializable {

    @Column(name = "inventory_id") // ★ snake_case 매핑
    private Long inventoryId;

    @Column(name = "item_code")    // ★ snake_case 매핑
    private String itemCode;
}
