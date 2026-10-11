package com.impact.admin;

import com.impact.config.AppClock;
import com.impact.organization.OrgStatus;
import com.impact.organization.Organization;
import com.impact.organization.OrganizationRepository;
import com.impact.user.Role;
import com.impact.user.User;
import com.impact.user.UserRepository;
import com.impact.user.UserStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Profile("dev")
public class DemoDataSeeder {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppClock appClock;

    public DemoDataSeeder(UserRepository userRepository, OrganizationRepository organizationRepository,
                          PasswordEncoder passwordEncoder, AppClock appClock) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.appClock = appClock;
    }

    @Transactional
    public void resetAndSeed() {
        // 1. Delete old demo data based on email pattern (preserves manual accounts)
        List<User> demoUsers = userRepository.findAll().stream()
                .filter(u -> u.getEmail().endsWith("@demo.com")).toList();
        
        for (User u : demoUsers) {
            organizationRepository.findByManagerId(u.getId()).ifPresent(organizationRepository::delete);
            userRepository.delete(u);
        }

        appClock.reset();

        // 2. Create standard password
        String passHash = passwordEncoder.encode("Password1");

        // 3. Seed Admin
        User admin = createUser("Admin User", "admin@demo.com", passHash, Role.ROLE_ADMIN, "9999999999");
        userRepository.save(admin);

        // 4. Seed Volunteers
        User vol1 = createUser("Alice Volunteer", "vol1@demo.com", passHash, Role.ROLE_VOLUNTEER, "9999999991");
        userRepository.save(vol1);

        User vol2 = createUser("Bob Volunteer", "vol2@demo.com", passHash, Role.ROLE_VOLUNTEER, "9999999992");
        userRepository.save(vol2);

        // 5. Seed Verified Organization
        User orgManager1 = createUser("Manager One", "org1@demo.com", passHash, Role.ROLE_ORG_MANAGER, "9999999993");
        userRepository.save(orgManager1);
        Organization org1 = new Organization();
        org1.setManagerId(orgManager1.getId());
        org1.setName("Green Earth Foundation");
        org1.setCity("Mumbai");
        org1.setStatus(OrgStatus.VERIFIED);
        organizationRepository.save(org1);

        // 6. Seed Pending Organization
        User orgManager2 = createUser("Manager Two", "org2@demo.com", passHash, Role.ROLE_ORG_MANAGER, "9999999994");
        userRepository.save(orgManager2);
        Organization org2 = new Organization();
        org2.setManagerId(orgManager2.getId());
        org2.setName("Urban Helpers");
        org2.setCity("Pune");
        org2.setStatus(OrgStatus.PENDING_VERIFICATION);
        organizationRepository.save(org2);
    }

    private User createUser(String name, String email, String passHash, Role role, String phone) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(passHash);
        u.setRole(role);
        u.setCity("Demo City");
        u.setPhone(phone);
        u.setStatus(UserStatus.ACTIVE); // Skip OTP phase
        u.setCreatedAt(appClock.now());
        return u;
    }
}