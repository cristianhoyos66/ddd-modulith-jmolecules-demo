package com.example.demo.orders.application;

import java.util.UUID;

public record CompleteOrderCommand(UUID orderId) {}
