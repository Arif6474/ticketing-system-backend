package com.ticket.system.service;

import com.ticket.system.dto.request.CreateModuleRequest;
import com.ticket.system.dto.request.UpdateModuleRequest;
import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.ModuleResponse;
import com.ticket.system.dto.response.PageResponse;
import com.ticket.system.entity.Module;
import com.ticket.system.entity.Project;
import com.ticket.system.entity.Role;
import com.ticket.system.entity.User;
import com.ticket.system.exception.AppException;
import com.ticket.system.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMembershipRepository membershipRepository;

    public ModuleService(ModuleRepository moduleRepository,
                         ProjectRepository projectRepository,
                         UserRepository userRepository,
                         ProjectMembershipRepository membershipRepository) {
        this.moduleRepository = moduleRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public ModuleResponse createModule(UUID currentUserId, CreateModuleRequest request) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() == Role.CLIENT_USER) {
            throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER is not authorized to create modules");
        }

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Specified project does not exist"));

        if (!project.isActive()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot create module for an inactive project");
        }

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(project.getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN can only create modules for projects in their own organization");
            }
        }

        String moduleName = request.getName().trim();
        if (moduleRepository.existsByProjectIdAndNameIgnoreCase(project.getId(), moduleName)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Module name already exists for this project: " + moduleName);
        }

        Module module = new Module(project, moduleName, request.getDescription());
        Module saved = moduleRepository.save(module);

        return ModuleResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ModuleResponse> getModules(UUID currentUserId, int page, int size,
                                                   String search, Boolean active, UUID requestedProjectId) {
        User currentUser = getCurrentUser(currentUserId);

        UUID targetProjectId = requestedProjectId;
        UUID targetOrgId = null;
        UUID memberUserId = null;

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            targetOrgId = currentUser.getOrganization().getId();
        } else if (currentUser.getRole() == Role.CLIENT_USER) {
            memberUserId = currentUser.getId();
            targetOrgId = currentUser.getOrganization().getId();
        }

        // If a specific projectId is requested, verify accessibility
        if (targetProjectId != null) {
            Project project = projectRepository.findById(targetProjectId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Project not found"));

            if (currentUser.getRole() == Role.CLIENT_ADMIN) {
                if (!Objects.equals(project.getOrganization().getId(), currentUser.getOrganization().getId())) {
                    throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN cannot access modules of another organization's project");
                }
            } else if (currentUser.getRole() == Role.CLIENT_USER) {
                if (!Objects.equals(project.getOrganization().getId(), currentUser.getOrganization().getId()) ||
                    !membershipRepository.existsByProjectIdAndUserId(targetProjectId, currentUser.getId())) {
                    throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER can only access modules of member projects");
                }
            }
        }

        Specification<Module> spec = ModuleSpecification.filterModules(search, active, targetProjectId, targetOrgId, memberUserId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").ascending()));

        Page<ModuleResponse> dtoPage = moduleRepository.findAll(spec, pageable)
                .map(ModuleResponse::fromEntity);

        return PageResponse.fromPage(dtoPage);
    }

    @Transactional(readOnly = true)
    public ModuleResponse getModuleById(UUID currentUserId, UUID moduleId) {
        User currentUser = getCurrentUser(currentUserId);
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Module not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(module.getProject().getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN cannot access modules of another organization");
            }
        } else if (currentUser.getRole() == Role.CLIENT_USER) {
            if (!Objects.equals(module.getProject().getOrganization().getId(), currentUser.getOrganization().getId()) ||
                !membershipRepository.existsByProjectIdAndUserId(module.getProject().getId(), currentUser.getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER can only access modules of member projects");
            }
        }

        return ModuleResponse.fromEntity(module);
    }

    @Transactional
    public ModuleResponse updateModule(UUID currentUserId, UUID moduleId, UpdateModuleRequest request) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() == Role.CLIENT_USER) {
            throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER is not authorized to update modules");
        }

        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Module not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(module.getProject().getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN can only update modules in their own organization");
            }
        }

        String newName = request.getName().trim();
        if (moduleRepository.existsByProjectIdAndNameIgnoreCaseAndIdNot(module.getProject().getId(), newName, moduleId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Module name already exists for this project: " + newName);
        }

        module.setName(newName);
        module.setDescription(request.getDescription());
        if (request.getActive() != null) {
            module.setActive(request.getActive());
        }

        Module updated = moduleRepository.save(module);
        return ModuleResponse.fromEntity(updated);
    }

    @Transactional
    public GenericResponse deleteModule(UUID currentUserId, UUID moduleId) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() == Role.CLIENT_USER) {
            throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER is not authorized to delete modules");
        }

        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Module not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(module.getProject().getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN can only delete modules in their own organization");
            }
        }

        moduleRepository.delete(module);
        return new GenericResponse("Module deleted successfully");
    }

    private User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }
}
