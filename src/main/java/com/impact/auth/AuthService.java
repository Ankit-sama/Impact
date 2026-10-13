package com.impact.auth;

import com.impact.auth.dto.RegisterRequest;
import com.impact.common.Attachment;
import com.impact.config.AppClock;
import com.impact.config.ImpactProperties;
import com.impact.organization.OrgStatus;
import com.impact.organization.Organization;
import com.impact.organization.OrganizationRepository;
import com.impact.user.Role;
import com.impact.user.User;
import com.impact.user.UserRepository;
import com.impact.user.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OtpRecordRepository otpRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpSender otpSender;
    private final ImpactProperties properties;
    private final AppClock appClock;

    public AuthService(UserRepository userRepository, OrganizationRepository organizationRepository,
                       OtpRecordRepository otpRecordRepository, PasswordEncoder passwordEncoder,
                       OtpSender otpSender, ImpactProperties properties, AppClock appClock) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.otpRecordRepository = otpRecordRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpSender = otpSender;
        this.properties = properties;
        this.appClock = appClock;
    }

    @Transactional
    public void registerUser(RegisterRequest request) throws Exception {
        if (request.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Registration as ADMIN is not permitted.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (request.getRole() == Role.ROLE_ORG_MANAGER && (request.getOrgName() == null || request.getOrgName().isBlank())) {
            throw new IllegalArgumentException("Organization name is required for managers");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setCity(request.getCity());
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setCreatedAt(appClock.now());

        if (request.getPhoto() != null && !request.getPhoto().isEmpty()) {
            Attachment photo = new Attachment();
            photo.setOwnerType("USER");
            photo.setMimeType(request.getPhoto().getContentType());
            photo.setSize(request.getPhoto().getSize());
            photo.setLabel("PROFILE_PHOTO");

            Path uploadDir = Paths.get(properties.getUpload().getDir());
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path filePath = uploadDir.resolve(photo.getStoragePath() + ".img");
            request.getPhoto().transferTo(filePath.toAbsolutePath().toFile());
            user.setPhoto(photo);
        }

        userRepository.save(user);

        if (request.getRole() == Role.ROLE_ORG_MANAGER) {
            Organization org = new Organization();
            org.setManagerId(user.getId());
            org.setName(request.getOrgName());
            org.setCity(request.getCity());
            org.setStatus(OrgStatus.DRAFT);
            org.setCorrectionRounds(0);
            organizationRepository.save(org);
        }
    }

    @Transactional
    public String generateOtp(Long userId, String phone) {
        // Only reject if phone belongs to ANOTHER user
        if (userRepository.existsByPhoneAndIdNot(phone, userId)) {
            throw new IllegalArgumentException("Phone number is already in use by another account");
        }
        String otpCode = otpSender.sendOtp(phone);
        
        OtpRecord record = new OtpRecord();
        record.setUserId(userId);
        record.setPhone(phone); // Saved temporarily here
        record.setCodeHash(passwordEncoder.encode(otpCode));
        record.setCreatedAt(appClock.now());
        otpRecordRepository.save(record);
        
        return otpCode;
    }

    @Transactional
    public boolean verifyOtp(Long userId, String code) {
        Optional<OtpRecord> recordOpt = otpRecordRepository.findTopByUserIdOrderByCreatedAtDesc(userId);
        if (recordOpt.isEmpty()) {
            return false;
        }
        OtpRecord record = recordOpt.get();
        if (passwordEncoder.matches(code, record.getCodeHash())) {
            // Check uniqueness again just in case another transaction sneaked in
            if (userRepository.existsByPhoneAndIdNot(record.getPhone(), userId)) {
                throw new IllegalStateException("Phone number was claimed by another user while verifying");
            }
            User user = userRepository.findById(userId).orElseThrow();
            user.setPhone(record.getPhone());
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public String getLastPhoneForUser(Long userId) {
        return otpRecordRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(OtpRecord::getPhone)
                .orElse(null);
    }
}