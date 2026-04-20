package com.example.demo.inventory.application;

import com.example.demo.inventory.domain.InventoryItem;
import java.util.UUID;

/** Application-layer read model returned by InventoryService. */
public record InventoryItemView(UUID id, UUID productId, long stock) {

  static InventoryItemView from(InventoryItem item) {
    return new InventoryItemView(
        item.getId().id(), item.getProduct().getId().id(), item.getStock());
  }
}
