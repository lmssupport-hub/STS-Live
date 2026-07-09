package com.example.nexus.controller;

import com.example.nexus.dto.InviteDto;
import com.example.nexus.entity.Invite;
import com.example.nexus.entity.RoleEntity;
import com.example.nexus.repository.RoleRepository;
import com.example.nexus.service.InviteService;
import com.example.nexus.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invites")
@CrossOrigin("*")
public class InviteController {

    @Autowired private InviteService inviteService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private JwtUtil jwtUtil;

    private String emailFrom(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!jwtUtil.validateToken(token)) {
            throw new com.example.nexus.exception.BadRequestException("Invalid or expired session. Please log in again.");
        }
        return jwtUtil.extractEmail(token);
    }

    // Admin sends the invite — same "Send Invite" click that used to hit /users/invite
    @PostMapping
    public ResponseEntity<?> createInvite(@Valid @RequestBody InviteDto.Request request,
                                           @RequestHeader("Authorization") String authHeader) {
        inviteService.createInvite(request.getEmail(), request.getRoleId(), emailFrom(authHeader));
        return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of("message", "Invite sent"));
    }

    // Register page hits this with the token from the link — no auth, the invited user isn't logged in yet
    @GetMapping("/{token}")
    public ResponseEntity<?> getInvite(@PathVariable String token) {
        Invite invite = inviteService.validateToken(token);
        String roleName = null;
        if (invite.getRoleId() != null) {
            roleName = roleRepository.findById(invite.getRoleId())
                    .map(RoleEntity::getName)
                    .orElse(null);
        }
        return ResponseEntity.ok(new InviteDto.InviteInfo(invite.getEmail(), roleName));
    }
}