package com.example.demo.inventory.domain;

import com.example.demo.catalog.domain.Product;
import org.jmolecules.ddd.types.Association;
import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public record OutOfStock(Association<Product, Product.ProductIdentifier> product) {}
