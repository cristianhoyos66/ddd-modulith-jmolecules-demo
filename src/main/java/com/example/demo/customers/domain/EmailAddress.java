package com.example.demo.customers.domain;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record EmailAddress(String address) {

  public EmailAddress {
    if (address == null || !address.contains("@")) {
      throw new IllegalArgumentException("Invalid email address: " + address);
    }
  }
}
