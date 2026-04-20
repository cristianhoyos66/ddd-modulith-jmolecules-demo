---
paths:
  - "src/test/**/*.java"
---

# Rules for tests

Loaded only when working on files under `src/test/`.

## Test types used in this repo

| Annotation | When | Example |
|---|---|---|
| `@SpringBootTest` | Full app context; event-driven scenarios | `EndToEndOrderFlowTests` |
| `@ApplicationModuleTest` | Single module sliced; proves it boots standalone | `CustomersModuleTests`, `CatalogModuleTests` |
| Plain `@Test` | Architectural rule (ArchUnit / Modulith verify) | `ModulithStructureTests`, `JMoleculesRulesTests` |

## Event publication assertions

When a use case publishes a domain event, assert it fires via `PublishedEvents`:

```java
@ApplicationModuleTest
class FooModuleTests {

  @Autowired FooService service;

  @Test
  void doingXPublishesFooEvent(PublishedEvents events) {
    var view = service.doX(new XCommand(...));

    assertThat(events.ofType(FooEvent.class).matching(e -> e.id().equals(view.id())))
        .hasSize(1);
  }
}
```

## Async / eventual consistency

Use Awaitility, never `Thread.sleep`:

```java
await().atMost(Duration.ofSeconds(3))
    .untilAsserted(() -> assertThat(service.stateOf(...)).isEqualTo(expected));
```

## Architectural tests

`ModulithStructureTests` and `JMoleculesRulesTests` encode invariants. If they fail:

- Don't modify the test to make it pass.
- Fix the production code that violated the invariant.

If you genuinely need a new invariant, add a **new** test class — don't mutate the existing ones.

## Module tests

- Use `@ApplicationModuleTest` per module to prove module isolation.
- Don't `@MockBean` a repository — real H2 is used with real Flyway migrations.
- Assert on behavior + events published, not on internal state.

## Test data

- Create data fresh in each test via the public API (call `service.register(...)` rather than `repository.save(new Customer(...))`). Using public API catches breakage in the use case flow.
- If you need fixtures, use small builders or factory methods — don't pull in heavy fixtures libs.

## Don't

- Don't disable a failing test with `@Disabled` to "come back to it later." Flakiness is a
  signal — usually missing `await()` for async flows.
- Don't mock what you don't own (the framework). Use real Spring + real H2.
- Don't write tests that only verify the framework (e.g., "Spring injects a bean").
- Don't write big end-to-end tests for logic that belongs in a unit test on a domain service
  or aggregate. Keep the pyramid shape.
