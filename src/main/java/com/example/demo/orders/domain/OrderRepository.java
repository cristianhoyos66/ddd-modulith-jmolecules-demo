package com.example.demo.orders.domain;

import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, Order.OrderIdentifier> {}
