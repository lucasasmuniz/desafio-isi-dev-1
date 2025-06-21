package br.com.lmuniz.desafio.senai.repositories;

import br.com.lmuniz.desafio.senai.domains.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByNormalizedName(String normalizedName);
}
