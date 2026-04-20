package com.example.demo.catalog.domain;

import java.util.UUID;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.data.domain.AbstractAggregateRoot;

public class Product extends AbstractAggregateRoot<Product>
    implements AggregateRoot<Product, Product.ProductIdentifier> {

  private final ProductIdentifier id;
  private String name;
  private Money price;

  public Product(String name, Money price) {
    this.id = new ProductIdentifier(UUID.randomUUID());
    this.name = name;
    this.price = price;
    registerEvent(new ProductAdded(id, name));
  }

  @Override
  public ProductIdentifier getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Money getPrice() {
    return price;
  }

  public record ProductIdentifier(UUID id) implements Identifier {}
}
