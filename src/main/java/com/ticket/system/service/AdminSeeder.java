package com.ticket.system.service;

import com.ticket.system.entity.ClientOrganization;
import com.ticket.system.entity.Role;
import com.ticket.system.entity.User;
import com.ticket.system.repository.ClientOrganizationRepository;
import com.ticket.system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class AdminSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final ClientOrganizationRepository orgRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.admin.email:admin@example.com}")
    private String adminEmail;

    @Value("${seed.admin.password:Admin@12345}")
    private String adminPassword;

    public AdminSeeder(UserRepository userRepository,
                       ClientOrganizationRepository orgRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.orgRepository = orgRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Seed default APP_ADMIN if absent
        if (!userRepository.existsByEmailIgnoreCase(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setFirstName("System");
            admin.setLastName("Admin");
            admin.setDesignation("System Administrator");
            admin.setOffice("Headquarters");
            admin.setRole(Role.APP_ADMIN);
            admin.setOrganization(null);
            admin.setActive(true);

            userRepository.save(admin);
            logger.info("========== DEVELOPMENT SEEDER ==========");
            logger.info("Created default APP_ADMIN user: {}", adminEmail);
        }

        // Seed demo Acme Corporation organization if absent
        ClientOrganization acmeOrg = orgRepository.findByCodeIgnoreCase("ACME")
                .orElseGet(() -> {
                    ClientOrganization org = new ClientOrganization("Acme Corporation", "ACME", "Default demo client organization");
                    ClientOrganization saved = orgRepository.save(org);
                    logger.info("Created demo organization: Acme Corporation (ACME)");
                    return saved;
                });

        // Seed demo CLIENT_ADMIN if absent
        String clientAdminEmail = "clientadmin@acme.com";
        if (!userRepository.existsByEmailIgnoreCase(clientAdminEmail)) {
            User clientAdmin = new User();
            clientAdmin.setEmail(clientAdminEmail);
            clientAdmin.setPasswordHash(passwordEncoder.encode("ClientAdmin@12345"));
            clientAdmin.setFirstName("Acme");
            clientAdmin.setLastName("Admin");
            clientAdmin.setDesignation("IT Manager");
            clientAdmin.setOffice("Acme HQ");
            clientAdmin.setRole(Role.CLIENT_ADMIN);
            clientAdmin.setOrganization(acmeOrg);
            clientAdmin.setActive(true);

            userRepository.save(clientAdmin);
            logger.info("Created demo CLIENT_ADMIN user: {}", clientAdminEmail);
        }

        // Seed demo CLIENT_USER if absent
        String clientUserEmail = "clientuser@acme.com";
        if (!userRepository.existsByEmailIgnoreCase(clientUserEmail)) {
            User clientUser = new User();
            clientUser.setEmail(clientUserEmail);
            clientUser.setPasswordHash(passwordEncoder.encode("ClientUser@12345"));
            clientUser.setFirstName("Acme");
            clientUser.setLastName("User");
            clientUser.setDesignation("Software Engineer");
            clientUser.setOffice("Acme HQ");
            clientUser.setRole(Role.CLIENT_USER);
            clientUser.setOrganization(acmeOrg);
            clientUser.setActive(true);

            userRepository.save(clientUser);
            logger.info("Created demo CLIENT_USER user: {}", clientUserEmail);
            logger.info("=========================================");
        }
    }
}
