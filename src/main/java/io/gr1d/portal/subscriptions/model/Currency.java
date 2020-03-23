package io.gr1d.portal.subscriptions.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Getter@Setter
@Table(name = "currency")
public class Currency {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "iso_code", nullable = false, length = 3)
    private String isoCode;

    @Column(name = "symbol", nullable = false, length = 4)
    private String symbol;

}
