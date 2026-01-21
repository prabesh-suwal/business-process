package com.enterprise.organization.service;

import com.enterprise.organization.dto.BranchDto;
import com.enterprise.organization.entity.Branch;
import com.enterprise.organization.entity.GeoLocation;
import com.enterprise.organization.repository.BranchRepository;
import com.enterprise.organization.repository.GeoLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchService {

    private final BranchRepository branchRepository;
    private final GeoLocationRepository geoLocationRepository;

    public List<BranchDto> getAllBranches() {
        return branchRepository.findByStatusOrderByName(Branch.Status.ACTIVE)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public BranchDto getBranch(UUID id) {
        return branchRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Branch not found: " + id));
    }

    public BranchDto getBranchByCode(String code) {
        return branchRepository.findByCode(code)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Branch not found: " + code));
    }

    @Transactional
    public BranchDto createBranch(BranchDto.CreateRequest request) {
        if (branchRepository.findByCode(request.getCode()).isPresent()) {
            throw new RuntimeException("Branch code already exists: " + request.getCode());
        }

        GeoLocation geoLocation = request.getGeoLocationId() != null
                ? geoLocationRepository.findById(request.getGeoLocationId()).orElse(null)
                : null;
        Branch parentBranch = request.getParentBranchId() != null
                ? branchRepository.findById(request.getParentBranchId()).orElse(null)
                : null;

        Branch branch = Branch.builder()
                .code(request.getCode())
                .name(request.getName())
                .localName(request.getLocalName())
                .branchType(request.getBranchType() != null
                        ? Branch.BranchType.valueOf(request.getBranchType())
                        : Branch.BranchType.BRANCH)
                .geoLocation(geoLocation)
                .parentBranch(parentBranch)
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .status(Branch.Status.ACTIVE)
                .build();

        branch = branchRepository.save(branch);
        log.info("Created branch: {} ({})", branch.getName(), branch.getCode());
        return toDto(branch);
    }

    @Transactional
    public BranchDto updateBranch(UUID id, BranchDto.UpdateRequest request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found: " + id));

        if (request.getName() != null)
            branch.setName(request.getName());
        if (request.getLocalName() != null)
            branch.setLocalName(request.getLocalName());
        if (request.getBranchType() != null)
            branch.setBranchType(Branch.BranchType.valueOf(request.getBranchType()));
        if (request.getAddress() != null)
            branch.setAddress(request.getAddress());
        if (request.getPhone() != null)
            branch.setPhone(request.getPhone());
        if (request.getEmail() != null)
            branch.setEmail(request.getEmail());
        if (request.getStatus() != null)
            branch.setStatus(Branch.Status.valueOf(request.getStatus()));

        if (request.getGeoLocationId() != null) {
            branch.setGeoLocation(geoLocationRepository.findById(request.getGeoLocationId()).orElse(null));
        }
        if (request.getParentBranchId() != null) {
            branch.setParentBranch(branchRepository.findById(request.getParentBranchId()).orElse(null));
        }

        branch = branchRepository.save(branch);
        log.info("Updated branch: {}", branch.getCode());
        return toDto(branch);
    }

    @Transactional
    public void deleteBranch(UUID id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found: " + id));
        branch.setStatus(Branch.Status.CLOSED);
        branchRepository.save(branch);
        log.info("Closed branch: {}", branch.getCode());
    }

    private BranchDto toDto(Branch b) {
        return BranchDto.builder()
                .id(b.getId())
                .code(b.getCode())
                .name(b.getName())
                .localName(b.getLocalName())
                .branchType(b.getBranchType().name())
                .geoLocationId(b.getGeoLocation() != null ? b.getGeoLocation().getId() : null)
                .geoLocationName(b.getGeoLocation() != null ? b.getGeoLocation().getName() : null)
                .parentBranchId(b.getParentBranch() != null ? b.getParentBranch().getId() : null)
                .parentBranchName(b.getParentBranch() != null ? b.getParentBranch().getName() : null)
                .address(b.getAddress())
                .phone(b.getPhone())
                .email(b.getEmail())
                .status(b.getStatus().name())
                .build();
    }
}
