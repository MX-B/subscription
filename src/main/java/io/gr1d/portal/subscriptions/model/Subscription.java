package io.gr1d.portal.subscriptions.model;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import io.gr1d.core.datasource.audit.AuditListener;
import io.gr1d.core.datasource.model.BaseModel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "subscription")
@EntityListeners(AuditListener.class)
public class Subscription extends BaseModel {

    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "payment_method")
    private String paymentMethod;

    @ManyToOne
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Column(name = "subscription_start")
    private LocalDate subscriptionStart;

    @Column(name = "subscription_end")
    private LocalDate subscriptionEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_subscription_id")
    private Subscription nextSubscription;

}
