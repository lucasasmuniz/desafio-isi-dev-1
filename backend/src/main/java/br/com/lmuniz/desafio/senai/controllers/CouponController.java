package br.com.lmuniz.desafio.senai.controllers;

import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponDetailsDTO;
import br.com.lmuniz.desafio.senai.services.CouponService;
import com.github.fge.jsonpatch.JsonPatch;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    public ResponseEntity<CouponDTO> createCoupon(@Valid @RequestBody CouponDTO couponDTO){
        couponDTO = couponService.createCoupon(couponDTO);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("{id}")
                .buildAndExpand(couponDTO.id()).toUri();
        return ResponseEntity.created(uri).body(couponDTO);
    }

    @GetMapping
    public ResponseEntity<List<CouponDTO>> getAllCoupons(@RequestParam(value = "onlyValid", defaultValue = "false") boolean onlyValid) {
        return ResponseEntity.ok(couponService.getAllCoupons(onlyValid));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponDetailsDTO> getCouponById(@PathVariable Long id){
        return ResponseEntity.ok(couponService.getCouponById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id}", consumes = "application/json-patch+json")
    public ResponseEntity<CouponDetailsDTO> partialUpdateCoupon(
            @PathVariable Long id,
            @RequestBody JsonPatch patch) {
        CouponDetailsDTO updatedCoupon = couponService.partialUpdateCoupon(id, patch);
        return ResponseEntity.ok(updatedCoupon);
    }
}
