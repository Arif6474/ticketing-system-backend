package com.ticket.system.service;

import com.ticket.system.dto.request.CreateUserRequest;
import com.ticket.system.dto.request.ForcePasswordResetRequest;
import com.ticket.system.dto.request.UpdateUserRequest;
import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.PageResponse;
import com.ticket.system.dto.response.UserResponse;
import com.ticket.system.entity.ClientOrganization;
import com.ticket.system.entity.Role;
import com.ticket.system.entity.User;
import com.ticket.system.exception.AppException;
import com.ticket.system.repository.ClientOrganizationRepository;
import com.ticket.system.repository.PasswordResetTokenRepository;
import com.ticket.system.repository.UserRepository;
import com.ticket.system.repository.UserSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ClientOrganizationRepository orgRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       ClientOrganizationRepository orgRepository,
                       PasswordResetTokenRepository resetTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.orgRepository = orgRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(UUID currentUserId, int page, int size, String search,
                                               Role role, Boolean active, UUID requestedOrgId) {
        User currentUser = getCurrentUser(currentUserId);

        if (currentUser.getRole() == Role.CLIENT_USER) {
            throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER is not authorized to access user management");
        }

        UUID targetOrgId = requestedOrgId;
        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            targetOrgId = currentUser.getOrganization().getId();
        }

        Specification<User> spec = UserSpecification.filterUsers(search, role, active, targetOrgId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").ascending()));

        Page<UserResponse> userPage = userRepository.findAll(spec, pageable)
                .map(UserResponse::fromEntity);

        return PageResponse.fromPage(userPage);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID currentUserId, UUID targetUserId) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() == Role.CLIENT_USER) {
            throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER is not authorized to access user details");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (targetUser.getOrganization() == null ||
                !Objects.equals(targetUser.getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN can only view users in their own organization");
            }
        }

        return UserResponse.fromEntity(targetUser);
    }

    @Transactional
    public UserResponse createUser(UUID currentUserId, CreateUserRequest request) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() == Role.CLIENT_USER) {
            throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER is not authorized to create users");
        }

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "User with email already exists: " + request.getEmail());
        }

        ClientOrganization targetOrg = null;

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (request.getRole() == Role.APP_ADMIN) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN cannot create APP_ADMIN users");
            }
            targetOrg = currentUser.getOrganization();
        } else if (currentUser.getRole() == Role.APP_ADMIN) {
            if (request.getRole() == Role.APP_ADMIN) {
                targetOrg = null;
            } else {
                if (request.getOrganizationId() == null) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Organization is required for " + request.getRole());
                }
                targetOrg = orgRepository.findById(request.getOrganizationId())
                        .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Specified organization does not exist"));
                if (!targetOrg.isActive()) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Cannot create user for an inactive organization");
                }
            }
        }

        User newUser = new User();
        newUser.setEmail(request.getEmail().toLowerCase().trim());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setMobile(request.getMobile());
        newUser.setDesignation(request.getDesignation());
        newUser.setOffice(request.getOffice());
        newUser.setRole(request.getRole());
        newUser.setOrganization(targetOrg);
        newUser.setActive(true);

        User savedUser = userRepository.save(newUser);
        return UserResponse.fromEntity(savedUser);
    }

    @Transactional
    public UserResponse updateUser(UUID currentUserId, UUID targetUserId, UpdateUserRequest request) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() == Role.CLIENT_USER) {
            throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER is not authorized to update users");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (targetUser.getOrganization() == null ||
                !Objects.equals(targetUser.getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN can only edit users in their own organization");
            }

            if (targetUser.getRole() == Role.APP_ADMIN) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN cannot edit APP_ADMIN users");
            }

            if (request.getRole() != null && request.getRole() == Role.APP_ADMIN) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN cannot promote users to APP_ADMIN");
            }

            if (request.getRole() != null) {
                targetUser.setRole(request.getRole());
            }
        } else if (currentUser.getRole() == Role.APP_ADMIN) {
            Role newRole = request.getRole() != null ? request.getRole() : targetUser.getRole();

            if (newRole == Role.APP_ADMIN) {
                targetUser.setOrganization(null);
            } else {
                UUID newOrgId = request.getOrganizationId() != null ? request.getOrganizationId() :
                        (targetUser.getOrganization() != null ? targetUser.getOrganization().getId() : null);

                if (newOrgId == null) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "Organization is required for " + newRole);
                }

                ClientOrganization newOrg = orgRepository.findById(newOrgId)
                        .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Specified organization does not exist"));
                targetUser.setOrganization(newOrg);
            }

            targetUser.setRole(newRole);
        }

        targetUser.setFirstName(request.getFirstName());
        targetUser.setLastName(request.getLastName());
        targetUser.setMobile(request.getMobile());
        targetUser.setDesignation(request.getDesignation());
        targetUser.setOffice(request.getOffice());

        if (request.getActive() != null) {
            targetUser.setActive(request.getActive());
        }

        User updatedUser = userRepository.save(targetUser);
        return UserResponse.fromEntity(updatedUser);
    }

    @Transactional
    public UserResponse deactivateUser(UUID currentUserId, UUID targetUserId) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() == Role.CLIENT_USER) {
            throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER is not authorized to deactivate users");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (targetUser.getOrganization() == null ||
                !Objects.equals(targetUser.getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN can only deactivate users in their own organization");
            }
            if (targetUser.getRole() == Role.APP_ADMIN) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN cannot deactivate APP_ADMIN users");
            }
        }

        targetUser.setActive(false);
        User updated = userRepository.save(targetUser);
        return UserResponse.fromEntity(updated);
    }

    @Transactional
    public GenericResponse hardDeleteUser(UUID currentUserId, UUID targetUserId) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() != Role.APP_ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only APP_ADMIN is authorized to hard delete users");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        resetTokenRepository.deleteByUser(targetUser);
        userRepository.delete(targetUser);

        return new GenericResponse("User deleted successfully");
    }

    @Transactional
    public GenericResponse forcePasswordReset(UUID currentUserId, UUID targetUserId, ForcePasswordResetRequest request) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() == Role.CLIENT_USER) {
            throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER is not authorized to force reset passwords");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (targetUser.getOrganization() == null ||
                !Objects.equals(targetUser.getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN can only force reset passwords for users in their own organization");
            }
            if (targetUser.getRole() == Role.APP_ADMIN) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN cannot force reset APP_ADMIN passwords");
            }
        }

        targetUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(targetUser);

        resetTokenRepository.deleteByUser(targetUser);

        return new GenericResponse("Password reset successfully for user: " + targetUser.getEmail());
    }

    private User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }
}
