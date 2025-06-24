package br.com.lmuniz.desafio.senai.repositories;

import br.com.lmuniz.desafio.senai.domains.entities.ProductCouponApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ProductCouponApplicationRepository extends JpaRepository<ProductCouponApplication, Long> {

    ProductCouponApplication findByProductIdAndRemovedAtIsNull(Long productId);
    List<ProductCouponApplication>  findAllByProductIdInAndRemovedAtIsNull(List<Long> productIds);
    List<ProductCouponApplication> findAllByCouponIdAndRemovedAtIsNull(Long couponId);
    @Modifying
    @Query("UPDATE ProductCouponApplication pca SET pca.removedAt = :removedAt WHERE pca.coupon.id = :couponId AND pca.removedAt IS NULL")
    int removeActiveApplicationsByCouponId(@Param("couponId") Long couponId, @Param("removedAt") Instant removedAt);
}
