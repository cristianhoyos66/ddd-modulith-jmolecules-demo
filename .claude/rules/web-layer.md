---
paths:
  - "src/main/java/com/example/demo/*/web/**/*.java"
---

# Rules for the `web/` layer

Loaded only when working on files under any module's `web/` package. Controllers and their
request/response records.

## Controllers

- Package-private class annotated `@org.springframework.web.bind.annotation.RestController`.
- `@RequestMapping("/<resource>")` at class level.
- Constructor injection — no field injection.
- Methods package-private, return `ResponseEntity<Dto>`.

## Request → Command mapping

- Controller method signatures take `*Request` records (HTTP-shaped) — never `*Command` records directly.
- In the method body, translate `Request → Command`, then call the application service.

```java
@PostMapping
ResponseEntity<OrderDto> place(@RequestBody PlaceOrderRequest request) {
  var lines = request.lines().stream()
      .map(l -> new PlaceOrderCommand.Line(l.productId(), l.quantity()))
      .toList();
  var view = service.place(new PlaceOrderCommand(request.customerId(), lines));
  return ResponseEntity.ok(OrderDto.from(view));
}
```

## View → Dto mapping

- Application services return `*View` records (application-layer).
- Controllers call `Dto.from(view)` to produce the HTTP response body.
- `*Dto` records have `static from(View)` factories — same shape as `View.from(Aggregate)` in the application layer.

## Why both layers (View + Dto)

- **View** is owned by the application layer. Its shape serves the use case.
- **Dto** is owned by the web layer. Its shape serves the HTTP contract.

They often look identical today. They exist separately so that when HTTP needs to diverge
(versioning, field rename, hide a field in v2), you touch `web/` without changing the
application layer.

## Package-info

- `@org.jspecify.annotations.NullMarked`.
- Do **not** mark `web/` as `@NamedInterface`. It's internal. Other modules don't import
  controllers.

## Don't

- Don't inject repositories into controllers. Controllers call services, services use repos.
- Don't inject `ApplicationEventPublisher`. Events are raised by aggregates.
- Don't return domain aggregates from handlers.
- Don't accept domain types (`Customer`, `Order`) as `@RequestBody`. Use Request records.
- Don't add business rules in the controller. Everything non-HTTP belongs in the service or domain.
- Don't annotate the controller with `@Transactional`. Transactions belong in the service.
