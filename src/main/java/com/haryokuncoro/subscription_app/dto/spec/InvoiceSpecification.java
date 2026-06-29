package com.haryokuncoro.subscription_app.dto.spec;

import com.haryokuncoro.subscription_app.entity.Invoice;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class InvoiceSpecification {
    public static Specification<Invoice> hasUser(UUID userId) {
        return (root, query, cb) ->
                cb.equal(root.get("subscription").get("user").get("id"), userId);
    }
    public static Specification<Invoice> hasSubscription(UUID subscriptionId) {
        return (root, query, cb) ->
                cb.equal(root.get("subscription").get("id"), subscriptionId);
    }
    public static Specification<Invoice> hasStatus(String status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }
    public static Specification<Invoice> hasStripeInvoiceId(String stripeInvoiceId) {
        return (root, query, cb) ->
                cb.equal(root.get("stripeInvoiceId"), stripeInvoiceId);
    }

}
