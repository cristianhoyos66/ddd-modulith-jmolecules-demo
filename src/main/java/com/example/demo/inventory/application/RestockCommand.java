package com.example.demo.inventory.application;

import java.util.UUID;

public record RestockCommand(UUID productId, long amount) {}
