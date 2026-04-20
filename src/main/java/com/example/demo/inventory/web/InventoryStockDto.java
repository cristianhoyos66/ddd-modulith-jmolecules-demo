package com.example.demo.inventory.web;

import java.util.UUID;

public record InventoryStockDto(UUID productId, long stock) {}
