package com.example.nexus.service;

import com.example.nexus.entity.Invite;
import com.example.nexus.entity.RoleEntity;
import com.example.nexus.entity.User;
import com.example.nexus.exception.BadRequestException;
import com.example.nexus.exception.ConflictException;
import com.example.nexus.exception.ResourceNotFoundException;
import com.example.nexus.repository.InviteRepository;
import com.example.nexus.repository.RoleRepository;
import com.example.nexus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InviteService {

    private static final int INVITE_VALIDITY_HOURS = 72;

    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url:https://vativahubnexus.netlify.app}")
    private String frontendUrl;

    @Autowired
    public InviteService(InviteRepository inviteRepository, UserRepository userRepository,
                          RoleRepository roleRepository, EmailService emailService) {
        this.inviteRepository = inviteRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
    }

    // ── Admin clicks "Send Invite" ────────────────────────────────────
    @Transactional
    public void createInvite(String rawEmail, Long roleId, String adminEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim();

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("This email is already registered");
        }
        if (inviteRepository.existsByEmailIgnoreCaseAndUsedFalse(email)) {
            throw new ConflictException("An invite has already been sent to this email");
        }

        // Role, if picked, must actually belong to this admin — same guard as everywhere else
        String roleName = null;
        if (roleId != null) {
            RoleEntity role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
            if (!role.getCreatedByAdminId().equals(admin.getId())) {
                throw new BadRequestException("That role does not belong to you");
            }
            roleName = role.getName();
        }

        Invite invite = new Invite();
        invite.setEmail(email);
        invite.setToken(UUID.randomUUID().toString());
        invite.setRoleId(roleId);
        invite.setInvitedByAdminId(admin.getId());
        invite.setExpiresAt(LocalDateTime.now().plusHours(INVITE_VALIDITY_HOURS));
        inviteRepository.save(invite);

        String registerLink = frontendUrl + "/auth?invite=" + invite.getToken();
        emailService.sendInviteEmail(email, registerLink, roleName);
    }

    // ── Register page loads with ?invite=token — used to prefill/lock the email ──
    @Transactional(readOnly = true)
    public Invite validateToken(String token) {
        Invite invite = inviteRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired invite link"));

        if (LocalDateTime.now().isAfter(invite.getExpiresAt())) {
            throw new BadRequestException("This invite link has expired. Ask your admin to resend it.");
        }
        return invite;
    }

    @Transactional
    public void markUsed(Invite invite) {
        invite.setUsed(true);
        inviteRepository.save(invite);
    }
}