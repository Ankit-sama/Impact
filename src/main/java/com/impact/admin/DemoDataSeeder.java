package com.impact.admin;

import com.impact.auth.OtpRecordRepository;
import com.impact.config.AppClock;
import com.impact.notification.NotificationRepository;
import com.impact.organization.OrgStatus;
import com.impact.organization.Organization;
import com.impact.organization.OrganizationRepository;
import com.impact.user.Role;
import com.impact.user.User;
import com.impact.user.UserRepository;
import com.impact.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("dev")
public class DemoDataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OtpRecordRepository otpRecordRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppClock appClock;

    public DemoDataSeeder(UserRepository userRepository, OrganizationRepository organizationRepository,
                          OtpRecordRepository otpRecordRepository, NotificationRepository notificationRepository,
                          ActivityLogRepository activityLogRepository, PasswordEncoder passwordEncoder, 
                          AppClock appClock) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.otpRecordRepository = otpRecordRepository;
        this.notificationRepository = notificationRepository;
        this.activityLogRepository = activityLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.appClock = appClock;
    }

    @Transactional
    public void resetAndSeed() {
        try {
            // Tables to wipe (Stage 2): ActivityLog, Notification, OtpRecord, Organization, User (cascades Attachment)
            // Note: Extend this list as new tables are added in later stages.
            activityLogRepository.deleteAll();
            notificationRepository.deleteAll();
            otpRecordRepository.deleteAll();
            organizationRepository.deleteAll();
            userRepository.deleteAll();
            
            appClock.reset();

            String passHash = passwordEncoder.encode("Password1");

            User admin = createUser("Admin User", "admin@demo.com", passHash, Role.ROLE_ADMIN, "9999999999");
            userRepository.save(admin);

            User vol1 = createUser("Alice Volunteer", "vol1@demo.com", passHash, Role.ROLE_VOLUNTEER, "9999999991");
            userRepository.save(vol1);

            User vol2 = createUser("Bob Volunteer", "vol2@demo.com", passHash, Role.ROLE_VOLUNTEER, "9999999992");
            userRepository.save(vol2);

            User orgManager1 = createUser("Manager One", "org1@demo.com", passHash, Role.ROLE_ORG_MANAGER, "9999999993");
            userRepository.save(orgManager1);
            Organization org1 = new Organization();
            org1.setManagerId(orgManager1.getId());
            org1.setName("Green Earth Foundation");
            org1.setCauses("Environment, Tree Plantation");
            org1.setCity("Mumbai");
            org1.setDescription("Planting trees since 2010.");
            org1.setContactEmail("contact@greenearth.demo.com");
            org1.setRegistrationNumber("REG-12345");
            org1.setStatus(OrgStatus.VERIFIED);
            org1.setSubmittedAt(appClock.now().minus(java.time.Duration.ofDays(1))); 
            organizationRepository.save(org1);

            User orgManager2 = createUser("Manager Two", "org2@demo.com", passHash, Role.ROLE_ORG_MANAGER, "9999999994");
            userRepository.save(orgManager2);
            Organization org2 = new Organization();
            org2.setManagerId(orgManager2.getId());
            org2.setName("Urban Helpers");
            org2.setCauses("Cleanliness, Education");
            org2.setCity("Pune");
            org2.setDescription("Helping urban poor.");
            org2.setContactEmail("hello@urbanhelpers.demo.com");
            org2.setRegistrationNumber("REG-98765");
            org2.setStatus(OrgStatus.PENDING_VERIFICATION);
            org2.setSubmittedAt(appClock.now()); 
            organizationRepository.save(org2);

        } catch (Exception e) {
            logger.error("Failed to reset and seed demo data. Transaction rolled back.", e);
            throw e;
        }
    }

    private User createUser(String name, String email, String passHash, Role role, String phone) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(passHash);
        u.setRole(role);
        u.setCity("Demo City");
        u.setPhone(phone);
        u.setStatus(UserStatus.ACTIVE);
        u.setCreatedAt(appClock.now());
        return u;
    }
}