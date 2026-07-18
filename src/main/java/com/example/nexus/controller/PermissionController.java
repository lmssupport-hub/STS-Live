package com.example.nexus.controller;

import com.example.nexus.dto.EffectivePermissionsDto;
import com.example.nexus.exception.BadRequestException;
import com.example.nexus.service.RoleService;
import com.example.nexus.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/permissions")
@CrossOrigin("*")
public class PermissionController {

    @Autowired private RoleService roleService;
    @Autowired private JwtUtil jwtUtil;

    private String emailFrom(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!jwtUtil.validateToken(token)) {
            throw new BadRequestException("Invalid or expired session. Please log in again.");
        }
        return jwtUtil.extractEmail(token);
    }

    // Called right after login (and on app refresh) to know what screens/actions this user can see
    @GetMapping("/me")
    public ResponseEntity<EffectivePermissionsDto> getMyPermissions(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(roleService.getEffectivePermissions(emailFrom(authHeader)));
    }
}