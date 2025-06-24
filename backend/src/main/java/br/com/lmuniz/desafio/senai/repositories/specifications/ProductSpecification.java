package br.com.lmuniz.desafio.senai.repositories.specifications;

import br.com.lmuniz.desafio.senai.domains.entities.Product;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecification {

    public static Specification<Product> nameOrDescriptionLike(String search) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + search.toLowerCase() + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("normalizedName")), "%" + search.toLowerCase() + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + search.toLowerCase() + "%")
                );
    }

    public static Specification<Product> priceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Product> isOutOfStock() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("stock"), 0);
    }

    public static Specification<Product> isActive() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isNull(root.get("deletedAt"));
    }

    public static Specification<Product> hasActiveDiscount() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.or(
                    criteriaBuilder.exists((Subquery<?>) root.join("productCouponApplications").get("removedAt").isNull()),
                    criteriaBuilder.exists((Subquery<?>) root.join("productDirectDiscountApplication").get("removedAt").isNull())
            );
        };
    }

    public static Specification<Product> hasCouponApplied() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.exists((Subquery<?>) root.join("productCouponApplications").get("removedAt").isNull());
    }
}
