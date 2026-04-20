# Feature request

> Fill each section. Leave nothing empty. When you're done, run
> `/generate-prp INITIAL.md` to produce a PRP the next session can execute.

## FEATURE

<!--
What are you building? Be concrete about the *what* and the *outcome*.
Name the module (existing or new), the endpoint or behavior, and the success signal.

Example:
"Add a `notifications` module that sends a welcome email via an external HTTP
service when a `CustomerRegistered` event fires. No retry needed for v1 — best effort."
-->

## EXAMPLES

<!--
Which existing code should the implementation pattern-match against?
List file paths + what to imitate.

Example:
- `customers/infrastructure/LoggingWelcomeNotifier.java` — the port/adapter pattern,
  interface in application + adapter in infrastructure.
- `inventory/infrastructure/InventoryListener.java` — @ApplicationModuleListener
  consuming cross-module events.
- `orders/application/OrderService.java` — how cross-module calls look from an
  application service.
-->

## DOCUMENTATION

<!--
External specs, API docs, libraries the implementation will need.
Include URLs and the sections that matter.

Example:
- SendGrid v3 API — POST /v3/mail/send: https://docs.sendgrid.com/...
- Spring RestClient: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient
-->

## OTHER CONSIDERATIONS

<!--
Constraints, gotchas, non-functional requirements, known failure modes.

Example:
- Must not block the publishing transaction if the HTTP call is slow — use @ApplicationModuleListener's default async.
- SendGrid API key via Spring config, never hardcoded.
- No retries in v1. If the call fails, log and move on.
- Must not break `EndToEndOrderFlowTests` (which doesn't currently exercise notifications).
-->
