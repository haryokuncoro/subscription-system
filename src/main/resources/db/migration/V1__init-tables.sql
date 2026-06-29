-- V1__create_subscription_tables.sql

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==========================================================
-- USERS
-- ==========================================================

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       version BIGINT NOT NULL DEFAULT 0,

                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash TEXT NOT NULL,
                       full_name VARCHAR(255),
                       country VARCHAR(255),

                       stripe_customer_id VARCHAR(255) UNIQUE,


                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_stripe_customer_id ON users(stripe_customer_id);

-- ==========================================================
-- PLANS
-- ==========================================================

CREATE TABLE plans (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       version BIGINT NOT NULL DEFAULT 0,

                       name VARCHAR(100) NOT NULL,
                       description TEXT,

                       stripe_product_id VARCHAR(255) NOT NULL UNIQUE,
                       stripe_price_id VARCHAR(255) NOT NULL UNIQUE,

                       amount_cents BIGINT NOT NULL,
                       currency VARCHAR(3) NOT NULL,
                       country VARCHAR(255),

                       billing_interval VARCHAR(20) NOT NULL, -- month | year

                       active BOOLEAN NOT NULL DEFAULT TRUE,

                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==========================================================
-- SUBSCRIPTIONS
-- ==========================================================

CREATE TABLE subscriptions (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               version BIGINT NOT NULL DEFAULT 0,

                               user_id UUID NOT NULL REFERENCES users(id),
                               plan_id UUID NOT NULL REFERENCES plans(id),

                               stripe_subscription_id VARCHAR(255) NOT NULL UNIQUE,

                               status VARCHAR(30) NOT NULL,

                               current_period_start TIMESTAMPTZ,
                               current_period_end TIMESTAMPTZ,

                               cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
                               canceled_at TIMESTAMPTZ,

                               created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_user_id
    ON subscriptions(user_id);

CREATE INDEX idx_subscriptions_status
    ON subscriptions(status);

CREATE INDEX idx_subscriptions_stripe_subscription_id
    ON subscriptions(stripe_subscription_id);

-- ==========================================================
-- INVOICES
-- ==========================================================

CREATE TABLE invoices (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          version BIGINT NOT NULL DEFAULT 0,

                          subscription_id UUID NOT NULL REFERENCES subscriptions(id),

                          stripe_invoice_id VARCHAR(255) NOT NULL UNIQUE,
                          stripe_payment_intent_id VARCHAR(255),

                          invoice_number VARCHAR(100),

                          status VARCHAR(30) NOT NULL,

                          subtotal NUMERIC(12,2) NOT NULL,
                          tax NUMERIC(12,2) NOT NULL DEFAULT 0,
                          discount NUMERIC(12,2) NOT NULL DEFAULT 0,
                          total NUMERIC(12,2) NOT NULL,

                          currency VARCHAR(3) NOT NULL,

                          hosted_invoice_url TEXT,
                          invoice_pdf TEXT,

                          invoice_date TIMESTAMPTZ,
                          due_date TIMESTAMPTZ,
                          paid_at TIMESTAMPTZ,

                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoices_subscription_id
    ON invoices(subscription_id);

CREATE INDEX idx_invoices_status
    ON invoices(status);

CREATE INDEX idx_invoices_stripe_invoice_id
    ON invoices(stripe_invoice_id);

-- ==========================================================
-- WEBHOOK EVENTS
-- ==========================================================

CREATE TABLE webhook_events (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                version BIGINT NOT NULL DEFAULT 0,

                                stripe_event_id VARCHAR(255) NOT NULL UNIQUE,

                                event_type VARCHAR(100) NOT NULL,

                                payload JSONB NOT NULL,

                                processed BOOLEAN NOT NULL DEFAULT FALSE,

                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()

);

CREATE INDEX idx_webhook_events_processed
    ON webhook_events(processed);

CREATE INDEX idx_webhook_events_event_type
    ON webhook_events(event_type);