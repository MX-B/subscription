package io.gr1d.portal.subscriptions.model;

import javax.persistence.*;
import java.util.*;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "plan_modality")
public class PlanModality {

    public static final PlanModality UNLIMITED = new PlanModality(1L, "UNLIMITED");
    public static final PlanModality DEFAULT = new PlanModality(2L, "DEFAULT");
    public static final PlanModality FREE = new PlanModality(3L, "FREE");
    public static final PlanModality UNLIMITED_RANGE = new PlanModality(4L, "UNLIMITED_RANGE");
    public static final PlanModality SANDBOX = new PlanModality(5L, "SANDBOX");
    public static final PlanModality REMUNERADA = new PlanModality(6L, "REMUNERADA");

    private static final Map<String, PlanModality> modalities = new HashMap<>();

    static {
        modalities.put(UNLIMITED.getCode(), UNLIMITED);
        modalities.put(DEFAULT.getCode(), DEFAULT);
        modalities.put(FREE.getCode(), FREE);
        modalities.put(UNLIMITED_RANGE.getCode(), UNLIMITED_RANGE);
        modalities.put(SANDBOX.getCode(), SANDBOX);
        modalities.put(REMUNERADA.getCode(), REMUNERADA);
    }

    public static PlanModality valueOf(String code) {
        return modalities.get(code);
    }

    public static Collection<PlanModality> values(PlanModality... except) {
        Collection<PlanModality> values = new ArrayList<>(modalities.values());
        Arrays.stream(except).forEach(values::remove);
        return values;
    }
    
    /**
     * Todos os valores
     * @return a lista de modalidades
     */
    public static Collection<PlanModality> values() {
        return modalities.values();
    }

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column
    private String code;

    public PlanModality() {
        super();
    }

    public PlanModality(Long id, String code) {
        this();
        setId(id);
        setCode(code);
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof PlanModality && getId() != null && getId().equals(((PlanModality) obj).getId());
    }
}
