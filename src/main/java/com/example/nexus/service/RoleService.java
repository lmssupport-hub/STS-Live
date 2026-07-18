package com.example.nexus.service;

import com.example.nexus.dto.EffectivePermissionsDto;
import com.example.nexus.dto.PackageDto;
import com.example.nexus.dto.RoleDto;
import com.example.nexus.entity.RoleEntity;
import com.example.nexus.entity.User;
import com.example.nexus.exception.ConflictException;
import com.example.nexus.exception.ResourceNotFoundException;
import com.example.nexus.repository.RoleRepository;
import com.example.nexus.repository.UserRepository;
import com.example.nexus.util.PermissionJsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PackageService packageService;
    private final PermissionJsonMapper permissionJsonMapper;

    @Autowired
    public RoleService(RoleRepository roleRepository, UserRepository userRepository,
                        PackageService packageService, PermissionJsonMapper permissionJsonMapper) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.packageService = packageService;
        this.permissionJsonMapper = permissionJsonMapper;
    }

    // ── Permission inheritance: only what the Super Admin's package granted this Admin ──
    @Transactional(readOnly = true)
    public List<PackageDto.Category> getAssignedFeatures(String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (admin.getAssignedPackageId() == null) {
            return List.of();
        }

        PackageDto.Response pkg = packageService.getPackageById(admin.getAssignedPackageId());
        List<PackageDto.Category> categories = pkg.getPermissions() == null ? List.of() : pkg.getPermissions();

        // Drop categories the package never enabled (e.g. Leads, Billing)
        return categories.stream()
                .filter(PackageDto.Category::isEnabled)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoleDto.Response> getRolesForAdmin(String adminEmail) {
        User admin = requireAdmin(adminEmail);
        return roleRepository.findByCreatedByAdminId(admin.getId()).stream()
                .map(entity -> {
                    try {
                        return toResponse(entity);
                    } catch (Exception e) {
                        System.err.println("Skipping corrupt role id=" + entity.getId() + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleDto.Response getRoleById(Long id, String adminEmail) {
        User admin = requireAdmin(adminEmail);
        return toResponse(findOwnedOrThrow(id, admin.getId()));
    }

    @Transactional
    public RoleDto.Response createRole(RoleDto.Request request, String adminEmail) {
        User admin = requireAdmin(adminEmail);

        if (roleRepository.existsByNameIgnoreCaseAndCreatedByAdminId(request.getName(), admin.getId())) {
            throw new ConflictException("A role named '" + request.getName() + "' already exists");
        }

        List<PackageDto.Category> validated = clampToAllowed(request.getPermissions(), getAssignedFeatures(adminEmail));

        RoleEntity entity = new RoleEntity();
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        entity.setCreatedByAdminId(admin.getId());
        entity.setPermissionsJson(permissionJsonMapper.toJson(validated));

        return toResponse(roleRepository.save(entity));
    }

    @Transactional
    public RoleDto.Response updateRole(Long id, RoleDto.Request request, String adminEmail) {
        User admin = requireAdmin(adminEmail);
        RoleEntity entity = findOwnedOrThrow(id, admin.getId());

        if (roleRepository.existsByNameIgnoreCaseAndCreatedByAdminIdAndIdNot(request.getName(), admin.getId(), id)) {
            throw new ConflictException("A role named '" + request.getName() + "' already exists");
        }

        List<PackageDto.Category> validated = clampToAllowed(request.getPermissions(), getAssignedFeatures(adminEmail));

        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        entity.setPermissionsJson(permissionJsonMapper.toJson(validated));

        return toResponse(roleRepository.save(entity));
    }

    @Transactional
    public void deleteRole(Long id, String adminEmail) {
        User admin = requireAdmin(adminEmail);
        roleRepository.delete(findOwnedOrThrow(id, admin.getId()));
    }

    @Transactional
    public void assignRoleToUser(Long roleId, Long userId, String adminEmail) {
        User admin = requireAdmin(adminEmail);
        findOwnedOrThrow(roleId, admin.getId()); // confirms this role belongs to the requesting Admin

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setAssignedRoleId(roleId);
        userRepository.save(user);
    }

    // ── Never persist a permission the Admin's own package didn't grant, even if the
    //    request tried to sneak one in (frontend bug, tampered payload, etc.) ──
    private List<PackageDto.Category> clampToAllowed(List<PackageDto.Category> requested, List<PackageDto.Category> allowed) {
        Map<String, PackageDto.Category> allowedByCatId = allowed.stream()
                .collect(Collectors.toMap(PackageDto.Category::getId, c -> c));

        List<PackageDto.Category> result = new ArrayList<>();
        for (PackageDto.Category cat : requested) {
            PackageDto.Category allowedCat = allowedByCatId.get(cat.getId());
            if (allowedCat == null) continue; // category not granted at all — drop it entirely

            Map<String, PackageDto.Feature> allowedFeaturesById = allowedCat.getFeatures().stream()
                    .collect(Collectors.toMap(PackageDto.Feature::getId, f -> f));

            List<PackageDto.Feature> clampedFeatures = new ArrayList<>();
            boolean anyOn = false;
            for (PackageDto.Feature f : cat.getFeatures()) {
                PackageDto.Feature allowedFeature = allowedFeaturesById.get(f.getId());
                PackageDto.Permission src = f.getPermissions();
                PackageDto.Permission allowedPerm = allowedFeature != null
                        ? allowedFeature.getPermissions() : new PackageDto.Permission();

                PackageDto.Permission clamped = new PackageDto.Permission(
                        src.isCreate() && allowedPerm.isCreate(),
                        src.isRead() && allowedPerm.isRead(),
                        src.isUpdate() && allowedPerm.isUpdate(),
                        src.isDelete() && allowedPerm.isDelete()
                );
                anyOn = anyOn || clamped.isCreate() || clamped.isRead() || clamped.isUpdate() || clamped.isDelete();

                PackageDto.Feature clampedFeature = new PackageDto.Feature();
                clampedFeature.setId(f.getId());
                clampedFeature.setName(f.getName());
                clampedFeature.setPermissions(clamped);
                clampedFeatures.add(clampedFeature);
            }

            PackageDto.Category clampedCat = new PackageDto.Category();
            clampedCat.setId(cat.getId());
            clampedCat.setName(cat.getName());
            // Keep the category enabled if either: (a) at least one clamped feature
            // permission is on, or (b) the requester explicitly toggled the category on
            // AND the admin's own package also grants that category. This second path
            // covers categories like "Authentication" that are driven purely by the
            // category-level toggle rather than per-feature checkboxes.
            clampedCat.setEnabled(anyOn || (cat.isEnabled() && allowedCat.isEnabled()));
            clampedCat.setFeatures(clampedFeatures);
            result.add(clampedCat);
        }
        return result;
    }

    private User requireAdmin(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private RoleEntity findOwnedOrThrow(Long id, Long adminId) {
        RoleEntity entity = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        if (!entity.getCreatedByAdminId().equals(adminId)) {
            throw new ResourceNotFoundException("Role not found with id: " + id); // don't leak existence
        }
        return entity;
    }

    private RoleDto.Response toResponse(RoleEntity entity) {
        RoleDto.Response response = new RoleDto.Response();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setCreatedAt(entity.getCreatedAt());
        response.setPermissions(permissionJsonMapper.fromJson(entity.getPermissionsJson()));
        return response;
    }

    // ── Single source of truth for "what can this logged-in user see/do" ──
    // SUPER_ADMIN -> fullAccess=true (frontend shows everything, no check needed)
    // ADMIN       -> resolved from their assigned PACKAGE
    // everyone else (MEMBER / custom roles) -> resolved from their assigned ROLE
    @Transactional(readOnly = true)
    public EffectivePermissionsDto getEffectivePermissions(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        EffectivePermissionsDto dto = new EffectivePermissionsDto();
        dto.setRoleType(user.getRole());

        if ("SUPER_ADMIN".equals(user.getRole())) {
            dto.setFullAccess(true);
            dto.setPermissions(List.of());
            return dto;
        }

        dto.setFullAccess(false);

        if ("ADMIN".equals(user.getRole())) {
            if (user.getAssignedPackageId() == null) {
                dto.setPermissions(List.of());
                return dto;
            }
            PackageDto.Response pkg = packageService.getPackageById(user.getAssignedPackageId());
            List<PackageDto.Category> categories = pkg.getPermissions() == null ? List.of() : pkg.getPermissions();
            dto.setPermissions(categories.stream()
                    .filter(PackageDto.Category::isEnabled)
                    .collect(Collectors.toList()));
            return dto;
        }

        // MEMBER or any other custom role -> permissions come from their assigned ROLE
        if (user.getAssignedRoleId() == null) {
            dto.setPermissions(List.of());
            return dto;
        }

        RoleEntity roleEntity = roleRepository.findById(user.getAssignedRoleId()).orElse(null);
        if (roleEntity == null) {
            dto.setPermissions(List.of());
            return dto;
        }

        List<PackageDto.Category> categories = permissionJsonMapper.fromJson(roleEntity.getPermissionsJson());
        dto.setPermissions(categories.stream()
                .filter(PackageDto.Category::isEnabled)
                .collect(Collectors.toList()));
        return dto;
    }

    // ── Login gate — the "Authentication" category toggle must be ON for this
    //    user's package (ADMIN) or role (MEMBER/custom) before they're allowed to log in.
    //    No sub-feature needed — just the category-level enabled flag.
    //    SUPER_ADMIN always passes — never gated by any toggle.
    @Transactional(readOnly = true)
    public boolean hasAuthenticationAccess(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if ("SUPER_ADMIN".equals(user.getRole())) {
            return true;
        }

        List<PackageDto.Category> categories;

        if ("ADMIN".equals(user.getRole())) {
            if (user.getAssignedPackageId() == null) {
                return false;
            }
            try {
                PackageDto.Response pkg = packageService.getPackageById(user.getAssignedPackageId());
                categories = pkg.getPermissions() == null ? List.of() : pkg.getPermissions();
            } catch (ResourceNotFoundException e) {
                return false; // package was deleted after being assigned
            }
        } else {
            if (user.getAssignedRoleId() == null) {
                return false;
            }
            RoleEntity roleEntity = roleRepository.findById(user.getAssignedRoleId()).orElse(null);
            if (roleEntity == null) {
                return false;
            }
            categories = permissionJsonMapper.fromJson(roleEntity.getPermissionsJson());
        }

        return categories.stream()
                .anyMatch(c -> "auth".equals(c.getId()) && c.isEnabled());
    }
}