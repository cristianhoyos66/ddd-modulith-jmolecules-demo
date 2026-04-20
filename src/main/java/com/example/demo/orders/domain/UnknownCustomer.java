package com.example.demo.orders.domain;

import com.example.demo.customers.domain.Customer;

public class UnknownCustomer extends RuntimeException {

  public UnknownCustomer(Customer.CustomerIdentifier id) {
    super("Unknown customer " + id + " — orders module has no record of this customer yet");
  }
}
