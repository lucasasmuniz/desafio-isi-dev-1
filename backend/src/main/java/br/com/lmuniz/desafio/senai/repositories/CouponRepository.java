package br.com.lmuniz.desafio.senai.repositories;

import br.com.lmuniz.desafio.senai.domains.entities.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    boolean existsByCode(String code);

    @Query("SELECT c FROM Coupon c WHERE c.deletedAt IS NULL AND :now BETWEEN c.validFrom AND c.validUntil AND ((c.maxUses IS NULL AND not(c.oneShot)) OR c.usesCount < c.maxUses OR (c.maxUses IS NULL AND c.oneShot AND c.usesCount = 0))")
    List<Coupon> searchValidCoupons(@Param("now") Instant now);

    Optional<Coupon> findByCodeAndIdNot(String code, Long id);
    Coupon findByCode(String code);
}
