package br.com.lmuniz.desafio.senai.repositories;

import br.com.lmuniz.desafio.senai.domains.entities.ProductDirectDiscountApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductDirectDiscountApplicationRepository extends JpaRepository<ProductDirectDiscountApplication, Long> {

    ProductDirectDiscountApplication findByProductIdAndRemovedAtIsNull(Long productId);
}
