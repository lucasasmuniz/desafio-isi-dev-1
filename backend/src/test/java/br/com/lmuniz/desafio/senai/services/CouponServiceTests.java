package br.com.lmuniz.desafio.senai.services;

import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponDetailsDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Coupon;
import br.com.lmuniz.desafio.senai.repositories.CouponRepository;
import br.com.lmuniz.desafio.senai.services.exceptions.BusinessRuleException;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceConflictException;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceNotFoundException;
import br.com.lmuniz.desafio.senai.tests.CouponFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class CouponServiceTests{

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());


    private Long existingId;
    private Long nonExistingId;
    private Coupon coupon;
    private CouponDTO couponDTO;

    @BeforeEach
    void setUp() {
        existingId = 1L;
        nonExistingId = 999L;
        coupon = CouponFactory.createCoupon();
        coupon.setId(existingId);
        coupon.setUsesCount(0);
        coupon.setCreatedAt(Instant.now());

        couponDTO = new CouponDTO(coupon);


        when(couponRepository.findById(existingId)).thenReturn(Optional.of(coupon));
        when(couponRepository.findById(nonExistingId)).thenReturn(Optional.empty());
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(couponRepository.saveAndFlush(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("getCouponById should return CouponDetailsDTO when ID exists")
    void getCouponById_ShouldReturnCouponDetailsDTO_WhenIdExists() {

        CouponDetailsDTO result = couponService.getCouponById(existingId);

        assertNotNull(result);
        assertEquals(existingId, result.id());
        verify(couponRepository).findById(existingId);
    }

    @Test
    @DisplayName("getCouponById should throw ResourceNotFoundException when ID does not exist")
    void getCouponById_ShouldThrowResourceNotFoundException_WhenIdDoesNotExist() {

        assertThrows(ResourceNotFoundException.class, () -> couponService.getCouponById(nonExistingId));
        verify(couponRepository).findById(nonExistingId);
    }

    @Test
    @DisplayName("getAllCoupons should return all coupons when onlyValid is false")
    void getAllCoupons_ShouldCallFindAll_WhenOnlyValidIsFalse() {
        when(couponRepository.findAll()).thenReturn(List.of(coupon));
        List<CouponDTO> result = couponService.getAllCoupons(false);

        assertEquals(1, result.size());
        verify(couponRepository).findAll();
        verify(couponRepository, never()).searchValidCoupons(any());
    }

    @Test
    @DisplayName("getAllCoupons should return only valid coupons when onlyValid is true")
    void getAllCoupons_ShouldCallSearchValidCoupons_WhenOnlyValidIsTrue() {
        when(couponRepository.searchValidCoupons(any(Instant.class))).thenReturn(List.of(coupon));
        List<CouponDTO> result = couponService.getAllCoupons(true);

        assertEquals(1, result.size());
        verify(couponRepository).searchValidCoupons(any(Instant.class));
        verify(couponRepository, never()).findAll();
    }

    @Test
    @DisplayName("createCoupon should return CouponDTO when data is valid and oneShot is true")
    void createCoupon_ShouldReturnDTO_WhenDataIsValidAndOneShotIsTrue() {
        when(couponRepository.findByCodeAndIdNot(anyString(), any())).thenReturn(Optional.empty());
        CouponDTO result = couponService.createCoupon(couponDTO);

        assertAll("Valid OneShot Coupon",
                () -> assertEquals(couponDTO.code(), result.code()),
                () -> assertNull(result.maxUses()),
                () -> assertNotNull(result)
        );
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    @DisplayName("createCoupon should return CouponDTO when data is valid and oneShot is false")
    void createCoupon_ShouldReturnDTO_WhenDataIsValidAndOneShotIsFalse() {
        // Arrange
        CouponDTO notOneShotDTO = new CouponDTO(null, "notoneshot", "fixed", BigDecimal.ONE, false, 100, Instant.now().plus(1, ChronoUnit.DAYS), Instant.now().plus(2, ChronoUnit.DAYS));
        when(couponRepository.findByCodeAndIdNot(anyString(), any())).thenReturn(Optional.empty());
        // Act
        CouponDTO result = couponService.createCoupon(notOneShotDTO);
        // Assert
        assertAll("Valid Multi-Use Coupon",
                () -> assertEquals(notOneShotDTO.code(), result.code()),
                () -> assertFalse(result.oneShot()),
                () -> assertEquals(100, result.maxUses()),
                () -> assertNotNull(result)
        );
        verify(couponRepository).save(any(Coupon.class));
    }

//    @Test
//    @DisplayName("createCoupon should throw ResourceConflictException when code already exists")
//    void createCoupon_ShouldThrowResourceConflictException_WhenCodeExists() {
//        when(couponRepository.findByCodeAndIdNot(couponDTO.code(), -1L)).thenReturn(Optional.of(new Coupon()));
//
//        assertThrows(ResourceConflictException.class, () -> couponService.createCoupon(couponDTO));
//        verify(couponRepository, never()).save(any());
//    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "auth", "null", "undefined"})
    @DisplayName("createCoupon should throw BusinessRuleException for reserved codes")
    void createCoupon_ShouldThrowBusinessRuleException_WhenCodeIsReserved(String reservedCode) {
        CouponDTO reservedCodeDto = new CouponDTO(null, reservedCode, "fixed", BigDecimal.ONE, false, 100, Instant.now().plus(1, ChronoUnit.DAYS), Instant.now().plus(2, ChronoUnit.DAYS));

        assertThrows(BusinessRuleException.class, () -> couponService.createCoupon(reservedCodeDto));
    }

//    @Test
//    @DisplayName("createCoupon should throw BusinessRuleException for fixed type coupon with zero value")
//    void createCoupon_ShouldThrowBusinessRuleException_WhenFixedTypeAndValueIsZero() {
//        CouponDTO zeroValueDto = new CouponDTO(null, "zerovalue", "fixed", BigDecimal.ZERO, false, 100, Instant.now().plus(1, ChronoUnit.DAYS), Instant.now().plus(2, ChronoUnit.DAYS));
//
//        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> couponService.createCoupon(zeroValueDto));
//        assertTrue(e.getMessage().contains("must be positive"));
//    }

//    @Test
//    @DisplayName("createCoupon should throw BusinessRuleException for valid dates range greater then 5 years")
//    void createCoupon_ShouldThrowBusinessRuleException_WhenValidDatesRangeGreaterThen5Years() {
//        CouponDTO zeroValueDto = new CouponDTO(null, "validdates", "fixed", BigDecimal.ONE, false, 100, Instant.now().plus(1, ChronoUnit.DAYS), Instant.now().plus(3000, ChronoUnit.DAYS));
//
//        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> couponService.createCoupon(zeroValueDto));
//        assertTrue(e.getMessage().contains("5 years"));
//    }

    @Test
    @DisplayName("deleteCoupon should set deletedAt when ID exists")
    void deleteCoupon_ShouldSetDeletedAt_WhenIdExists() {
        couponService.deleteCoupon(existingId);

        verify(couponRepository).findById(existingId);
        verify(couponRepository).save(any(Coupon.class));
        assertNotNull(coupon.getDeletedAt());
    }

    @Test
    @DisplayName("deleteCoupon should throw ResourceNotFoundException when ID does not exist")
    void deleteCoupon_ShouldThrowResourceNotFoundException_WhenIdDoesNotExist() {
        assertThrows(ResourceNotFoundException.class, () -> couponService.deleteCoupon(nonExistingId));
        verify(couponRepository).findById(nonExistingId);
        verify(couponRepository, never()).save(any());
    }

//    @Test
//    @DisplayName("partialUpdateCoupon should update coupon when patch is valid")
//    void partialUpdateCoupon_ShouldUpdate_WhenPatchIsValid() throws Exception {
//        String patchJson = """
//        [
//            { "op": "replace", "path": "/value", "value": 25 }
//        ]
//        """;
//        JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchJson));
//
//        CouponDetailsDTO result = couponService.partialUpdateCoupon(existingId, patch);
//
//        assertEquals(new BigDecimal("25"), result.value());
//        verify(couponRepository).saveAndFlush(any(Coupon.class));
//    }
//
//    @Test
//    @DisplayName("partialUpdateCoupon should throw BusinessRuleException when changes maxUses to less than usesCount")
//    void partialUpdateCoupon_ShouldThrowBusinessRuleException_WhenMaxUsesLessThenUsesCount() throws Exception {
//        coupon.setUsesCount(10);
//        when(couponRepository.findById(existingId)).thenReturn(Optional.of(coupon));
//        String patchJson = """
//        [
//            { "op": "replace", "path": "/maxUses", "value": 2 },
//            { "op": "replace", "path": "/oneShot", "value": false }
//        ]
//        """;
//        JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchJson));
//
//        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> {
//            couponService.partialUpdateCoupon(existingId, patch);
//        });
//        assertTrue(e.getMessage().contains("lower than the current usage count"));
//        verify(couponRepository, never()).saveAndFlush(any());
//    }

    @Test
    @DisplayName("partialUpdateCoupon should throw ResourceNotFoundException when non existing id")
    void partialUpdateCoupon_ShouldThrowResourceNotFoundException_WhenPatchFails() throws Exception {
        String patchJson = """
        [
            { "op": "replace", "path": "/value", "value": 25 }
        ]
        """;
        JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchJson));

        ResourceNotFoundException e = assertThrows(ResourceNotFoundException.class, () -> {
            couponService.partialUpdateCoupon(nonExistingId, patch);
        });
        assertTrue(e.getMessage().contains("not found"));
        verify(couponRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("partialUpdateCoupon should throw BusinessRuleException when patch operation fails")
    void partialUpdateCoupon_ShouldThrowBusinessRuleException_WhenPatchFails() throws Exception {
        JsonPatch patch = mock(JsonPatch.class);
        when(patch.apply(any())).thenThrow(new JsonPatchException("Invalid patch"));

        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> {
            couponService.partialUpdateCoupon(existingId, patch);
        });
        assertTrue(e.getMessage().contains("Invalid patch"));
        verify(couponRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideInvalidPatchOperations")
    @DisplayName("partialUpdateCoupon should throw BusinessRuleException for invalid operations")
    void partialUpdateCoupon_shouldThrowBusinessRuleException_forInvalidPatchOperations(String testName, String patchJson) throws Exception {
        JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchJson));

        assertThrows(BusinessRuleException.class, () -> {
            couponService.partialUpdateCoupon(existingId, patch);
        });
        verify(couponRepository, never()).saveAndFlush(any());
    }

    private static Stream<Arguments> provideInvalidPatchOperations() {
        return Stream.of(
                Arguments.of("when trying to change immutable code", """
                    [ { "op": "replace", "path": "/code", "value": "newcode" } ]
                """),
                Arguments.of("when trying to change immutable usesCount", """
                    [ { "op": "replace", "path": "/usesCount", "value": 99 } ]
                """),
                Arguments.of("when trying to remove required field validUtil", """
                    [ { "op": "remove", "path": "/validUntil" } ]
                """),
                Arguments.of("when trying to change createdAt field", """
                    [ { "op": "replace", "path": "/createdAt", "value": "2025-01-01T00:00:00Z"} ]
                """),
                Arguments.of("when trying to change updatedAt field", """
                    [ { "op": "replace", "path": "/updatedAt", "value": "2025-01-01T00:00:00Z"} ]
                """),
                Arguments.of("when trying to change validUntil field to past or present date", """
                    [ { "op": "replace", "path": "/validUntil","value": "2024-01-01T00:00:00Z"} ]
                """),
                Arguments.of("when trying to change invert validDates field values", """
                    [
                     { "op": "replace", "path": "/validFrom","value": "2026-01-01T00:00:00Z"},
                     { "op": "replace", "path": "/validUntil","value": "2025-12-01T00:00:00Z"}
                  ]
                """),
                Arguments.of("when trying to change deletedAt field", """
                    [ { "op": "replace", "path": "/deletedAt","value": "2025-01-01T00:00:00Z"} ]
                """),
                Arguments.of("when trying to change one shot to true and max uses is different then null", """
                    [ { "op": "replace", "path": "/oneShot","value": true},
                     { "op": "replace", "path": "/maxUses","value": 10} ]
                """)
        );
    }
}