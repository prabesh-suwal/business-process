package com.enterprise.lms.controller;

import com.enterprise.lms.dto.LoanProductDTO;
import com.enterprise.lms.service.LoanProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/loan-products")
@RequiredArgsConstructor
public class LoanProductController {

    private final LoanProductService loanProductService;

    @PostMapping
    public ResponseEntity<LoanProductDTO> createProduct(@Valid @RequestBody LoanProductDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanProductService.createProduct(request));
    }

    @GetMapping
    public ResponseEntity<List<LoanProductDTO>> getActiveProducts() {
        return ResponseEntity.ok(loanProductService.getActiveProducts());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<LoanProductDTO> getProductByCode(@PathVariable String code) {
        return ResponseEntity.ok(loanProductService.getProductByCode(code));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanProductDTO> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(loanProductService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoanProductDTO> updateProduct(
            @PathVariable UUID id,
            @RequestBody LoanProductDTO request) {
        return ResponseEntity.ok(loanProductService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        loanProductService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
