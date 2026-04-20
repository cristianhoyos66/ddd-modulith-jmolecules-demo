package com.example.demo.orders.domain;

import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public record OrderCompleted(Order.OrderIdentifier orderId) {}
