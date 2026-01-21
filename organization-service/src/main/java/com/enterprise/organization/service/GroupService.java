package com.enterprise.organization.service;

import com.enterprise.organization.dto.OrgGroupDto;
import com.enterprise.organization.entity.GroupMember;
import com.enterprise.organization.entity.OrgGroup;
import com.enterprise.organization.repository.*;
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
public class GroupService {

    private final OrgGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final BranchRepository branchRepository;
    private final DepartmentRepository departmentRepository;

    public List<OrgGroupDto> getAllGroups() {
        return groupRepository.findByStatusOrderByName(OrgGroup.Status.ACTIVE)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<OrgGroupDto> getGroupsByType(String groupType) {
        return groupRepository.findByGroupTypeAndStatus(OrgGroup.GroupType.valueOf(groupType), OrgGroup.Status.ACTIVE)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public OrgGroupDto getGroup(UUID id) {
        return groupRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Group not found: " + id));
    }

    public List<UUID> getUserGroupIds(UUID userId) {
        return memberRepository.findByUserId(userId).stream()
                .map(m -> m.getGroup().getId())
                .collect(Collectors.toList());
    }

    @Transactional
    public OrgGroupDto createGroup(OrgGroupDto.CreateRequest request) {
        if (groupRepository.findByCode(request.getCode()).isPresent()) {
            throw new RuntimeException("Group code already exists: " + request.getCode());
        }

        OrgGroup group = OrgGroup.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .groupType(request.getGroupType() != null
                        ? OrgGroup.GroupType.valueOf(request.getGroupType())
                        : OrgGroup.GroupType.TEAM)
                .branch(request.getBranchId() != null
                        ? branchRepository.findById(request.getBranchId()).orElse(null)
                        : null)
                .department(request.getDepartmentId() != null
                        ? departmentRepository.findById(request.getDepartmentId()).orElse(null)
                        : null)
                .status(OrgGroup.Status.ACTIVE)
                .build();

        group = groupRepository.save(group);
        log.info("Created group: {}", group.getName());
        return toDto(group);
    }

    @Transactional
    public OrgGroupDto updateGroup(UUID id, OrgGroupDto.UpdateRequest request) {
        OrgGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found: " + id));

        if (request.getName() != null)
            group.setName(request.getName());
        if (request.getDescription() != null)
            group.setDescription(request.getDescription());
        if (request.getGroupType() != null)
            group.setGroupType(OrgGroup.GroupType.valueOf(request.getGroupType()));
        if (request.getStatus() != null)
            group.setStatus(OrgGroup.Status.valueOf(request.getStatus()));

        group = groupRepository.save(group);
        log.info("Updated group: {}", group.getName());
        return toDto(group);
    }

    @Transactional
    public void addMember(UUID groupId, OrgGroupDto.AddMemberRequest request) {
        OrgGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        if (memberRepository.existsByGroupIdAndUserId(groupId, request.getUserId())) {
            throw new RuntimeException("User is already a member of this group");
        }

        GroupMember member = GroupMember.builder()
                .group(group)
                .userId(request.getUserId())
                .memberRole(request.getMemberRole() != null
                        ? GroupMember.MemberRole.valueOf(request.getMemberRole())
                        : GroupMember.MemberRole.MEMBER)
                .build();

        memberRepository.save(member);
        log.info("Added user {} to group {}", request.getUserId(), group.getName());
    }

    @Transactional
    public void removeMember(UUID groupId, UUID userId) {
        memberRepository.deleteByGroupIdAndUserId(groupId, userId);
        log.info("Removed user {} from group {}", userId, groupId);
    }

    private OrgGroupDto toDto(OrgGroup g) {
        List<OrgGroupDto.MemberDto> members = g.getMembers().stream()
                .map(m -> OrgGroupDto.MemberDto.builder()
                        .id(m.getId())
                        .userId(m.getUserId())
                        .memberRole(m.getMemberRole().name())
                        .build())
                .collect(Collectors.toList());

        return OrgGroupDto.builder()
                .id(g.getId())
                .code(g.getCode())
                .name(g.getName())
                .description(g.getDescription())
                .groupType(g.getGroupType().name())
                .branchId(g.getBranch() != null ? g.getBranch().getId() : null)
                .branchName(g.getBranch() != null ? g.getBranch().getName() : null)
                .departmentId(g.getDepartment() != null ? g.getDepartment().getId() : null)
                .departmentName(g.getDepartment() != null ? g.getDepartment().getName() : null)
                .status(g.getStatus().name())
                .members(members)
                .build();
    }
}
