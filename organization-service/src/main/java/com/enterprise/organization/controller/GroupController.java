package com.enterprise.organization.controller;

import com.cas.common.policy.RequiresPolicy;
import com.enterprise.organization.dto.OrgGroupDto;
import com.enterprise.organization.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@RequiresPolicy(resource = "GROUP", skip = true)
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public ResponseEntity<List<OrgGroupDto>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping(params = "type")
    public ResponseEntity<List<OrgGroupDto>> getGroupsByType(@RequestParam String type) {
        return ResponseEntity.ok(groupService.getGroupsByType(type));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrgGroupDto> getGroup(@PathVariable UUID id) {
        return ResponseEntity.ok(groupService.getGroup(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UUID>> getUserGroups(@PathVariable UUID userId) {
        return ResponseEntity.ok(groupService.getUserGroupIds(userId));
    }

    @PostMapping
    public ResponseEntity<OrgGroupDto> createGroup(@RequestBody OrgGroupDto.CreateRequest request) {
        return ResponseEntity.ok(groupService.createGroup(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrgGroupDto> updateGroup(
            @PathVariable UUID id,
            @RequestBody OrgGroupDto.UpdateRequest request) {
        return ResponseEntity.ok(groupService.updateGroup(id, request));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable UUID id,
            @RequestBody OrgGroupDto.AddMemberRequest request) {
        groupService.addMember(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        groupService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }
}
