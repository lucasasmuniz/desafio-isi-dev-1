package br.com.lmuniz.desafio.senai.repositories;

import br.com.lmuniz.desafio.senai.domains.entities.ProductCouponApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCouponApplicationRepository extends JpaRepository<ProductCouponApplication, Long> {
}
