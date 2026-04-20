package com.example.demo.customers.domain;

import org.springframework.data.repository.CrudRepository;

public interface CustomerRepository extends CrudRepository<Customer, Customer.CustomerIdentifier> {}
