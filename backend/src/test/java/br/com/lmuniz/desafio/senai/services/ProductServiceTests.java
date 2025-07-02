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
import br.com.lmuniz.desafio.senai.repositories.specifications.ProductSpecification;
import br.com.lmuniz.desafio.senai.services.exceptions.*;
import br.com.lmuniz.desafio.senai.tests.CouponFactory;
import br.com.lmuniz.desafio.senai.tests.ProductDiscountApplicationsFactory;
import br.com.lmuniz.desafio.senai.tests.ProductFactory;
import br.com.lmuniz.desafio.senai.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.jsonpatch.JsonPatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private ProductDirectDiscountApplicationRepository productDirectDiscountApplicationRepository;

    private String nonExistingNormalizedName;
    private String existingNormalizedName;
    private String couponValidNormalizedCode;
    private String couponInvalidNormalizedCode;
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
        couponValidNormalizedCode = "promo";
        couponInvalidNormalizedCode = "invalid";
        existingId = 1L;
        nonExistingId = 2L;
        product = ProductFactory.createProduct();
        product.setNormalizedName(Utils.normalizeName(product.getName()));
        coupon = CouponFactory.createCoupon();
        coupon.setUsesCount(2);
        productDirectDiscountApplication = ProductDiscountApplicationsFactory.createProductDirectDiscountApplication(product);
        productCouponApplication = ProductDiscountApplicationsFactory.createProductCouponApplication(product, coupon);

        when(productRepository.existsByNormalizedName(nonExistingNormalizedName)).thenReturn(false);
        when(productRepository.existsByNormalizedName(existingNormalizedName)).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findById(existingId)).thenReturn(Optional.of(product));
        when(productRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        when(couponRepository.findByCode(couponValidNormalizedCode)).thenReturn(coupon);
        when(couponRepository.findByCode(couponInvalidNormalizedCode)).thenReturn(null);

        when(productCouponApplicationRepository.save(any(ProductCouponApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productDirectDiscountApplicationRepository.save(any(ProductDirectDiscountApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("create product should throw ResourceNotFoundException when normalized name does not exist and valid data")
    void createProductShouldCreateProductWhenNonExistingNormalizedNameAndValidData() {
        product.setName(nonExistingNormalizedName);
        ProductDTO dto = new ProductDTO(product);
        ProductDTO result = productService.createProduct(dto);

        assertNotNull(result);
        assertEquals(product.getName(), result.name());
        assertEquals(product.getDescription(), result.description());
        assertEquals(product.getStock(), result.stock());
        assertEquals(product.getPrice().setScale(2, RoundingMode.HALF_UP), result.price().setScale(2, RoundingMode.HALF_UP));
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
        DirectPercentageDiscountDTO directPercentageDiscountDTO = new DirectPercentageDiscountDTO(BigDecimal.valueOf(10));
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

    @Test
    @DisplayName("remove discount should throw ResourceNotFoundException when product id does not exist")
    void removeDiscount_ShouldThrowResourceNotFoundException_WhenProductIdDoesNotExist() {
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.removeDiscount(nonExistingId);
        });

        verify(productRepository, times(1)).findById(nonExistingId);
    }

    @Test
    @DisplayName("remove discount should throw DatabaseException when coupon applied and uses count is zero")
    void removeDiscount_ShouldThrowDatabaseException_WhenCouponAppliedAndUsesCountIsZero() {
        coupon.setUsesCount(0);
        productCouponApplication.setCoupon(coupon);
        when(productCouponApplicationRepository.findByProductIdAndRemovedAtIsNull(existingId)).thenReturn(productCouponApplication);

        assertThrows(DatabaseException.class, () -> {
            productService.removeDiscount(existingId);
        });

        verify(productRepository, times(1)).findById(existingId);
        verify(productCouponApplicationRepository, times(1)).findByProductIdAndRemovedAtIsNull(existingId);
    }

    @Test
    @DisplayName("remove discount should throw BusinessRuleException when product has no discount applied")
    void removeDiscount_ShouldThrowBusinessRuleException_WhenNoDiscountsApplied() {
        when(productCouponApplicationRepository.findByProductIdAndRemovedAtIsNull(existingId)).thenReturn(null);
        when(productDirectDiscountApplicationRepository.findByProductIdAndRemovedAtIsNull(existingId)).thenReturn(null);

        assertThrows(BusinessRuleException.class, () -> {
            productService.removeDiscount(existingId);
        });

        verify(productRepository, times(1)).findById(existingId);
        verify(productCouponApplicationRepository, times(1)).findByProductIdAndRemovedAtIsNull(existingId);
        verify(productDirectDiscountApplicationRepository, times(1)).findByProductIdAndRemovedAtIsNull(existingId);
    }

    @Test
    @DisplayName("partial update should throw ResourceNotFoundException when product id does not exist")
    void partialUpdate_shouldThrowResourceNotFoundException_whenProductIdDoesNotExist() {
        JsonPatch patch = new JsonPatch(List.of());

        assertThrows(ResourceNotFoundException.class, () -> productService.partialUpdateProduct(nonExistingId, patch));

        verify(productRepository, times(1)).findById(nonExistingId);
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @Test
    @DisplayName("partial update should throw BusinessRuleException when patch is invalid")
    void partialUpdate_shouldThrowBusinessRuleException_whenInvalidPatch() throws Exception {
        String invalidPatchJson = """
                [
                    { "op": "remove", "path": "/invalid" }
                ]
                """;

        JsonPatch invalidPatch = JsonPatch.fromJson(objectMapper.readTree(invalidPatchJson));

        assertThrows(BusinessRuleException.class, () -> {
            productService.partialUpdateProduct(existingId, invalidPatch);
        });
        verify(productRepository, times(1)).findById(existingId);
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @ParameterizedTest(name = "should fail with multiple errors {0}")
    @MethodSource("providePatchesWithMultipleErrors")
    @DisplayName("partial update  should collect all errors and throw BusinessRuleException for invalid business rules")
    void partialUpdate_shouldThrowBusinessRuleException_whenBusinessRuleInvalidPatch(
            String scenarioName, String patchJson, List<String> expectedErrorKeys) throws Exception {

        JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchJson));

        BusinessRuleException exceptionResult = assertThrows(BusinessRuleException.class, () -> {
            productService.partialUpdateProduct(existingId, patch);
        });

        Map<String, String> errors = exceptionResult.getErrors();

        assertNotNull(errors, "Error map should not be null");
        assertEquals(expectedErrorKeys.size(), errors.size(), "Should find the exact number of errors");
        for (String expectedKey : expectedErrorKeys) {
            assertTrue(errors.containsKey(expectedKey), "Expected error map to contain key: " + expectedKey);
        }

        verify(productRepository, times(1)).findById(existingId);
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    private static Stream<Arguments> providePatchesWithMultipleErrors() {
        String longDescription = "*".repeat(301);
        String longName = "*".repeat(101);

        return Stream.of(
                Arguments.of(
                        "when remove required fields",
                        """
                               [
                                  { "op": "remove", "path": "/name" },
                                  { "op": "remove", "path": "/price" },
                                  { "op": "remove", "path": "/stock" }
                                ]
                               """,
                        List.of("name", "price","stock")
                ),
                Arguments.of(
                        "when name and description are too long, price and stock are negative",
                        """
                               [
                                  { "op": "replace", "path": "/name", "value": "%s" },
                                  { "op": "replace", "path": "/price", "value": -10.00 },
                                  { "op": "replace", "path": "/stock", "value": -10.00 },
                                  { "op": "replace", "path": "/description", "value": "%s" }
                                ]
                               """.formatted(longName,longDescription),
                        List.of("name", "price","stock", "description")
                ),
                Arguments.of(
                        "when name is blank, price and stock are too high",
                        """
                               [
                                  { "op": "replace", "path": "/name", "value": "" },
                                  { "op": "replace", "path": "/price", "value": 1000001.00 },
                                  { "op": "replace", "path": "/stock", "value": 1000000 }
                                ]
                               """,
                        List.of("name", "price","stock")
                ),
                Arguments.of(
                        "when name is too short",
                        """
                               [
                                  { "op": "replace", "path": "/name", "value": "a" }
                                ]
                               """,
                        List.of("name")
                ));
    }

    @Test
    @DisplayName("partial update should throw ResourceConflictException when normalized name already exists")
    void partialUpdate_shouldThrowResourceConflicException_whenNormalizedNameExists() throws Exception {
        String patchJson = """
                [
                    { "op": "replace", "path": "/name", "value": "%s" }
                ]
                """.formatted(existingNormalizedName);

        JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchJson));

        assertThrows(ResourceConflictException.class, () -> {
            productService.partialUpdateProduct(existingId, patch);
        });

        verify(productRepository, times(1)).findById(existingId);
        verify(productRepository, never()).saveAndFlush(any(Product.class));
    }

    @ParameterizedTest(name = "should update product successfully {0}")
    @MethodSource("provideValidPatchScenarios")
    @DisplayName("partialUpdateProduct should succeed for various valid patch operations")
    void partialUpdate_shouldUpdateProductSuccessfully_forValidScenarios(
            String scenarioName, String patchJson, Consumer<ProductDTO> assertionLogic) throws Exception {

        JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchJson));

        ProductDTO updatedProduct = productService.partialUpdateProduct(existingId, patch);

        assertNotNull(updatedProduct);

        assertionLogic.accept(updatedProduct);

        verify(productRepository, times(1)).findById(existingId);
        verify(productRepository, times(1)).saveAndFlush(any(Product.class));
    }

    private static Stream<Arguments> provideValidPatchScenarios() {
        return Stream.of(
                Arguments.of(
                        "when removing description and updating stock and price",
                        """
                        [
                            { "op": "remove", "path": "/description" },
                            { "op": "replace", "path": "/stock", "value": 200 },
                            { "op": "replace", "path": "/price", "value": 15.90 }
                        ]
                        """,
                        (Consumer<ProductDTO>) dto -> {
                            assertAll(
                                    () -> assertNull(dto.description()),
                                    () -> assertEquals(200, dto.stock()),
                                    () -> assertEquals(new BigDecimal("15.90"), dto.price())
                            );
                        }
                ),
                Arguments.of(
                        "when only updating stock",
                        """
                        [
                            { "op": "replace", "path": "/stock", "value": 50 }
                        ]
                        """,
                        (Consumer<ProductDTO>) dto -> assertEquals(50, dto.stock())
                )
        );
    }

    @Test
    @DisplayName("findAllProducts should return page of products filtered when given params")
    void findAllProducts_ShouldReturnPageOfProductsFiltered_WhenGivenParams() {
        Pageable pageable = PageRequest.of(0, 10);
        String search = "teste";
        BigDecimal minPrice = BigDecimal.ONE;
        BigDecimal maxPrice = BigDecimal.valueOf(100);
        Boolean hasDiscount = true;
        Boolean includeDeleted = true;
        Boolean onlyOutOfStock = false;
        Boolean withCouponApplied = true;

        Product productWithDirectDiscount = ProductFactory.createProduct(2L, "Test Product 2", "Another test product", BigDecimal.valueOf(50), 0);
        Product productWithNoDiscount = ProductFactory.createProduct(3L, "Test Product 3", "Another test product no discount", BigDecimal.valueOf(40), 12);
        productDirectDiscountApplication.setProduct(productWithDirectDiscount);

        when(productCouponApplicationRepository.findAllByProductIdInAndRemovedAtIsNull(any())).thenReturn(List.of(productCouponApplication));
        when(productDirectDiscountApplicationRepository.findAllByProductIdInAndRemovedAtIsNull(any())).thenReturn(List.of(productDirectDiscountApplication));

        Page<Product> productPage = new PageImpl<>(List.of(product, productWithDirectDiscount, productWithNoDiscount));
        when(productRepository.findAll((Specification<Product>) any(), (Pageable) any())).thenReturn(productPage);

        Page<ProductDiscountDTO> result = productService.getAllProducts(
                pageable, search, minPrice, maxPrice, hasDiscount, includeDeleted, onlyOutOfStock, withCouponApplied);

        assertNotNull(result);
        assertEquals(productPage.getTotalElements() - 2, result.getTotalElements());
        assertEquals(productPage.getTotalPages(), result.getTotalPages());
        assertEquals(product.getName(), result.getContent().getFirst().name());

        verify(productRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }


    @ParameterizedTest(name = "getAllProducts should return page {0}")
    @MethodSource("provideGetAllProductsScenarios")
    @DisplayName("getAllProducts should return correctly filtered and paged products")
    void getAllProducts_shouldReturnCorrectlyFilteredAndPagedProducts(
            String scenarioName,
            Page<Product> mockedProductPage,
            Pageable pageable,
            String search,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean hasDiscount,
            Boolean includeDeleted,
            Boolean onlyOutOfStock,
            Boolean withCouponApplied,
            Consumer<Page<ProductDiscountDTO>> assertionLogic
    ) {

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockedProductPage);

        when(productCouponApplicationRepository.findAllByProductIdInAndRemovedAtIsNull(any())).thenReturn(List.of());
        when(productDirectDiscountApplicationRepository.findAllByProductIdInAndRemovedAtIsNull(any())).thenReturn(List.of());

        Page<ProductDiscountDTO> result = productService.getAllProducts(
                pageable, search, minPrice, maxPrice, hasDiscount, includeDeleted, onlyOutOfStock, withCouponApplied);

        assertNotNull(result);

        assertionLogic.accept(result);
        verify(productRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    private static Stream<Arguments> provideGetAllProductsScenarios() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product1 = ProductFactory.createProduct(1L, "Product with coupon", "Desc", BigDecimal.valueOf(100), 10);
        Product product2 = ProductFactory.createProduct(2L, "Product with direct discount", "Desc", BigDecimal.valueOf(150), 0);
        Product product3 = ProductFactory.createProduct(3L, "Product with no discount", "Desc", BigDecimal.valueOf(50), 20);

        return Stream.of(
                Arguments.of(
                        "when just 'out of stock' and expecting empty result",
                        new PageImpl<Product>(List.of()),
                        pageable, "", null, null, false, false, true, null,
                        (Consumer<Page<ProductDiscountDTO>>) page -> {
                            assertTrue(page.isEmpty());
                        }
                ),
                Arguments.of(
                        "when price filter is applied and no coupon filter",
                        new PageImpl<>(List.of(product1, product2, product3)),
                        pageable, null, BigDecimal.ONE, new BigDecimal("200"), null, null, null, false,
                        (Consumer<Page<ProductDiscountDTO>>) page -> {
                            assertEquals(3, page.getTotalElements());
                            assertTrue(page.getContent().stream().anyMatch(p -> p.name().equals("Product with no discount")));
                        }
                ),
                Arguments.of(
                        "when text search and hasDiscount are applied",
                        new PageImpl<>(List.of(product1, product2)),
                        pageable, "product", null, null, true, false, false, null,
                        (Consumer<Page<ProductDiscountDTO>>) page -> {
                            assertEquals(2, page.getTotalElements());
                            assertTrue(page.getContent().stream().allMatch(p -> p.discount() != null));
                        }
                )
        );
    }
}
