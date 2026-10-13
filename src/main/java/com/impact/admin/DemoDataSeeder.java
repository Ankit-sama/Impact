package com.impact.admin;

import com.impact.auth.OtpRecord;
import com.impact.auth.OtpRecordRepository;
import com.impact.common.Attachment;
import com.impact.common.AttachmentRepository;
import com.impact.config.AppClock;
import com.impact.notification.Notification;
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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Profile("dev")
public class DemoDataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OtpRecordRepository otpRecordRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AttachmentRepository attachmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppClock appClock;

    public DemoDataSeeder(UserRepository userRepository, OrganizationRepository organizationRepository,
                          OtpRecordRepository otpRecordRepository, NotificationRepository notificationRepository,
                          ActivityLogRepository activityLogRepository, AttachmentRepository attachmentRepository, 
                          PasswordEncoder passwordEncoder, AppClock appClock) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.otpRecordRepository = otpRecordRepository;
        this.notificationRepository = notificationRepository;
        this.activityLogRepository = activityLogRepository;
        this.attachmentRepository = attachmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.appClock = appClock;
    }

    @Transactional
    public void resetAndSeed() {
        try {
            List<User> demoUsers = userRepository.findByEmailEndingWithIgnoreCase("@demo.com");
            logger.info("Found {} demo users", demoUsers.size());
            
            List<Long> demoUserIds = demoUsers.stream().map(User::getId).collect(Collectors.toList());

            if (!demoUserIds.isEmpty()) {
                List<Organization> demoOrgs = organizationRepository.findAll().stream()
                        .filter(org -> demoUserIds.contains(org.getManagerId()))
                        .collect(Collectors.toList());
                
                logger.info("Found {} demo organizations", demoOrgs.size());
                
                List<Long> demoOrgIds = demoOrgs.stream().map(Organization::getId).collect(Collectors.toList());

                List<ActivityLog> logsToDelete = activityLogRepository.findAll().stream()
                        .filter(log -> demoUserIds.contains(log.getActorId()) || 
                                ("USER".equals(log.getTargetType()) && demoUserIds.contains(log.getTargetId())) ||
                                ("ORGANIZATION".equals(log.getTargetType()) && demoOrgIds.contains(log.getTargetId())))
                        .collect(Collectors.toList());
                
                List<Notification> notesToDelete = notificationRepository.findAll().stream()
                        .filter(n -> demoUserIds.contains(n.getUserId()))
                        .collect(Collectors.toList());
                
                List<OtpRecord> otpsToDelete = otpRecordRepository.findAll().stream()
                        .filter(o -> demoUserIds.contains(o.getUserId()))
                        .collect(Collectors.toList());

                notificationRepository.deleteAllInBatch(notesToDelete);
                activityLogRepository.deleteAllInBatch(logsToDelete);
                otpRecordRepository.deleteAllInBatch(otpsToDelete);
                organizationRepository.deleteAllInBatch(demoOrgs);

                List<Attachment> attachmentsToDelete = demoUsers.stream()
                        .map(User::getPhoto).filter(Objects::nonNull).collect(Collectors.toList());
                
                for (User u : demoUsers) { u.setPhoto(null); }
                userRepository.saveAllAndFlush(demoUsers);
                
                if (!attachmentsToDelete.isEmpty()) {
                    attachmentRepository.deleteAllInBatch(attachmentsToDelete);
                }
                
                userRepository.deleteAllInBatch(demoUsers);

                userRepository.flush();
                organizationRepository.flush();
            }

            String passHash = passwordEncoder.encode("Password1");

            if (!userRepository.existsByEmailIgnoreCase("admin@demo.com")) {
                User admin = createUser("Admin User", "admin@demo.com", passHash, Role.ROLE_ADMIN, "9999999999");
                userRepository.save(admin);
            }

            if (!userRepository.existsByEmailIgnoreCase("vol1@demo.com")) {
                User vol1 = createUser("Alice Volunteer", "vol1@demo.com", passHash, Role.ROLE_VOLUNTEER, "9999999991");
                userRepository.save(vol1);
            }

            if (!userRepository.existsByEmailIgnoreCase("vol2@demo.com")) {
                User vol2 = createUser("Bob Volunteer", "vol2@demo.com", passHash, Role.ROLE_VOLUNTEER, "9999999992");
                userRepository.save(vol2);
            }

            if (!userRepository.existsByEmailIgnoreCase("org1@demo.com")) {
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
                org1.setCorrectionRounds(0);
                org1.setSubmittedAt(appClock.now().minus(java.time.Duration.ofDays(1))); 
                organizationRepository.save(org1);
            }

            if (!userRepository.existsByEmailIgnoreCase("org2@demo.com")) {
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
                org2.setCorrectionRounds(0);
                org2.setSubmittedAt(appClock.now()); 
                organizationRepository.save(org2);
            }

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