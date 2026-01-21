package com.enterprise.lms.service;

import com.enterprise.lms.dto.LoanProductDTO;
import com.enterprise.lms.entity.LoanProduct;
import com.enterprise.lms.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanProductService {

    private final LoanProductRepository loanProductRepository;

    public LoanProductDTO createProduct(LoanProductDTO dto) {
        if (loanProductRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Product with code " + dto.getCode() + " already exists");
        }

        LoanProduct product = LoanProduct.builder()
                .code(dto.getCode().toUpperCase())
                .name(dto.getName())
                .description(dto.getDescription())
                .minAmount(dto.getMinAmount())
                .maxAmount(dto.getMaxAmount())
                .interestRate(dto.getInterestRate())
                .minTenure(dto.getMinTenure())
                .maxTenure(dto.getMaxTenure())
                .processingFeePercent(dto.getProcessingFeePercent())
                .workflowTemplateId(dto.getWorkflowTemplateId())
                .applicationFormId(dto.getApplicationFormId())
                .config(dto.getConfig())
                .active(true)
                .build();

        product = loanProductRepository.save(product);
        log.info("Created loan product: {} ({})", product.getName(), product.getCode());

        return toDTO(product);
    }

    @Transactional(readOnly = true)
    public List<LoanProductDTO> getActiveProducts() {
        return loanProductRepository.findByActiveTrue()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanProductDTO getProductByCode(String code) {
        return loanProductRepository.findByCode(code.toUpperCase())
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + code));
    }

    @Transactional(readOnly = true)
    public LoanProductDTO getProductById(UUID id) {
        return loanProductRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    public LoanProductDTO updateProduct(UUID id, LoanProductDTO dto) {
        LoanProduct product = loanProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        if (dto.getName() != null)
            product.setName(dto.getName());
        if (dto.getDescription() != null)
            product.setDescription(dto.getDescription());
        if (dto.getMinAmount() != null)
            product.setMinAmount(dto.getMinAmount());
        if (dto.getMaxAmount() != null)
            product.setMaxAmount(dto.getMaxAmount());
        if (dto.getInterestRate() != null)
            product.setInterestRate(dto.getInterestRate());
        if (dto.getMinTenure() != null)
            product.setMinTenure(dto.getMinTenure());
        if (dto.getMaxTenure() != null)
            product.setMaxTenure(dto.getMaxTenure());
        if (dto.getProcessingFeePercent() != null)
            product.setProcessingFeePercent(dto.getProcessingFeePercent());
        if (dto.getWorkflowTemplateId() != null)
            product.setWorkflowTemplateId(dto.getWorkflowTemplateId());
        if (dto.getApplicationFormId() != null)
            product.setApplicationFormId(dto.getApplicationFormId());
        if (dto.getConfig() != null)
            product.setConfig(dto.getConfig());
        if (dto.getActive() != null)
            product.setActive(dto.getActive());

        product = loanProductRepository.save(product);
        log.info("Updated loan product: {}", product.getCode());

        return toDTO(product);
    }

    public void deleteProduct(UUID id) {
        LoanProduct product = loanProductRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        loanProductRepository.delete(product);
        log.info("Deleted loan product: {} ({})", product.getName(), product.getCode());
    }

    private LoanProductDTO toDTO(LoanProduct product) {
        return LoanProductDTO.builder()
                .id(product.getId())
                .code(product.getCode())
                .name(product.getName())
                .description(product.getDescription())
                .minAmount(product.getMinAmount())
                .maxAmount(product.getMaxAmount())
                .interestRate(product.getInterestRate())
                .minTenure(product.getMinTenure())
                .maxTenure(product.getMaxTenure())
                .processingFeePercent(product.getProcessingFeePercent())
                .workflowTemplateId(product.getWorkflowTemplateId())
                .applicationFormId(product.getApplicationFormId())
                .active(product.getActive())
                .config(product.getConfig())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
