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

Configure PostgreSQL and Stripe in `application.yml`.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/subscription
    username: postgres
    password: postgres

stripe:
  api-key: sk_test_xxxxxxxxx
  webhook-secret: whsec_xxxxxxxxx

jwt:
  secret: your-secret-key
  expiration: 86400000
```

Run Flyway migration.

```
./mvnw spring-boot:run
```

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