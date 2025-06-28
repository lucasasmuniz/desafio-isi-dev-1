package br.com.lmuniz.desafio.senai.services;

import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponCodeDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.discounts.DirectPercentageDiscountDTO;
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
import br.com.lmuniz.desafio.senai.services.exceptions.BusinessRuleException;
import br.com.lmuniz.desafio.senai.services.exceptions.InvalidPriceException;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceConflictException;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceNotFoundException;
import br.com.lmuniz.desafio.senai.tests.CouponFactory;
import br.com.lmuniz.desafio.senai.tests.ProductDiscountApplicationsFactory;
import br.com.lmuniz.desafio.senai.tests.ProductFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class ProductServiceTests {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private ProductCouponApplicationRepository productCouponApplicationRepository;

    @Mock
    private ProductDirectDiscountApplicationRepository productDirectDiscountApplicationRepository;

    private String nonExistingNormalizedName;
    private String existingNormalizedName;
    private String couponValidNormalizedCode = "promo";
    private String couponInvalidNormalizedCode = "invalid";
    private Long existingId;
    private Long nonExistingId;
    private Product product;
    private Coupon coupon;
    private ProductDirectDiscountApplication productDirectDiscountApplication;
    private ProductCouponApplication productCouponApplication;

    @BeforeEach
    void setUp() {
        nonExistingNormalizedName = "cafe premium";
        existingNormalizedName = "cafe normal";
        existingId = 1L;
        nonExistingId = 2L;
        product = ProductFactory.createProduct();
        coupon = CouponFactory.createCoupon();
        coupon.setUsesCount(2);
        productDirectDiscountApplication = ProductDiscountApplicationsFactory.createProductDirectDiscountApplication(product);
        productCouponApplication = ProductDiscountApplicationsFactory.createProductCouponApplication(product,coupon);

        when(productRepository.existsByNormalizedName(nonExistingNormalizedName)).thenReturn(false);
        when(productRepository.existsByNormalizedName(existingNormalizedName)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productRepository.findById(existingId)).thenReturn(Optional.of(product));
        when(productRepository.findById(nonExistingId)).thenReturn(Optional.empty());
        when(couponRepository.findByCode(couponValidNormalizedCode)).thenReturn(coupon);
        when(couponRepository.findByCode(couponInvalidNormalizedCode)).thenReturn(null);
        when(productCouponApplicationRepository.save(any(ProductCouponApplication.class))).thenReturn(productCouponApplication);
        when(productDirectDiscountApplicationRepository.save(any(ProductDirectDiscountApplication.class))).thenReturn(productDirectDiscountApplication);
    }

    @Test
    @DisplayName("create product should throw ResourceNotFoundException when normalized name does not exist and valid data")
    void createProductShouldCreateProductWhenNonExistingNormalizedNameAndValidData() {
        ProductDTO dto = new ProductDTO(1L, nonExistingNormalizedName,product.getDescription(), product.getStock(), product.getPrice());
        ProductDTO result = productService.createProduct(dto);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(product.getName(), result.name());
        assertEquals(product.getDescription(), result.description());
        assertEquals(product.getStock(), result.stock());
        assertEquals(product.getPrice(), result.price());
    }

    @Test
    @DisplayName("create product should throw ResourceNotFoundException when normalized name exists")
    void createProduct_ShouldThrowResourceConflictException_WhenExistingNormalizedName() {
        ProductDTO dto = new ProductDTO(1L, existingNormalizedName,product.getDescription(), product.getStock(), product.getPrice());
        assertThrows(ResourceConflictException.class, () -> {
            productService.createProduct(dto);
        });
        verify(productRepository, times(1)).existsByNormalizedName(existingNormalizedName);
    }

    @Test
    @DisplayName("product soft delete shoud do nothing when id exists and has coupon discount")
    void softDelete_ShouldDoNothing_WhenIdExistsAndHasCouponDiscount(){
        when(productCouponApplicationRepository.findByProductIdAndRemovedAtIsNull(existingId)).thenReturn(productCouponApplication);
        when(productDirectDiscountApplicationRepository.findByProductIdAndRemovedAtIsNull(existingId)).thenReturn(null);

        productService.softDeleteProduct(existingId);

        verify(productRepository, times(2)).findById(existingId);
        verify(productRepository, times(1)).save(any(Product.class));
        verify(couponRepository, times(1)).save(any(Coupon.class));
        verify(productCouponApplicationRepository, times(1)).save(any(ProductCouponApplication.class));
        verify(productDirectDiscountApplicationRepository, times(0)).save(any(ProductDirectDiscountApplication.class));
    }

    @Test
    @DisplayName("product soft delete shoud do nothing when id exists and has direct discount")
    void softDelete_ShouldDoNothing_WhenIdExistsAndHasDirectDiscount(){
        when(productCouponApplicationRepository.findByProductIdAndRemovedAtIsNull(existingId)).thenReturn(null);
        when(productDirectDiscountApplicationRepository.findByProductIdAndRemovedAtIsNull(existingId)).thenReturn(productDirectDiscountApplication);

        productService.softDeleteProduct(existingId);

        verify(productRepository, times(2)).findById(existingId);
        verify(productRepository, times(1)).save(any(Product.class));
        verify(couponRepository, times(0)).save(any(Coupon.class));
        verify(productCouponApplicationRepository, times(0)).save(any(ProductCouponApplication.class));
        verify(productDirectDiscountApplicationRepository, times(1)).save(any(ProductDirectDiscountApplication.class));
    }

    @Test
    @DisplayName("product soft delete shoud throw ResourceNotFoundException when id does not exists")
    void softDelete_ShouldThrowResourceNotFoundException_WhenIdDoNotExists(){
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.softDeleteProduct(nonExistingId);
        });

        verify(productRepository, times(1)).findById(nonExistingId);
        verify(productRepository, times(0)).save(any(Product.class));
    }

    @Test
    @DisplayName("product soft delete shoud do nothing when id exists and has no discounts")
    void softDelete_ShouldDoNothing_WhenIdExistsAndHasNoDiscounts(){
        productService.softDeleteProduct(existingId);

        verify(productRepository, times(1)).findById(existingId);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("restore product should return product when id exists and product is soft deleted")
    void restoreProduct_ShouldReturnProduct_WhenIdExistsAndProductIsSoftDeleted(){
        product.setDeletedAt(Instant.now());
        when(productRepository.findById(existingId)).thenReturn(Optional.of(product));

        ProductDTO result = productService.restoreProduct(existingId);

        assertNotNull(result);
        assertEquals(existingId, result.id());
        assertEquals(product.getDescription(), result.description());
    }

    @Test
    @DisplayName("restore product should throw ResourceNotFoundException when id does not exists")
    void restoreProduct_ShouldThrowResourceNotFoundException_WhenIdDoesNotExists(){
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.restoreProduct(nonExistingId);
        });

        verify(productRepository, times(1)).findById(nonExistingId);
    }

    @Test
    @DisplayName("restore product should throw BusinessRuleException when id exists and product not deleted")
    void restoreProduct_ShouldThrowBusinessRuleException_WhenIdExistsAndProductNotDeleted(){
        assertThrows(BusinessRuleException.class, () -> {
            productService.restoreProduct(existingId);
        });

        verify(productRepository, times(1)).findById(existingId);
    }

    @Test
    @DisplayName("find by id should return product when id exists")
    void findById_ShouldReturnProduct_WhenIdExists() {
        ProductDTO result = productService.getProductById(existingId);

        assertNotNull(result);
        assertEquals(existingId, result.id());
        assertEquals(product.getName(), result.name());
        assertEquals(product.getDescription(), result.description());
        assertEquals(product.getStock(), result.stock());
        assertEquals(product.getPrice(), result.price());

        verify(productRepository, times(1)).findById(existingId);
    }

    @Test
    @DisplayName("find by id should throw ResourceNotFoundException when id does not exists")
    void findById_ShouldThrowResourceNotFoundException_WhenIdDoesNotExists() {
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProductById(nonExistingId);
        });

        verify(productRepository, times(1)).findById(nonExistingId);
    }

    @Test
    @DisplayName("apply coupon discount should throw ResourceNotFoundException when coupon normalized name does not exist")
    void applyCouponDiscount_ShouldThrowResouceNotFoundException_WhenCouponNormalizedNameDoesNotExists() {
        CouponCodeDTO couponCodeDTO = new CouponCodeDTO(couponInvalidNormalizedCode);
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.applyCouponDiscount(existingId, couponCodeDTO);
        });

        verify(couponRepository, times(1)).findByCode(couponInvalidNormalizedCode);
        verify(productRepository, never()).findById(existingId);
    }

    @Test
    @DisplayName("apply coupon discount should throw ResourceNotFoundException when product id does not exist")
    void applyCouponDiscount_ShouldThrowResouceNotFoundException_WhenProductIdDoesNotExists() {
        CouponCodeDTO couponCodeDTO = new CouponCodeDTO(couponValidNormalizedCode);
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.applyCouponDiscount(nonExistingId, couponCodeDTO);
        });

        verify(productRepository, times(1)).findById(nonExistingId);
        verify(couponRepository, times(1)).findByCode(couponValidNormalizedCode);
        verify(couponRepository, never()).save(coupon);
        verify(productCouponApplicationRepository, never()).save(productCouponApplication);
    }

    @Test
    @DisplayName("apply coupon discount should throw InvalidPriceException when final price is invalid")
    void applyCouponDiscount_ShouldThrowInvalidPriceException_WhenFinalPriceInvalid() {
        CouponCodeDTO couponCodeDTO = new CouponCodeDTO(couponValidNormalizedCode);
        coupon.setType(CouponEnum.FIXED);
        coupon.setValue(BigDecimal.valueOf(10000));
        when(couponRepository.findByCode(couponValidNormalizedCode)).thenReturn(coupon);

        assertThrows(InvalidPriceException.class, () -> {
            productService.applyCouponDiscount(existingId, couponCodeDTO);
        });

        verify(productRepository, times(1)).findById(existingId);
        verify(couponRepository, times(1)).findByCode(couponValidNormalizedCode);
        verify(couponRepository, never()).save(coupon);
        verify(productCouponApplicationRepository, never()).save(productCouponApplication);
    }

    @Test
    @DisplayName("apply coupon discount should throw ResourceConflictException when coupon is one shot and has already been taken")
    void applyCouponDiscount_ShouldResourceConflictException_WhenCouponOneShotAndAlreadyTaken() {
        CouponCodeDTO couponCodeDTO = new CouponCodeDTO(couponValidNormalizedCode);
        coupon.setOneShot(true);
        coupon.setUsesCount(1);
        when(couponRepository.findByCode(couponValidNormalizedCode)).thenReturn(coupon);

        assertThrows(ResourceConflictException.class, () -> {
            productService.applyCouponDiscount(existingId, couponCodeDTO);
        });

        verify(productRepository, never()).findById(existingId);
        verify(couponRepository, times(1)).findByCode(couponValidNormalizedCode);
    }

    @ParameterizedTest(name = "applyCouponDiscount should throw BusinessRuleException {0}")
    @MethodSource("provideInvalidCouponScenarios")
    @DisplayName("applyCouponDiscount should throw exception for invalid coupon states")
    void applyCouponDiscount_shouldThrowBusinessRuleException_forInvalidCouponStates(String scenarioName, Consumer<Coupon> couponSetup) {
        Coupon testCoupon = this.coupon;

        couponSetup.accept(testCoupon);

        CouponCodeDTO couponCodeDTO = new CouponCodeDTO(testCoupon.getCode());
        when(couponRepository.findByCode(testCoupon.getCode())).thenReturn(testCoupon);

        assertThrows(BusinessRuleException.class, () -> {
            productService.applyCouponDiscount(existingId, couponCodeDTO);
        });

        verify(productRepository, never()).findById(anyLong());
        verify(couponRepository, times(1)).findByCode(testCoupon.getCode());
    }

    private static Stream<Arguments> provideInvalidCouponScenarios() {
        return Stream.of(
                Arguments.of(
                        "when coupon is soft-deleted",
                        (Consumer<Coupon>) coupon -> coupon.setDeletedAt(Instant.now())
                ),
                Arguments.of(
                        "when coupon usage limit is reached",
                        (Consumer<Coupon>) coupon -> {
                            coupon.setMaxUses(10);
                            coupon.setUsesCount(10);
                        }
                ),
                Arguments.of(
                        "when coupon is expired",
                        (Consumer<Coupon>) coupon -> {
                            coupon.setValidUntil(Instant.now().minus(5, ChronoUnit.DAYS));
                        }
                ),
                Arguments.of(
                        "when coupon is not yet valid",
                        (Consumer<Coupon>) coupon -> {
                            coupon.setValidFrom(Instant.now().plus(5, ChronoUnit.DAYS));
                        }
                )
        );
    }

    @ParameterizedTest(name = "applyCouponDiscount should apply discount successfully {0}")
    @MethodSource("provideValidCouponScenarios")
    @DisplayName("applyCouponDiscount should return product dto for valid coupon and product states")
    void applyCouponDiscount_ShouldApplyCouponDiscount_WhenProductAndCouponAndProductHasNoDiscount(String scenarioName, Consumer<Coupon> couponSetup) {
        when(productCouponApplicationRepository.findByProductIdAndRemovedAtIsNull(existingId)).thenReturn(null);
        when(productDirectDiscountApplicationRepository.findByProductIdAndRemovedAtIsNull(existingId)).thenReturn(null);
        Coupon testCoupon = this.coupon;

        couponSetup.accept(testCoupon);

        CouponCodeDTO couponCodeDTO = new CouponCodeDTO(coupon.getCode());
        when(couponRepository.findByCode(couponCodeDTO.code())).thenReturn(testCoupon);

        ProductDiscountDTO result = assertDoesNotThrow(() -> {
            return productService.applyCouponDiscount(existingId, couponCodeDTO);
        });
        assertNotNull(result);
        assertNotNull(result.discount());
        assertEquals(existingId, result.id());
        assertEquals(coupon.getValue(), result.discount().value());
    }

    private static Stream<Arguments> provideValidCouponScenarios() {
        return Stream.of(
                Arguments.of(
                        "when coupon has max uses and uses count is less than max uses",
                        (Consumer<Coupon>) coupon -> {
                            coupon.setMaxUses(5);
                            coupon.setUsesCount(0);
                        }
                ),
                Arguments.of(
                        "when one shot coupon is not used",
                        (Consumer<Coupon>) coupon -> {
                            coupon.setOneShot(true);
                            coupon.setUsesCount(0);
                        }
                )
        );
    }

    @Test
    @DisplayName("apply direct discount should throw ResourceConflictException when product id does not exist")
    void applyDirectDiscount_ShouldThrowResourceNotFoundException_WhenProductIdDoesNotExist() {
        DirectPercentageDiscountDTO directPercentageDiscountDTO = new DirectPercentageDiscountDTO(BigDecimal.TEN);
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.applyDirectPercentDiscount(nonExistingId, directPercentageDiscountDTO);
        });

        verify(productRepository, times(1)).findById(nonExistingId);
    }

    @Test
    @DisplayName("apply direct discount should throw InvalidPriceException when final price is invalid")
    void applyDirectDiscount_ShouldThrowInvalidPriceException_WhenFinalPriceInvalid() {
        DirectPercentageDiscountDTO directPercentageDiscountDTO = new DirectPercentageDiscountDTO(BigDecimal.valueOf(70));
        product.setPrice(BigDecimal.valueOf(0.01));
        when(productRepository.findById(existingId)).thenReturn(Optional.of(product));

        assertThrows(InvalidPriceException.class, () -> {
            productService.applyDirectPercentDiscount(existingId, directPercentageDiscountDTO);
        });

        verify(productRepository, times(1)).findById(existingId);
    }

    @Test
    @DisplayName("apply direct discount should throw ResourceConflictException when product is deleted")
    void applyDirectDiscount_ShouldThrowBusinessRuleException_WhenProductDeleted(){
        DirectPercentageDiscountDTO directPercentageDiscountDTO = new DirectPercentageDiscountDTO(BigDecimal.valueOf(70));
        product.setDeletedAt(Instant.now());
        when(productRepository.findById(existingId)).thenReturn(Optional.of(product));

        assertThrows(BusinessRuleException.class, () -> {
            productService.applyDirectPercentDiscount(existingId, directPercentageDiscountDTO);
        });

        verify(productRepository, times(1)).findById(existingId);
    }

    @Test
    @DisplayName("apply direct discount should throw ResourceConflictException when product already has direct discount")
    void applyDirectDiscount_ShouldThrowResourceConflictException_WhenProductAlreadyHasDirectDiscount() {
        DirectPercentageDiscountDTO directPercentageDiscountDTO = new DirectPercentageDiscountDTO(BigDecimal.valueOf(10));
        when(productDirectDiscountApplicationRepository.findByProductIdAndRemovedAtIsNull(existingId)).thenReturn(productDirectDiscountApplication);
        when(productRepository.findById(existingId)).thenReturn(Optional.of(product));

        assertThrows(ResourceConflictException.class, () -> {
            productService.applyDirectPercentDiscount(existingId, directPercentageDiscountDTO);
        });

        verify(productRepository, times(1)).findById(existingId);
        verify(productDirectDiscountApplicationRepository, times(1)).findByProductIdAndRemovedAtIsNull(existingId);
    }

    @Test
    @DisplayName("apply direct discount should throw ResourceConflictException when product already has coupon discount")
    void applyDirectDiscount_ShouldThrowResourceConflictException_WhenProductAlreadyHasCouponDiscount() {
        DirectPercentageDiscountDTO directPercentageDiscountDTO = new DirectPercentageDiscountDTO(BigDecimal.valueOf(10));
        when(productCouponApplicationRepository.findByProductIdAndRemovedAtIsNull(existingId)).thenReturn(productCouponApplication);
        when(productRepository.findById(existingId)).thenReturn(Optional.of(product));

        assertThrows(ResourceConflictException.class, () -> {
            productService.applyDirectPercentDiscount(existingId, directPercentageDiscountDTO);
        });

        verify(productRepository, times(1)).findById(existingId);
        verify(productCouponApplicationRepository, times(1)).findByProductIdAndRemovedAtIsNull(existingId);
    }

    @Test
    @DisplayName("apply direct discount should apply discount successfully when product is valid and has no discounts")
    void applyDirectDiscount_ShouldApplyDirectDiscount_WhenProductValidAndDiscountAreValid() {
        DirectPercentageDiscountDTO directPercentageDiscountDTO = new DirectPercentageDiscountDTO(BigDecimal.valueOf(10));
        when(productDirectDiscountApplicationRepository.findByProductIdAndRemovedAtIsNull(existingId)).thenReturn(null);
        when(productCouponApplicationRepository.findByProductIdAndRemovedAtIsNull(existingId)).thenReturn(null);
        when(productRepository.findById(existingId)).thenReturn(Optional.of(product));

        ProductDiscountDTO result = assertDoesNotThrow(() -> {
            return productService.applyDirectPercentDiscount(existingId, directPercentageDiscountDTO);
        });

        assertNotNull(result);
        assertEquals(existingId, result.id());
        assertEquals(directPercentageDiscountDTO.percentage(), result.discount().value());
    }
}
