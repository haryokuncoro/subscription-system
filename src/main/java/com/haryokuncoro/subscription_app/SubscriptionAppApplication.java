package com.haryokuncoro.subscription_app;

import com.haryokuncoro.subscription_app.stripe.StripeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(StripeProperties.class)
public class SubscriptionAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(SubscriptionAppApplication.class, args);
	}

}
