package br.com.lmuniz.desafio.senai.services;

import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponDetailsDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Coupon;
import br.com.lmuniz.desafio.senai.repositories.CouponRepository;
import br.com.lmuniz.desafio.senai.repositories.ProductCouponApplicationRepository;
import br.com.lmuniz.desafio.senai.services.exceptions.BusinessRuleException;
import br.com.lmuniz.desafio.senai.services.exceptions.DatabaseException;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceConflictException;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceNotFoundException;
import br.com.lmuniz.desafio.senai.tests.CouponFactory;

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
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class CouponServiceTests {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private ProductCouponApplicationRepository productCouponApplicationRepository;

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
        CouponDTO oneShotDTO = new CouponDTO(null, "oneshot", "fixed", BigDecimal.ONE, true, null, Instant.now().plus(1, ChronoUnit.DAYS), Instant.now().plus(2, ChronoUnit.DAYS));
        when(couponRepository.findByCodeAndIdNot(anyString(), any())).thenReturn(Optional.empty());

        CouponDTO result = couponService.createCoupon(oneShotDTO);

        assertAll("Valid Multi-Use Coupon",
                () -> assertEquals(oneShotDTO.code(), result.code()),
                () -> assertTrue(result.oneShot()),
                () -> assertNotNull(result)
        );
        verify(couponRepository).save(any(Coupon.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "auth", "null", "undefined"})
    @DisplayName("createCoupon should throw BusinessRuleException for reserved codes")
    void createCoupon_ShouldThrowBusinessRuleException_WhenCodeIsReserved(String reservedCode) {
        CouponDTO reservedCodeDto = new CouponDTO(null, reservedCode, "fixed", BigDecimal.ONE, false, 100, Instant.now().plus(1, ChronoUnit.DAYS), Instant.now().plus(2, ChronoUnit.DAYS));

        assertThrows(BusinessRuleException.class, () -> couponService.createCoupon(reservedCodeDto));
    }

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

    @Test
    @DisplayName("partialUpdateCoupon should throw BusinessRuleException when patch is invalid")
    void partialUpdateCoupon_ShouldThrowBusinessRuleException_WhenPatchIsInvalid() throws Exception {
        String invalidPatchJson = """
                [
                    { "op": "remove", "path": "/invalid" }
                ]
                """;

        JsonPatch invalidPatch = JsonPatch.fromJson(objectMapper.readTree(invalidPatchJson));

        assertThrows(BusinessRuleException.class, () -> {
            couponService.partialUpdateCoupon(existingId, invalidPatch);
        });
        verify(couponRepository, times(1)).findById(existingId);
    }

    @Test
    @DisplayName("partial update coupon should throw ResourceConflictException when id does not exists")
    void partialUpdateCoupon_ShouldThrowResourceNotFoundException_WhenIdDoesNotExist() throws Exception {
        String patchJson = """
                [
                    { "op": "replace", "path": "/code", "value": "newCode" }
                ]
                """;

        JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchJson));

        assertThrows(ResourceNotFoundException.class, () -> {
            couponService.partialUpdateCoupon(nonExistingId, patch);
        });
        verify(couponRepository, times(1)).findById(nonExistingId);
    }


    @Test
    @DisplayName("partialUpdateCoupon should throw ResourceConflictException when normalized code already exists")
    void partialUpdateCoupon_ShouldThrowResourceConflictException_WhenNormalizedCodeAlreadyExists() throws Exception {
        String patchJson = """
                [{ "op": "replace", "path": "/code", "value": "newCode" }]
                """;

        JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchJson));

        when(couponRepository.findByCodeAndIdNot("newCode", existingId)).thenReturn(Optional.of(coupon));

        assertThrows(ResourceConflictException.class, () -> {
            couponService.partialUpdateCoupon(existingId, patch);
        });

        verify(couponRepository, times(1)).findById(existingId);
        verify(couponRepository, times(1)).findByCodeAndIdNot("newCode", existingId);
    }

    @Test
    @DisplayName("partialUpdateCoupon should throw DatabaseException when coupon uses count would be negative")
    void partialUpdateCoupon_ShouldThrowDatabaseException_WhenCouponNegativeUsesCount() throws Exception{
        String patchJson = """
                [
                    { "op": "replace", "path": "/value", "value": 8 }
                ]
                """;

        JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchJson));
        when(productCouponApplicationRepository.removeActiveApplicationsByCouponId(any(Long.class), any(Instant.class)))
                .thenReturn(50);

        assertThrows(DatabaseException.class, () -> {
            couponService.partialUpdateCoupon(existingId, patch);
        });

        verify(couponRepository, times(1)).findById(existingId);
        verify(couponRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest(name = "should update coupon {0}")
    @MethodSource("provideValidPatches")
    @DisplayName("partialUpdateCoupon should update coupon when patch is valid")
    void partialUpdateCoupon_shouldUpdateCoupon_whenPatchIsValid(
            String scenarioName, String patchJson) throws Exception{
        JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchJson));

        CouponDetailsDTO result = assertDoesNotThrow(() -> {
            return couponService.partialUpdateCoupon(existingId, patch);
        });

        assertNotNull(result);
        assertEquals(existingId, result.id());
        assertEquals(coupon.getCode(), result.code());
        verify(couponRepository, times(1)).findById(existingId);
        verify(couponRepository, times(1)).saveAndFlush(any());
    }

    private static Stream<Arguments> provideValidPatches() {
        return Stream.of(
                Arguments.of(
                        "when updating type and value",
                        """
                            [
                                { "op": "replace", "path": "/type", "value": "percent" },
                                { "op": "replace", "path": "/value", "value": "50" }
                            ]
                            """),
                Arguments.of(
                        "when updating type and value",
                        """
                                [
                                    { "op": "replace", "path": "/maxUses", "value": 500 }
                                ]
                            """)
                ,
                Arguments.of(
                        "when updating type and value",
                        """
                                [
                                  { "op": "replace", "path": "/type", "value": "percent" }
                                ]
                            """),
                Arguments.of(
                        "when updating type and value",
                        """
                            [
                              { "op": "replace", "path": "/value", "value": 50 }
                            ]
                            """),
                Arguments.of(
                        "when updating type and value",
                        """
                            [
                              { "op": "replace", "path": "/code", "value": "sameCode" }
                            ]
                            """)
        );
    }

    @ParameterizedTest(name = "should fail with multiple errors {0}")
    @MethodSource("providePatchesWithMultipleErrors")
    @DisplayName("partialUpdateCoupon should collect all errors for invalid patch")
    void partialUpdateCoupon_shouldCollectAllErrors_forInvalidPatch(
            String scenarioName, String patchJson, List<String> expectedErrorKeys) throws Exception {

        JsonPatch patch = JsonPatch.fromJson(objectMapper.readTree(patchJson));

        BusinessRuleException exceptionResult = assertThrows(BusinessRuleException.class, () -> {
            couponService.partialUpdateCoupon(existingId, patch);
        });

        Map<String, String> errors = exceptionResult.getErrors();
        assertNotNull(errors, "Error map should not be null");
        assertEquals(expectedErrorKeys.size(), errors.size(), "Should find the exact number of errors");

        for (String expectedKey : expectedErrorKeys) {
            assertTrue(errors.containsKey(expectedKey), "Expected error map to contain key: " + expectedKey);
        }

        verify(couponRepository, times(1)).findById(existingId);
        verify(couponRepository, never()).saveAndFlush(any());
    }

    private static Stream<Arguments> providePatchesWithMultipleErrors() {
        return java.util.stream.Stream.of(
                Arguments.of(
                        "when value is invalid and dates are inverted",
                        """
                               [
                                  { "op": "replace", "path": "/maxUses", "value": 3 },
                                  { "op": "replace", "path": "/oneShot", "value": "true" },
                                  { "op": "replace", "path": "/usesCount", "value": 7 },
                                  { "op": "replace", "path": "/type", "value": "percent" },
                                  { "op": "replace", "path": "/value", "value": 90 },
                                  { "op": "replace", "path": "/validFrom", "value": "2025-06-29T00:00:00Z" },
                                  { "op": "replace", "path": "/validUntil", "value": "2025-06-27T00:00:00Z" }
                                ]
                               """,
                        List.of("maxUses", "oneShot","value","validFrom","validUntil")
                ),
                Arguments.of(
                        "when maxUses conflicts with oneShot and usesCount",
                        """
                            [
                                { "op": "replace", "path": "/oneShot", "value": "false" },
                                { "op": "replace", "path": "/usesCount", "value": 7 },
                                { "op": "replace", "path": "/maxUses", "value": 3 },
                                { "op": "replace", "path": "/type", "value": "invalidType" },
                                { "op": "replace", "path": "/value", "value": 90 }
                            ]
                            """,
                        List.of("maxUses", "type")
                ),
                Arguments.of(
                        "when fixed coupon value is negative",
                        """
                            [
                                { "op": "replace", "path": "/type", "value": "fixed" },
                                { "op": "replace", "path": "/value", "value": -4 }
                            ]
                            """,
                        List.of("value")
                ),
                Arguments.of(
                        "when percent coupon value is less than 1",
                        """
                            [
                                { "op": "replace", "path": "/type", "value": "percent" },
                                { "op": "replace", "path": "/value", "value": 0.3 }
                            ]
                            """,
                        List.of("value")
                ),
                Arguments.of(
                        "when removing multiple required fields",
                        """
                                [
                                    { "op": "replace", "path": "/oneShot", "value": "true" },
                                    { "op": "replace", "path": "/usesCount", "value": 0 },
                                    { "op": "replace", "path": "/maxUses", "value": 3 },
                                    { "op": "replace", "path": "/type", "value": "fixed" },
                                    { "op": "remove", "path": "/value" },
                                    { "op": "replace", "path": "/validFrom", "value": "2031-06-27T00:00:00Z" },
                                    { "op": "remove", "path": "/validUntil" }
                                ]
                                """,
                        List.of("value", "validUntil","maxUses")
                ),
                Arguments.of(
                        "when type is invalid and date range exceeds 5 years",
                        """
                                [
                                    { "op": "replace", "path": "/validFrom", "value": "2025-06-29T00:00:00Z" },
                                    { "op": "replace", "path": "/validUntil", "value": "2031-06-27T00:00:00Z" }
                                ]
                                """,
                        List.of("validUntil")
                ),
                Arguments.of(
                        "when removing multiples required fields",
                        """
                        [
                            { "op": "remove", "path": "/type"},
                            { "op": "remove", "path": "/value" },
                            { "op": "remove", "path": "/validFrom" },
                            { "op": "remove", "path": "/validUntil" }
                        ]
                        """,
                        List.of("type", "value","validFrom", "validUntil")
                )
        );
    }
}
