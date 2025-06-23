package br.com.lmuniz.desafio.senai.services;

import br.com.lmuniz.desafio.senai.domains.dtos.CouponCodeDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.DiscountDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.ProductDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.ProductDiscountDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponCodeDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.discounts.DiscountDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.products.ProductDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.products.ProductDiscountDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Coupon;
import br.com.lmuniz.desafio.senai.domains.entities.Product;
import br.com.lmuniz.desafio.senai.domains.entities.ProductCouponApplication;
import br.com.lmuniz.desafio.senai.domains.enums.CouponEnum;
import br.com.lmuniz.desafio.senai.repositories.CouponRepository;
import br.com.lmuniz.desafio.senai.repositories.ProductCouponApplicationRepository;
import br.com.lmuniz.desafio.senai.repositories.ProductRepository;
import br.com.lmuniz.desafio.senai.services.exceptions.BusinessRuleException;
import br.com.lmuniz.desafio.senai.services.exceptions.InvalidPriceException;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceConflictException;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceNotFoundException;
import br.com.lmuniz.desafio.senai.utils.Utils;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final ProductCouponApplicationRepository productCouponApplicationRepository;

    public ProductService(ProductRepository productRepository, CouponRepository couponRepository, ProductCouponApplicationRepository productCouponApplicationRepository) {
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.productCouponApplicationRepository = productCouponApplicationRepository;
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO dto){
        final String name = dto.name().trim().replaceAll("\\s+", " ");
        final String normalizedName = Utils.normalizeName(name);

        if(productRepository.existsByNormalizedName(normalizedName)){
            throw new ResourceConflictException("Product with name '" + name + "' already exists.");
        }

        Product entity = new Product(
                name,
                normalizedName,
                dto.description(),
                dto.price(),
                dto.stock()
        );
        entity = productRepository.save(entity);
        return new ProductDTO(entity);
    }

    @Transactional
    public void softDeleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id '" + id + "' not found."));

        product.setDeletedAt(Instant.now());
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
    }

    @Transactional
    public ProductDTO restoreProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id '" + id + "' not found."));

        if (product.getDeletedAt() == null) {
            throw new BusinessRuleException("Product is already active and cannot be restored.");
        }

        product.setDeletedAt(null);
        product.setUpdatedAt(Instant.now());
        product = productRepository.save(product);
        return new ProductDTO(product);
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        return productRepository.findById(id)
                .map(ProductDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id '" + id + "' not found."));
    }

    @Transactional
    public ProductDiscountDTO applyCouponDiscount(Long id, CouponCodeDTO couponCodeDTO) {
        final String normalizedCode = Utils.normalizeName(couponCodeDTO.code());
        Coupon coupon = couponRepository.findByCode(normalizedCode);
        if (coupon == null) {
            throw new ResourceNotFoundException("Coupon with code '" + couponCodeDTO.code() + "' not found.");
        }
        validateCoupon(coupon);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id '" + id + "' not found."));
        validateProduct(product);

        BigDecimal finalPrice = calculateFinalPrice(product, coupon);

        if (finalPrice.compareTo(new BigDecimal("0.01")) < 0) {
            throw new InvalidPriceException("Final price after applying coupon cannot less than 0.01");
        }

        ProductCouponApplication productCouponApplication = new ProductCouponApplication();
        productCouponApplication.setProduct(product);
        productCouponApplication.setCoupon(coupon);
        productCouponApplication.setAppliedAt(Instant.now());
        productCouponApplicationRepository.save(productCouponApplication);

        coupon.setUsesCount(coupon.getUsesCount() + 1);
        couponRepository.save(coupon);

        DiscountDTO discountDTO = new DiscountDTO(coupon, productCouponApplication);
        return new ProductDiscountDTO(product, finalPrice, discountDTO, true);
    }

    private BigDecimal calculateFinalPrice(Product product, Coupon coupon) {
        if (coupon.getType() == CouponEnum.PERCENT) {
            BigDecimal discountFactor = BigDecimal.ONE.subtract(coupon.getValue().divide(BigDecimal.valueOf(100)));
            return product.getPrice().multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
        } else {
            return product.getPrice().subtract(coupon.getValue());
        }
    }

    private void validateProduct(Product product) {
        if (product.getDeletedAt() != null) {
            throw new BusinessRuleException("Product is deleted and cannot have a coupon applied.");
        }

        if (productCouponApplicationRepository.existsByProductIdAndRemovedAtIsNull(product.getId())) {
            throw new ResourceConflictException("Coupon is already applied to this product.");
        }
    }

    private void validateCoupon(Coupon coupon) {
        if (coupon.getOneShot() && coupon.getUsesCount() > 0) {
            throw new ResourceConflictException("Coupon is one-shot and has already been used.");
        }

        if (coupon.getDeletedAt() != null) {
            throw new BusinessRuleException("Coupon is deleted and cannot be applied.");
        }

        if (coupon.getMaxUses() != null && coupon.getUsesCount() >= coupon.getMaxUses()) {
            throw new BusinessRuleException("Coupon has reached its maximum usage limit.");
        }

        Instant currentTime = Instant.now();
        if (coupon.getValidFrom().compareTo(currentTime) > 0 || coupon.getValidUntil().compareTo(currentTime) < 0) {
            throw new BusinessRuleException("Coupon is not valid for the current date.");
        }
    }
}
