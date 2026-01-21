package com.cas.server.controller.admin;

import com.cas.server.dto.ApiClientDto;
import com.cas.server.dto.PageDto;
import com.cas.server.service.ApiClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/clients")
@RequiredArgsConstructor
public class ApiClientAdminController {

    private final ApiClientService apiClientService;

    @GetMapping
    public ResponseEntity<PageDto<ApiClientDto>> listClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        return ResponseEntity.ok(apiClientService.listClients(PageRequest.of(page, size, sort)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiClientDto> getClient(@PathVariable UUID id) {
        return ResponseEntity.ok(apiClientService.getClient(id));
    }

    @PostMapping
    public ResponseEntity<ApiClientDto> createClient(@RequestBody ApiClientDto.CreateClientRequest request) {
        return ResponseEntity.ok(apiClientService.createClient(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiClientDto> updateClient(
            @PathVariable UUID id,
            @RequestBody ApiClientDto.UpdateClientRequest request) {
        return ResponseEntity.ok(apiClientService.updateClient(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeClient(@PathVariable UUID id) {
        apiClientService.revokeClient(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rotate-secret")
    public ResponseEntity<ApiClientDto> rotateSecret(@PathVariable UUID id) {
        return ResponseEntity.ok(apiClientService.rotateSecret(id));
    }
}
