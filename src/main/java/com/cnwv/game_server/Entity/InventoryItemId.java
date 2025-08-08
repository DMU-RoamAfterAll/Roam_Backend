package com.cnwv.game_server.Entity;

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
    private Long inventoryId;
    private String itemCode;
}