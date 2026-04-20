package com.example.demo.catalog.domain;

import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public record ProductAdded(Product.ProductIdentifier productId, String name) {}
