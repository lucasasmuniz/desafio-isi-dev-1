package br.com.lmuniz.desafio.senai.services;

import br.com.lmuniz.desafio.senai.domains.dtos.products.ProductDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Coupon;
import br.com.lmuniz.desafio.senai.domains.entities.Product;
import br.com.lmuniz.desafio.senai.domains.entities.ProductCouponApplication;
import br.com.lmuniz.desafio.senai.domains.entities.ProductDirectDiscountApplication;
import br.com.lmuniz.desafio.senai.repositories.CouponRepository;
import br.com.lmuniz.desafio.senai.repositories.ProductCouponApplicationRepository;
import br.com.lmuniz.desafio.senai.repositories.ProductDirectDiscountApplicationRepository;
import br.com.lmuniz.desafio.senai.repositories.ProductRepository;
import br.com.lmuniz.desafio.senai.services.exceptions.BusinessRuleException;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceConflictException;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceNotFoundException;
import br.com.lmuniz.desafio.senai.tests.CouponFactory;
import br.com.lmuniz.desafio.senai.tests.ProductDiscountApplicationsFactory;
import br.com.lmuniz.desafio.senai.tests.ProductFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.Optional;

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
        when(productRepository.save(any())).thenReturn(product);
        when(productRepository.findById(existingId)).thenReturn(Optional.of(product));
        when(productRepository.findById(nonExistingId)).thenReturn(Optional.empty());
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

}
