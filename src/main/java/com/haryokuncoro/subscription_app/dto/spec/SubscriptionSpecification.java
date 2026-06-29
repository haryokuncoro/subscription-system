package com.haryokuncoro.subscription_app.dto.spec;

import com.haryokuncoro.subscription_app.entity.Subscription;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class SubscriptionSpecification {
    public static Specification<Subscription> hasUser(UUID userId) {
        return (root, query, cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Subscription> hasPlan(UUID planId) {
        return (root, query, cb) ->
                cb.equal(root.get("plan").get("id"), planId);
    }

    public static Specification<Subscription> hasStatus(String status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }
}
