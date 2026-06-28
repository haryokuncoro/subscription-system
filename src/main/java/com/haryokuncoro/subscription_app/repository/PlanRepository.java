package com.haryokuncoro.subscription_app.repository;

import com.haryokuncoro.subscription_app.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

}
