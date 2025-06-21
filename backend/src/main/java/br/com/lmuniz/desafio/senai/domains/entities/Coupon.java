package br.com.lmuniz.desafio.senai.domains.entities;

import br.com.lmuniz.desafio.senai.domains.enums.CouponEnum;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CouponEnum type;

    @Column(name = "coupon_value", nullable = false)
    private BigDecimal value;

    @Column(nullable = false)
    private boolean oneShot;

    private Integer maxUses;

    private Integer usesCount;

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Instant validFrom;

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Instant validUntil;

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITHOUT TIME ZONE", updatable = false)
    private Instant createdAt;

    @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE", insertable = false)
    private Instant updatedAt;

    @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
    private Instant deletedAt;

    @OneToMany(mappedBy = "coupon", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ProductCouponApplication> productCouponApplications = new ArrayList<>();

    public Coupon() {
    }

    public Coupon(String code, CouponEnum type, BigDecimal value, boolean oneShot, Integer maxUses, Instant validFrom, Instant validUntil) {
        this.code = code;
        this.type = type;
        this.value = value;
        this.oneShot = oneShot;
        this.maxUses = maxUses;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public CouponEnum getType() {
        return type;
    }

    public void setType(CouponEnum type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public boolean getOneShot() {
        return oneShot;
    }

    public void setOneShot(boolean oneShot) {
        this.oneShot = oneShot;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    public Integer getUsesCount() {
        return usesCount;
    }

    public void setUsesCount(Integer usesCount) {
        this.usesCount = usesCount;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public List<ProductCouponApplication> getProductCouponApplications() {
        return productCouponApplications;
    }

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
        if (usesCount == null) {
            usesCount = 0;
        }
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Coupon coupon = (Coupon) o;
        return Objects.equals(id, coupon.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
