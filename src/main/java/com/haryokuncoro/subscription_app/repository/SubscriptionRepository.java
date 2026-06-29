package com.haryokuncoro.subscription_app.repository;

import com.haryokuncoro.subscription_app.dto.enums.SubscriptionStatus;
import com.haryokuncoro.subscription_app.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID>, JpaSpecificationExecutor<Subscription> {

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    Optional<Subscription> findByUserId(UUID userId);
    List<Subscription> findByStatus(SubscriptionStatus status);
    boolean existsByUserId(UUID userId);

}