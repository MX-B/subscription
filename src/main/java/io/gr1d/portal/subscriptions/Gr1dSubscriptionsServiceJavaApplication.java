package io.gr1d.portal.subscriptions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ComponentScan("io.gr1d")
@EnableFeignClients({ "io.gr1d.portal.subscriptions.api", "io.gr1d.integrations.whitelabel" })
public class Gr1dSubscriptionsServiceJavaApplication {

    public static void main(final String[] args) {
        SpringApplication.run(Gr1dSubscriptionsServiceJavaApplication.class, args);
    }

}
