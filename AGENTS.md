# AGENTS — How to be productive in this codebase

This file captures practical, project-specific knowledge an AI coding agent needs to be immediately productive.

Quick summary
- Spring Boot 3 (Java 17) monolith that exposes REST endpoints + small Thymeleaf UI.
- Stripe integration with multi-region API keys and an option to mock Stripe (see `src/main/java/.../stripe`).
- Postgres + Flyway migrations stored in `src/main/resources/db/migration`.

Quick start (run & test)
- Run in dev: `./gradlew bootRun` (uses Java toolchain 17). For a packaged app: `./gradlew bootJar && java -jar build/libs/*.jar`.
- Tests: `./gradlew test` (JUnit Platform).
- DB migrations: Flyway auto-runs (enabled in `application-local.properties`) and the migration SQLs are in `src/main/resources/db/migration/V1__init-tables.sql`.

Environment & secrets (how config is loaded)
- The project uses Spring properties with environment interpolation in `src/main/resources/application-local.properties`.
- Important env vars:
  - `JWT_SECRET`, `STRIPE_API_KEY_SG`, `STRIPE_API_KEY_HK`, `STRIPE_API_KEY_MY`
  - `STRIPE_WEBHOOK_SECRET`, `STRIPE_SKIP_SIGNATURE_CHECK` (true/false), `STRIPE_MOCK_ENABLED`, `STRIPE_MOCK_BASE_URL`
 - The project can read secrets from environment variables. For local convenience `spring-dotenv` will pick up a `.env` file if present.
  - The `docker-compose.yml` uses shell-style substitution (`${VAR:-default}`) so variables can be provided by the environment at runtime (CI/CD, systemd, ECS task definition, AWS SSM parameter injection) or fall back to safe defaults for local development.
  - Keep `.env` out of VCS. Use `.env.example` with placeholders (already added) as a template.

Tips for servers and CI/CD:
- AWS: inject parameters into your container runtime (ECS task definition environmentVariables, or use SSM Parameter Store/Secrets Manager coupled with your deployment tool) — Compose itself does not fetch SSM values automatically; you must provide them in the runtime environment.
- For systemd or Docker on a host, export the variables in the shell that runs `docker compose up` or put them in a systemd unit EnvironmentFile.
- For production, consider Docker secrets or an external secret manager instead of plaintext `.env` files.

Stripe specifics (critical integration points)
- Stripe configuration classes:
  - `src/main/java/com/haryokuncoro/subscription_app/stripe/StripeProperties.java` — holds regional keys and mock flags.
  - `src/main/java/com/haryokuncoro/subscription_app/stripe/StripeKeyResolver.java` — resolves API key by country string (expects values like "singapore", "hong kong", "malaysia").
  - `src/main/java/com/haryokuncoro/subscription_app/config/StripeConfig.java` — overrides Stripe API base when `mockEnabled` is true.
- Webhooks:
  - Endpoint: POST /api/webhooks/stripe implemented in `WebhookController`.
  - Verification: `StripeWebhookService` will either call `Webhook.constructEvent(payload, signature, webhookSecret)` or, when `stripe.skipSignatureCheck=true`, it deserializes the payload directly (useful in local tests). This behavior is controlled by `STRIPE_SKIP_SIGNATURE_CHECK`.
  - Example curl to simulate a webhook (signature check disabled):

    curl -X POST -H "Content-Type: application/json" \
      -H "Stripe-Signature: t=0,v1=fake" \
      --data @sample-invoice.json http://localhost:8080/api/webhooks/stripe

Project structure & important packages
- Controllers: `src/main/java/com/haryokuncoro/subscription_app/controller` (AuthController, UserController, PlanController, SubscriptionController, InvoiceController, WebhookController).
- Services: `.../service` (business logic). Example: `StripeService`, `SubscriptionService`, `InvoiceService`, `StripeWebhookService`.
- Persistence: `.../repository`, entities in `.../entity`, and Flyway SQL in resources.
- UI: Thymeleaf templates under `src/main/resources/templates` and client JS under `src/main/resources/static/js`. The JS helper `api.js` attaches `Authorization: Bearer <token>` header for API calls.

Patterns and conventions discovered (do not change without care)
- Standard API response shape is used across controllers (see README examples): { success, message, data, timestamp }.
- Lombok is used widely (e.g. `@RequiredArgsConstructor`) — constructors and getters may be generated at compile time.
- Properties records: `StripeProperties` is a Java record annotated with `@ConfigurationProperties(prefix = "stripe")` — expect constructor-style access (`properties.apiKeySG()`).
- Country names are matched with exact lowercase strings in `StripeKeyResolver`. If adding countries, update resolver.

Developer workflows & tips
- Local dev: prefer `application-local.properties` (uses env vars) + `.env` file for convenience. Keep secrets out of VCS.
- To test webhooks locally: set `STRIPE_SKIP_SIGNATURE_CHECK=true` or run the exact signature verification path with a real Stripe webhook signing secret.
- Seed data: README documents `POST /api/seed` to insert sample users/plans — check `controller` for the seed endpoint implementation.
- Debugging: run from IDE (IntelliJ) or `./gradlew bootRun` and attach remote debugger. Devtools is enabled (hot reload) via `spring-boot-devtools`.

Where to look first when changing payments or webhooks
- `StripeService` — core Stripe API interactions (create customer, prices, subscriptions).
- `StripeWebhookService` — webhook verification and dispatch to `InvoiceService`.
- `InvoiceService` — maps Stripe invoice events to local DB records.

Other useful references
- Build config: `build.gradle` (Java 17 toolchain, Spring Boot plugin version and Stripe SDK version).
- Readme for API details and example payloads: `README.md`.

If you're an AI agent modifying code
- Always run `./gradlew test` after changes; run Flyway migrations against a transient Postgres instance when modifying schema.
- When changing Stripe behavior, ensure tests cover both `skipSignatureCheck=true` and signature-verified paths.
- When adding new configuration properties, register them in a `@ConfigurationProperties` record/class and wire default values via `application-local.properties`.

End of AGENTS.md

