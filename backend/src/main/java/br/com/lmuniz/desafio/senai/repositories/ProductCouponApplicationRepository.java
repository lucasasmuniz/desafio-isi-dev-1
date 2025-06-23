package br.com.lmuniz.desafio.senai.repositories;

import br.com.lmuniz.desafio.senai.domains.entities.ProductCouponApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductCouponApplicationRepository extends JpaRepository<ProductCouponApplication, Long> {

    ProductCouponApplication findByProductIdAndRemovedAtIsNull(Long productId);
}
