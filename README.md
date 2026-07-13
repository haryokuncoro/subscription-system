# Subscription System API

A Spring Boot REST API for managing users, subscription plans, subscriptions, and Stripe webhook synchronization.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Stripe Java SDK
- JWT Authentication

---

# Features

- JWT Authentication
- User Management
- Subscription Plan Management
- Subscription Management
- Stripe Invoice Integration
- Stripe Webhook Integration
- Database Migration (Flyway)
- Seed Development Data

---

# Authentication

The API uses JWT Bearer Authentication.

```
Authorization: Bearer <access_token>
```

Public endpoints:

```
POST /api/auth/register
POST /api/auth/login
POST /api/webhooks/stripe
```

All other endpoints require authentication.

---

# Running

## With Docker (recommended)

**1. Copy the environment template and fill in your secrets:**

```bash
cp .env.example .env
# edit .env with real values
```

The `.env` file is gitignored. At minimum set:

| Variable | Description |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Use `local` to load `application-local.properties` |
| `SPRING_DATASOURCE_URL` | e.g. `jdbc:postgresql://db:5432/subscription_system` |
| `SPRING_DATASOURCE_USERNAME` | Postgres user |
| `SPRING_DATASOURCE_PASSWORD` | Postgres password |
| `JWT_SECRET` | Random secret string for signing JWT tokens |
| `STRIPE_API_KEY_SG` / `_HK` / `_MY` | Stripe secret keys per region |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret |
| `STRIPE_SKIP_SIGNATURE_CHECK` | `true` for local dev, `false` in production |
| `STRIPE_MOCK_ENABLED` | `true` to use stripe-mock, `false` for real Stripe |
| `STRIPE_MOCK_BASE_URL` | e.g. `http://stripe-mock:12111` when mock is enabled |

**2. Build and start all services (Postgres + stripe-mock + app):**

```bash
docker compose up --build
```

The app will be available at `http://localhost:8080`.

Flyway migrations run automatically on startup. Swagger UI: `http://localhost:8080/swagger-ui.html`.

**Stop and remove containers:**

```bash
docker compose down
```

To also remove the database volume:

```bash
docker compose down -v
```

---

## Local Development (without Docker)

Requires a running PostgreSQL instance on `localhost:5433` (or adjust `application-local.properties`).

```bash
./gradlew bootRun
```

Secrets are read from environment variables. For convenience, `spring-dotenv` will pick up a `.env` file in the project root automatically.

---

# Seed Data

Generate sample users and plans.

```
POST /api/seed
```

This creates:

- 5 Users
- 5 Subscription Plans

---

# Authentication APIs

## Register

```
POST /api/auth/register
```

Request

```json
{
  "email": "john@example.com",
  "password": "password",
  "fullName": "John Doe",
  "country": "US"
}
```

Response

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "accessToken": "...",
    "tokenType": "Bearer",
    "expiresAt": "2026-06-30T10:00:00Z"
  },
  "timestamp": "2026-06-30T10:00:00Z"
}
```

---

## Login

```
POST /api/auth/login
```

Request

```json
{
  "email": "john@example.com",
  "password": "password"
}
```

---

# User APIs

| Method | Endpoint |
|---------|----------|
| GET | /api/users |
| GET | /api/users/{id} |
| POST | /api/users |
| PUT | /api/users/{id} |
| DELETE | /api/users/{id} |

Create Request

```json
{
  "email": "alice@example.com",
  "password": "password",
  "fullName": "Alice",
  "country": "US"
}
```

---

# Plan APIs

| Method | Endpoint |
|---------|----------|
| GET | /api/plans |
| GET | /api/plans/{id} |
| POST | /api/plans |
| PUT | /api/plans/{id} |
| DELETE | /api/plans/{id} |

Create Request

```json
{
  "name": "Pro Monthly",
  "description": "Professional subscription",
  "amount": 19.99,
  "currency": "USD",
  "country": "US",
  "billingInterval": "MONTH",
  "active": true
}
```

---

# Subscription APIs

| Method | Endpoint |
|---------|----------|
| GET | /api/subscriptions |
| GET | /api/subscriptions/{id} |
| POST | /api/subscriptions |
| PUT | /api/subscriptions/{id} |
| DELETE | /api/subscriptions/{id}?immediately=false |

Create Request

```json
{
  "userId": "c2a99b95-4d7b-4e7f-9b87-40e2a83b0f77",
  "planId": "74af8a4c-7a34-4b0f-96d0-59d0e95f9357",
  "currentPeriodStart": "2026-06-30T00:00:00Z",
  "currentPeriodEnd": "2026-07-30T00:00:00Z",
  "cancelAtPeriodEnd": false
}
```

---
# Invoice APIs

| Method | Endpoint                                  |
|---------|-------------------------------------------|
| GET | /api/invoices                             |

---
# Stripe Webhook

Stripe should send webhook events to:

```
POST /api/webhooks/stripe
```

Required Header

```
Stripe-Signature: t=1719700000,v1=xxxxxxxxxxxxxxxx
```

Example payload (`invoice.paid`)

```json
{
  "id": "evt_1RvXXXXXX",
  "object": "event",
  "type": "invoice.paid",
  "created": 1719700000,
  "data": {
    "object": {
      "id": "in_1RvXXXXX",
      "object": "invoice",
      "customer": "cus_test_001",
      "subscription": "sub_test_001",
      "payment_intent": "pi_test_001",
      "status": "paid",
      "number": "INV-0001",
      "subtotal": 999,
      "tax": 0,
      "total": 999,
      "currency": "usd",
      "hosted_invoice_url": "https://invoice.stripe.com/i/123456",
      "invoice_pdf": "https://pay.stripe.com/invoice/123456.pdf",
      "created": 1719700000
    }
  }
}
```

Example payload (`customer.subscription.updated`)

```json
{
  "id": "evt_1RvYYYYYY",
  "object": "event",
  "type": "customer.subscription.updated",
  "created": 1719700000,
  "data": {
    "object": {
      "id": "sub_test_001",
      "object": "subscription",
      "customer": "cus_test_001",
      "status": "active",
      "cancel_at_period_end": false,
      "current_period_start": 1719700000,
      "current_period_end": 1722378400,
      "items": {
        "data": [
          {
            "price": {
              "id": "price_pro_month"
            }
          }
        ]
      }
    }
  }
}
```

---

# Standard Response

All successful APIs return:

```json
{
  "success": true,
  "message": "Success",
  "data": {},
  "timestamp": "2026-06-30T10:00:00Z"
}
```

Error response:

```json
{
  "success": false,
  "message": "Resource not found",
  "data": null,
  "timestamp": "2026-06-30T10:00:00Z"
}
```

---

# Project Structure

```
controller/
service/
repository/
entity/
dto/
mapper/
config/
security/
exception/
util/
```

---

# API Documentation

Swagger UI

```
http://localhost:8080/swagger-ui.html
```

OpenAPI

```
http://localhost:8080/v3/api-docs
```