package br.com.lmuniz.desafio.senai.services;

import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponCodeDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.discounts.DirectPercentageDiscountDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.discounts.DiscountDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.products.ProductDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.products.ProductDiscountDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Coupon;
import br.com.lmuniz.desafio.senai.domains.entities.Product;
import br.com.lmuniz.desafio.senai.domains.entities.ProductCouponApplication;
import br.com.lmuniz.desafio.senai.domains.entities.ProductDirectDiscountApplication;
import br.com.lmuniz.desafio.senai.domains.enums.CouponEnum;
import br.com.lmuniz.desafio.senai.repositories.CouponRepository;
import br.com.lmuniz.desafio.senai.repositories.ProductCouponApplicationRepository;
import br.com.lmuniz.desafio.senai.repositories.ProductDirectDiscountApplicationRepository;
import br.com.lmuniz.desafio.senai.repositories.ProductRepository;
import br.com.lmuniz.desafio.senai.repositories.specifications.ProductSpecification;
import br.com.lmuniz.desafio.senai.services.exceptions.*;
import br.com.lmuniz.desafio.senai.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final ProductCouponApplicationRepository productCouponApplicationRepository;
    private final ProductDirectDiscountApplicationRepository productDirectDiscountApplicationRepository;
    private final ObjectMapper objectMapper;

    public ProductService(ProductRepository productRepository, CouponRepository couponRepository, ProductCouponApplicationRepository productCouponApplicationRepository, ProductDirectDiscountApplicationRepository productDirectDiscountApplicationRepository, ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.productCouponApplicationRepository = productCouponApplicationRepository;
        this.productDirectDiscountApplicationRepository = productDirectDiscountApplicationRepository;
        this.objectMapper = objectMapper;
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
                dto.price().setScale(2, RoundingMode.HALF_UP),
                dto.stock()
        );
        entity = productRepository.save(entity);
        return new ProductDTO(entity);
    }

    @Transactional
    public void softDeleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id '" + id + "' not found."));

        hasDiscountCheck(product);

        product.setDeletedAt(Instant.now());
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
    }

    private void hasDiscountCheck(Product product) {
        if(productCouponApplicationRepository.findByProductIdAndRemovedAtIsNull(product.getId()) != null ||
                productDirectDiscountApplicationRepository.findByProductIdAndRemovedAtIsNull(product.getId()) != null) {
            this.removeDiscount(product.getId());
        }
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
        validateProductAvailableDiscount(product);

        BigDecimal finalPrice = calculateFinalPrice(product, coupon);

        if (finalPrice.compareTo(new BigDecimal("0.01")) < 0) {
            throw new InvalidPriceException("Final price after applying coupon cannot less than 0.01");
        }

        ProductCouponApplication productCouponApplication = new ProductCouponApplication();
        productCouponApplication.setProduct(product);
        productCouponApplication.setCoupon(coupon);
        productCouponApplication.setAppliedAt(Instant.now());
        productCouponApplication = productCouponApplicationRepository.save(productCouponApplication);

        coupon.setUsesCount(coupon.getUsesCount() + 1);
        couponRepository.save(coupon);

        DiscountDTO discountDTO = new DiscountDTO(coupon, productCouponApplication);
        return new ProductDiscountDTO(product, finalPrice, discountDTO, true);
    }

    @Transactional
    public ProductDiscountDTO applyDirectPercentDiscount(Long id, DirectPercentageDiscountDTO directPercentageDiscountDTO) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id '" + id + "' not found."));

        validateProductAvailableDiscount(product);

        BigDecimal discountFraction = directPercentageDiscountDTO.percentage().divide(BigDecimal.valueOf(100));
        BigDecimal discountFactor = BigDecimal.ONE.subtract(discountFraction);
        BigDecimal finalPrice = product.getPrice().multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);

        if (finalPrice.compareTo(new BigDecimal("0.01")) < 0) {
            throw new InvalidPriceException("Final price after applying discount cannot be less than 0.01");
        }

        ProductDirectDiscountApplication directDiscountApplication = new ProductDirectDiscountApplication();
        directDiscountApplication.setProduct(product);
        directDiscountApplication.setDiscountPercentage(directPercentageDiscountDTO.percentage());
        directDiscountApplication.setAppliedAt(Instant.now());
        directDiscountApplication = productDirectDiscountApplicationRepository.save(directDiscountApplication);

        DiscountDTO discountDTO = new DiscountDTO(
                CouponEnum.PERCENT.getTypeValue(),
                directPercentageDiscountDTO.percentage(),
                directDiscountApplication.getAppliedAt()
        );

        return new ProductDiscountDTO(product, finalPrice, discountDTO, false);
    }

    @Transactional
    public void removeDiscount(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id '" + id + "' not found."));

        ProductCouponApplication productCouponApplication = productCouponApplicationRepository.findByProductIdAndRemovedAtIsNull(product.getId());
        if (productCouponApplication != null) {
            Coupon coupon = productCouponApplication.getCoupon();
            if (coupon.getUsesCount() <= 0) {
                throw new DatabaseException(
                        "Data integrity error: Cannot decrement usage count for coupon '" + coupon.getCode() +
                                "'. Current count is " + coupon.getUsesCount() + "."
                );
            }
            coupon.setUsesCount(coupon.getUsesCount() - 1);
            couponRepository.save(coupon);

            productCouponApplication.setRemovedAt(Instant.now());
            productCouponApplicationRepository.save(productCouponApplication);
            return;
        }

        ProductDirectDiscountApplication productDirectDiscountApplication = productDirectDiscountApplicationRepository.findByProductIdAndRemovedAtIsNull(product.getId());
        if (productDirectDiscountApplication != null) {
            productDirectDiscountApplication.setRemovedAt(Instant.now());
            productDirectDiscountApplicationRepository.save(productDirectDiscountApplication);
            return;
        }

        throw new BusinessRuleException("Product has no active discounts to remove.");
    }

    @Transactional
    public ProductDTO partialUpdateProduct(Long id, JsonPatch patch) {
        Product entity = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id '" + id + "' not found."));
        BigDecimal originalPrice = entity.getPrice();
        String originalNormalizedName = entity.getNormalizedName();

        ProductDTO patchedDto;
        try {
            JsonNode productNode = objectMapper.valueToTree(entity);
            JsonNode patchedNode = patch.apply(productNode);
            patchedDto = objectMapper.treeToValue(patchedNode, ProductDTO.class);
        } catch (JsonProcessingException | JsonPatchException e) {
            throw new BusinessRuleException(e.getMessage());
        }

        entity.setName(patchedDto.name());
        entity.setDescription(patchedDto.description());
        entity.setPrice(patchedDto.price().setScale(2, RoundingMode.HALF_UP));
        entity.setStock(patchedDto.stock());
        validateProductUpdate(originalPrice, originalNormalizedName, entity);

        entity.setUpdatedAt(Instant.now());
        Product updatedProduct = productRepository.saveAndFlush(entity);
        return new ProductDTO(updatedProduct);
    }

    private void validateProductUpdate(BigDecimal originalPrice, String originalNormalizedName,Product patchedProduct) {
        validateRequiredFields(patchedProduct);
        validateBusinessRules(patchedProduct);
        processUpdateSideEffects(originalPrice, originalNormalizedName, patchedProduct);
    }

    private void validateRequiredFields(Product entity){
        if (entity.getName() == null) {
            throw new BusinessRuleException("Name is required");
        }
        if (entity.getStock() == null) {
            throw new BusinessRuleException("Stock is required");
        }
        if (entity.getPrice() == null) {
            throw new BusinessRuleException("Price is required");
        }
    }

    private void validateBusinessRules(Product patched) {
        if (patched.getName().length() > 100 || patched.getName().length() < 3) {
            throw new BusinessRuleException("Name must be between 3 and 100 characters");
        }

        if (!patched.getName().matches("^[\\p{L}0-9\\s\\-_,.]+$")){
            throw new BusinessRuleException("Name can only contain letters, numbers, spaces, and special characters (-, _, , .)");
        }

        if (patched.getDescription() != null && patched.getDescription().length() > 300) {
            throw new BusinessRuleException("Description must be less than or equal to 300 characters");
        }

        if (patched.getStock() < 0 || patched.getStock() > 999999) {
            throw new BusinessRuleException("Stock must be between 0 and 999999");
        }

        if (patched.getPrice().compareTo(BigDecimal.valueOf(0.01)) < 0 || patched.getPrice().compareTo(BigDecimal.valueOf(1000000.00)) > 0) {
            throw new BusinessRuleException("Price must be between 0.01 and 1000000.00");
        }
    }

    private void processUpdateSideEffects (BigDecimal originalPrice, String originalNormalizedName,Product patched) {
        String normalizedPatchedName = Utils.normalizeName(patched.getName());
        if(!normalizedPatchedName.equals(originalNormalizedName) && productRepository.existsByNormalizedName(normalizedPatchedName)) {
                throw new ResourceConflictException("Product with name '" + patched.getName() + "' already exists.");
            }

        patched.setNormalizedName(normalizedPatchedName);

        if (!originalPrice.equals(patched.getPrice().setScale(2, RoundingMode.HALF_UP))){
            hasDiscountCheck(patched);
        }
    }

    private BigDecimal calculateFinalPrice(Product product, Coupon coupon) {
        if (coupon.getType() == CouponEnum.PERCENT) {
            BigDecimal discountFactor = BigDecimal.ONE.subtract(coupon.getValue().divide(BigDecimal.valueOf(100)));
            return product.getPrice().multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
        } else {
            return product.getPrice().subtract(coupon.getValue());
        }
    }

    private void validateProductAvailableDiscount(Product product) {
        if (product.getDeletedAt() != null) {
            throw new BusinessRuleException("Product is deleted and cannot have a coupon applied.");
        }

        if (productCouponApplicationRepository.findByProductIdAndRemovedAtIsNull(product.getId()) != null) {
            throw new ResourceConflictException("Coupon is already applied to this product.");
        }

        if (productDirectDiscountApplicationRepository.findByProductIdAndRemovedAtIsNull(product.getId()) != null){
            throw new ResourceConflictException("Direct discount is already applied to this product.");
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

    @Transactional(readOnly = true)
    public Page<ProductDiscountDTO> getAllProducts(
            Pageable pageable, String search, BigDecimal minPrice, BigDecimal maxPrice,
            Boolean hasDiscount, Boolean includeDeleted, Boolean onlyOutOfStock, Boolean withCouponApplied) {

        List<Specification<Product>> specs = new ArrayList<>();

        if (includeDeleted == null || !includeDeleted) {
            specs.add(ProductSpecification.isActive());
        }

        if (search != null && !search.trim().isEmpty()) {
            specs.add(ProductSpecification.nameOrDescriptionLike(search));
        }
        if (minPrice != null) {
            specs.add(ProductSpecification.priceGreaterThanOrEqual(minPrice));
        }
        if (maxPrice != null) {
            specs.add(ProductSpecification.priceLessThanOrEqual(maxPrice));
        }
        if (onlyOutOfStock != null && onlyOutOfStock) {
            specs.add(ProductSpecification.isOutOfStock());
        }

        Specification<Product> finalSpec = specs.stream()
                .reduce(Specification::and)
                .orElse(null);

        Page<Product> productPage = productRepository.findAll(finalSpec, pageable);

        return convertToPageProductDiscountDTO(productPage, withCouponApplied, hasDiscount, pageable);
    }


    private Page<ProductDiscountDTO> convertToPageProductDiscountDTO(Page<Product> productPage,Boolean withCouponApplied, Boolean hasDiscount, Pageable pageable) {
        List<Long> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .toList();

        if (productIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<ProductCouponApplication> couponApps = productCouponApplicationRepository.findAllByProductIdInAndRemovedAtIsNull(productIds);
        List<ProductDirectDiscountApplication> directApps = productDirectDiscountApplicationRepository.findAllByProductIdInAndRemovedAtIsNull(productIds);

        Map<Long, ProductCouponApplication> couponAppMap = couponApps.stream()
                .collect(Collectors.toMap(app -> app.getProduct().getId(), app -> app));
        Map<Long, ProductDirectDiscountApplication> directAppMap = directApps.stream()
                .collect(Collectors.toMap(app -> app.getProduct().getId(), app -> app));

        Page<ProductDiscountDTO> dtoPage = productPage.map(product -> {
            ProductCouponApplication couponApp = couponAppMap.get(product.getId());
            ProductDirectDiscountApplication directApp = directAppMap.get(product.getId());

            if (couponApp != null) {
                BigDecimal finalPrice = calculateFinalPrice(product, couponApp.getCoupon());
                DiscountDTO discountDTO = new DiscountDTO(couponApp.getCoupon(), couponApp);
                return new ProductDiscountDTO(product, finalPrice, discountDTO, true);
            } else if (directApp != null) {
                BigDecimal discountFraction = directApp.getDiscountPercentage().divide(new BigDecimal("100"));
                BigDecimal finalPrice = product.getPrice().multiply(BigDecimal.ONE.subtract(discountFraction)).setScale(2, RoundingMode.DOWN);
                DiscountDTO discountDTO = new DiscountDTO(CouponEnum.PERCENT.getTypeValue(), directApp.getDiscountPercentage(), directApp.getAppliedAt());
                return new ProductDiscountDTO(product, finalPrice, discountDTO, false);
            } else {
                return new ProductDiscountDTO(product, product.getPrice(), null, false);
            }
        });

        List<ProductDiscountDTO> filteredList = dtoPage.getContent();
        if (hasDiscount != null) {
            filteredList = filteredList.stream()
                    .filter(p -> hasDiscount.equals(p.discount() != null))
                    .toList();
        }
        if (withCouponApplied != null && withCouponApplied) {
            filteredList = filteredList.stream()
                    .filter(ProductDiscountDTO::hasCouponApplied)
                    .toList();
        }

        return new PageImpl<>(filteredList, pageable, productPage.getTotalElements());
    }
}
