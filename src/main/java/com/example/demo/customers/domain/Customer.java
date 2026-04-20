package com.example.demo.customers.domain;

import java.util.UUID;
import org.jmolecules.ddd.types.AggregateRoot;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.data.domain.AbstractAggregateRoot;

public class Customer extends AbstractAggregateRoot<Customer>
    implements AggregateRoot<Customer, Customer.CustomerIdentifier> {

  private final CustomerIdentifier id;
  private String name;
  private EmailAddress email;

  public Customer(String name, EmailAddress email) {
    this.id = new CustomerIdentifier(UUID.randomUUID());
    this.name = name;
    this.email = email;
    registerEvent(new CustomerRegistered(id, email));
  }

  @Override
  public CustomerIdentifier getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public EmailAddress getEmail() {
    return email;
  }

  public record CustomerIdentifier(UUID id) implements Identifier {}
}
