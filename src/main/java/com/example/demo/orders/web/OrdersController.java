package com.example.demo.orders.web;

import com.example.demo.orders.application.CompleteOrderCommand;
import com.example.demo.orders.application.OrderService;
import com.example.demo.orders.application.PlaceOrderCommand;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
class OrdersController {

  private final OrderService service;

  OrdersController(OrderService service) {
    this.service = service;
  }

  @PostMapping
  ResponseEntity<OrderDto> place(@RequestBody PlaceOrderRequest request) {
    var lines =
        request.lines().stream()
            .map(l -> new PlaceOrderCommand.Line(l.productId(), l.quantity()))
            .toList();
    var view = service.place(new PlaceOrderCommand(request.customerId(), lines));
    return ResponseEntity.ok(OrderDto.from(view));
  }

  @PostMapping("/{id}/complete")
  ResponseEntity<OrderDto> complete(@PathVariable UUID id) {
    return ResponseEntity.ok(OrderDto.from(service.complete(new CompleteOrderCommand(id))));
  }

  @GetMapping("/{id}")
  ResponseEntity<OrderDto> get(@PathVariable UUID id) {
    return ResponseEntity.ok(OrderDto.from(service.get(id)));
  }

  @GetMapping("/{id}/total")
  ResponseEntity<OrderTotalDto> total(@PathVariable UUID id) {
    return ResponseEntity.ok(OrderTotalDto.from(service.calculateTotal(id)));
  }
}
