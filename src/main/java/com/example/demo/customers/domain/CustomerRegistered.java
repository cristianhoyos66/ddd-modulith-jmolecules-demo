package com.example.demo.customers.domain;

import org.jmolecules.event.annotation.DomainEvent;

@DomainEvent
public record CustomerRegistered(Customer.CustomerIdentifier customerId, EmailAddress email) {}
