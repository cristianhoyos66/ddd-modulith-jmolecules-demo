package com.example.demo.orders.application;

import java.math.BigDecimal;
import java.util.UUID;

/** Application-layer read model returned by OrderService.calculateTotal. */
public record OrderTotalView(UUID orderId, BigDecimal total, String currency) {}
