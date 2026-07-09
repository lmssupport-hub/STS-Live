package com.example.nexus.controller;

import com.example.nexus.dto.PackageDto;
import com.example.nexus.dto.RoleDto;
import com.example.nexus.service.RoleService;
import com.example.nexus.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin("*")
public class RoleController {

    @Autowired private RoleService roleService;
    @Autowired private JwtUtil jwtUtil;

    private String emailFrom(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!jwtUtil.validateToken(token)) {
            throw new com.example.nexus.exception.BadRequestException("Invalid or expired session. Please log in again.");
        }
        return jwtUtil.extractEmail(token);
    }

    @GetMapping("/assigned-features")
    public ResponseEntity<List<PackageDto.Category>> getAssignedFeatures(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(roleService.getAssignedFeatures(emailFrom(authHeader)));
    }

    @GetMapping
    public ResponseEntity<List<RoleDto.Response>> getMyRoles(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(roleService.getRolesForAdmin(emailFrom(authHeader)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDto.Response> getRoleById(@PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(roleService.getRoleById(id, emailFrom(authHeader)));
    }

    @PostMapping
    public ResponseEntity<RoleDto.Response> createRole(@Valid @RequestBody RoleDto.Request request,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request, emailFrom(authHeader)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleDto.Response> updateRole(@PathVariable Long id,
            @Valid @RequestBody RoleDto.Request request, @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(roleService.updateRole(id, request, emailFrom(authHeader)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        roleService.deleteRole(id, emailFrom(authHeader));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roleId}/assign/{userId}")
    public ResponseEntity<Void> assignRole(@PathVariable Long roleId, @PathVariable Long userId,
            @RequestHeader("Authorization") String authHeader) {
        roleService.assignRoleToUser(roleId, userId, emailFrom(authHeader));
        return ResponseEntity.ok().build();
    }
}