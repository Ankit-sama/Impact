package com.impact.admin;

import com.impact.auth.OtpRecord;
import com.impact.auth.OtpRecordRepository;
import com.impact.common.Attachment;
import com.impact.common.AttachmentRepository;
import com.impact.config.AppClock;
import com.impact.event.*;
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

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Profile("dev")
public class DemoDataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final OtpRecordRepository otpRecordRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AttachmentRepository attachmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppClock appClock;

    public DemoDataSeeder(UserRepository userRepository, OrganizationRepository organizationRepository,
                          EventRepository eventRepository, RegistrationRepository registrationRepository,
                          OtpRecordRepository otpRecordRepository, NotificationRepository notificationRepository, 
                          ActivityLogRepository activityLogRepository, AttachmentRepository attachmentRepository, 
                          PasswordEncoder passwordEncoder, AppClock appClock) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
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
            // FIX: Reset the clock offset to zero FIRST, so all seeded events generate precise real-world start times.
            appClock.reset();

            List<User> demoUsers = userRepository.findByEmailEndingWithIgnoreCase("@demo.com");
            logger.info("Found {} demo users", demoUsers.size());
            
            List<Long> demoUserIds = demoUsers.stream().map(User::getId).collect(Collectors.toList());

            if (!demoUserIds.isEmpty()) {
                List<Organization> demoOrgs = organizationRepository.findAll().stream()
                        .filter(org -> demoUserIds.contains(org.getManagerId()))
                        .collect(Collectors.toList());
                
                List<Long> demoOrgIds = demoOrgs.stream().map(Organization::getId).collect(Collectors.toList());

                List<Event> demoEvents = eventRepository.findAll().stream()
                        .filter(e -> demoUserIds.contains(e.getCreatorId()))
                        .collect(Collectors.toList());
                List<Long> demoEventIds = demoEvents.stream().map(Event::getId).collect(Collectors.toList());

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

                List<Registration> regsToDelete = registrationRepository.findAll().stream()
                        .filter(r -> demoUserIds.contains(r.getUserId()) || demoEventIds.contains(r.getEventId()))
                        .collect(Collectors.toList());

                notificationRepository.deleteAllInBatch(notesToDelete);
                activityLogRepository.deleteAllInBatch(logsToDelete);
                otpRecordRepository.deleteAllInBatch(otpsToDelete);
                registrationRepository.deleteAllInBatch(regsToDelete);
                eventRepository.deleteAllInBatch(demoEvents);
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
                eventRepository.flush();
                registrationRepository.flush();
            }

            String passHash = passwordEncoder.encode("Password1");
            User orgManager1 = null;
            User volunteer2 = null;

            if (!userRepository.existsByEmailIgnoreCase("admin@demo.com")) {
                User admin = createUser("Admin User", "admin@demo.com", passHash, Role.ROLE_ADMIN, "9999999999");
                userRepository.save(admin);
            }

            if (!userRepository.existsByEmailIgnoreCase("vol1@demo.com")) {
                User vol1 = createUser("Alice Volunteer", "vol1@demo.com", passHash, Role.ROLE_VOLUNTEER, "9999999991");
                userRepository.save(vol1);
            }

            if (!userRepository.existsByEmailIgnoreCase("vol2@demo.com")) {
                volunteer2 = createUser("Bob Volunteer", "vol2@demo.com", passHash, Role.ROLE_VOLUNTEER, "9999999992");
                userRepository.save(volunteer2);
            }

            if (!userRepository.existsByEmailIgnoreCase("org1@demo.com")) {
                orgManager1 = createUser("Manager One", "org1@demo.com", passHash, Role.ROLE_ORG_MANAGER, "9999999993");
                userRepository.save(orgManager1);
                
                Organization org1 = new Organization();
                org1.setManagerId(orgManager1.getId());
                org1.setName("Green Earth Foundation");
                org1.setCauses("Environment, Tree Plantation");
                org1.setCity("Pune");
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
                org2.setCity("Mumbai");
                org2.setDescription("Helping urban poor.");
                org2.setContactEmail("hello@urbanhelpers.demo.com");
                org2.setRegistrationNumber("REG-98765");
                org2.setStatus(OrgStatus.PENDING_VERIFICATION);
                org2.setCorrectionRounds(0);
                org2.setSubmittedAt(appClock.now()); 
                organizationRepository.save(org2);
            }

            if (orgManager1 == null) orgManager1 = userRepository.findByEmail("org1@demo.com").orElse(null);
            if (volunteer2 == null) volunteer2 = userRepository.findByEmail("vol2@demo.com").orElse(null);

            if (orgManager1 != null) {
                Organization org1 = organizationRepository.findByManagerId(orgManager1.getId()).orElse(null);
                
                if (org1 != null && eventRepository.findByCreatorIdOrderByCreatedAtDesc(orgManager1.getId()).isEmpty()) {
                    Event e1 = new Event();
                    e1.setCreatorId(orgManager1.getId());
                    e1.setOrganizationId(org1.getId());
                    e1.setTitle("Park Cleanup Drive");
                    e1.setCategory(EventCategory.CLEANLINESS_DRIVE);
                    e1.setPurpose("Clean up the central park area.");
                    e1.setStartAt(appClock.now().plus(2, ChronoUnit.DAYS));
                    e1.setEndAt(appClock.now().plus(2, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS));
                    e1.setExpectedHours(4);
                    e1.setState("Maharashtra");
                    e1.setCity("Pune");
                    e1.setAddress("Central Park");
                    e1.setCapacity(10);
                    e1.setStatus(EventStatus.PUBLISHED);
                    e1.setPublishedAt(appClock.now());
                    e1.setCreatedAt(appClock.now());
                    eventRepository.save(e1);

                    Event e2 = new Event();
                    e2.setCreatorId(orgManager1.getId());
                    e2.setOrganizationId(org1.getId());
                    e2.setTitle("Tree Plantation Day");
                    e2.setCategory(EventCategory.ENVIRONMENT);
                    e2.setPurpose("Plant 100 trees in the suburbs.");
                    e2.setStartAt(appClock.now().plus(5, ChronoUnit.DAYS));
                    e2.setEndAt(appClock.now().plus(5, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS));
                    e2.setExpectedHours(6);
                    e2.setState("Maharashtra");
                    e2.setCity("Pune");
                    e2.setCapacity(5);
                    e2.setStatus(EventStatus.PUBLISHED);
                    e2.setPublishedAt(appClock.now());
                    e2.setCreatedAt(appClock.now());
                    eventRepository.save(e2);
                    
                    Event e3 = new Event();
                    e3.setCreatorId(orgManager1.getId());
                    e3.setOrganizationId(org1.getId());
                    e3.setTitle("Beach Cleanup (low volunteers demo)");
                    e3.setCategory(EventCategory.AWARENESS_CAMPAIGN);
                    e3.setPurpose("Spread awareness by cleaning the beach.");
                    e3.setStartAt(appClock.now().plus(6, ChronoUnit.MINUTES));
                    e3.setEndAt(appClock.now().plus(6, ChronoUnit.MINUTES).plus(3, ChronoUnit.HOURS));
                    e3.setExpectedHours(2);
                    e3.setState("Maharashtra");
                    e3.setCity("Pune");
                    e3.setCapacity(5);
                    e3.setStatus(EventStatus.PUBLISHED);
                    e3.setPublishedAt(appClock.now());
                    e3.setCreatedAt(appClock.now());
                    eventRepository.save(e3);

                    Event e4 = new Event();
                    e4.setCreatorId(orgManager1.getId());
                    e4.setOrganizationId(org1.getId());
                    e4.setTitle("Library Drive (no volunteers demo)");
                    e4.setCategory(EventCategory.EDUCATIONAL);
                    e4.setPurpose("Organize library books for the community.");
                    e4.setStartAt(appClock.now().plus(6, ChronoUnit.MINUTES));
                    e4.setEndAt(appClock.now().plus(6, ChronoUnit.MINUTES).plus(3, ChronoUnit.HOURS));
                    e4.setExpectedHours(2);
                    e4.setState("Maharashtra");
                    e4.setCity("Pune");
                    e4.setCapacity(5);
                    e4.setStatus(EventStatus.PUBLISHED);
                    e4.setPublishedAt(appClock.now());
                    e4.setCreatedAt(appClock.now());
                    eventRepository.save(e4);

                    if (volunteer2 != null) {
                        Registration reg1 = new Registration();
                        reg1.setEventId(e1.getId());
                        reg1.setUserId(volunteer2.getId());
                        reg1.setStatus(RegistrationStatus.CONFIRMED);
                        reg1.setRegisteredAt(appClock.now());
                        registrationRepository.save(reg1);
                        
                        Registration reg3 = new Registration();
                        reg3.setEventId(e3.getId());
                        reg3.setUserId(volunteer2.getId());
                        reg3.setStatus(RegistrationStatus.CONFIRMED);
                        reg3.setRegisteredAt(appClock.now());
                        registrationRepository.save(reg3);
                    }
                }
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