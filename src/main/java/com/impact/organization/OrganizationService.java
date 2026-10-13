package com.impact.organization;

import com.impact.admin.ActivityLogService;
import com.impact.config.AppClock;
import com.impact.config.ImpactProperties;
import com.impact.notification.NotificationService;
import com.impact.organization.dto.OrganizationDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AppClock appClock;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final ImpactProperties impactProperties;

    public OrganizationService(OrganizationRepository organizationRepository, AppClock appClock,
                               ActivityLogService activityLogService, NotificationService notificationService,
                               ImpactProperties impactProperties) {
        this.organizationRepository = organizationRepository;
        this.appClock = appClock;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.impactProperties = impactProperties;
    }

    public Organization getOrganizationByManagerId(Long managerId) {
        return organizationRepository.findByManagerId(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
    }

    public Optional<Organization> getOrganizationById(Long orgId) {
        return organizationRepository.findById(orgId);
    }

    public List<Organization> getPendingOrganizations() {
        return organizationRepository.findAllByStatusOrderBySubmittedAtAsc(OrgStatus.PENDING_VERIFICATION);
    }

    public List<Organization> getRejectedOrganizations() {
        return organizationRepository.findAllByStatusOrderBySubmittedAtAsc(OrgStatus.REJECTED);
    }

    @Transactional
    public void updateOrganization(Long managerId, OrganizationDto dto) {
        Organization org = getOrganizationByManagerId(managerId);
        if (org.getStatus() != OrgStatus.DRAFT && org.getStatus() != OrgStatus.RETURNED_FOR_CORRECTION) {
            throw new IllegalStateException("Organization can only be edited when Draft or Returned for Correction.");
        }
        org.setName(dto.getName());
        org.setCauses(dto.getCauses());
        org.setCity(dto.getCity());
        org.setDescription(dto.getDescription());
        org.setContactEmail(dto.getContactEmail());
        org.setRegistrationNumber(dto.getRegistrationNumber());
        organizationRepository.save(org);
    }

    @Transactional
    public void submitForVerification(Long managerId) {
        Organization org = getOrganizationByManagerId(managerId);
        if (org.getStatus() != OrgStatus.DRAFT && org.getStatus() != OrgStatus.RETURNED_FOR_CORRECTION) {
            throw new IllegalStateException("Organization can only be submitted when Draft or Returned for Correction.");
        }
        if (org.getCauses() == null || org.getDescription() == null || org.getContactEmail() == null || org.getRegistrationNumber() == null) {
            throw new IllegalStateException("All fields must be filled to submit.");
        }
        org.setStatus(OrgStatus.PENDING_VERIFICATION);
        org.setSubmittedAt(appClock.now());
        organizationRepository.save(org);
        
        notificationService.notifyAdmins("'" + org.getName() + "' was submitted for verification.", "/admin/organization/" + org.getId() + "/review");
    }

    @Transactional
    public void reviewOrganization(Long adminId, Long orgId, OrgStatus decision, String reason) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        if (org.getStatus() != OrgStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Organization is not pending verification.");
        }

        if ((decision == OrgStatus.REJECTED || decision == OrgStatus.RETURNED_FOR_CORRECTION) && (reason == null || reason.trim().isEmpty())) {
            throw new IllegalArgumentException("Reason is required for Rejection or Return.");
        }

        int maxRounds = impactProperties.getOrganization().getMaxCorrectionRounds();

        if (decision == OrgStatus.RETURNED_FOR_CORRECTION) {
            if (org.getCorrectionRounds() >= maxRounds) {
                throw new IllegalStateException("Maximum correction rounds reached.");
            }
            org.setCorrectionRounds(org.getCorrectionRounds() + 1);
        }

        org.setStatus(decision);
        org.setReviewReason(reason);
        org.setReviewedBy(adminId);
        org.setReviewedAt(appClock.now());
        organizationRepository.save(org);

        String detail = "Decision: " + decision.name();
        if (reason != null && !reason.isBlank()) {
            detail += " | Reason: " + reason;
        }
        activityLogService.logAction(adminId, "ORGANIZATION_REVIEWED", "ORGANIZATION", orgId, detail);

        String msg;
        if (decision == OrgStatus.VERIFIED) {
            msg = "Your organization '" + org.getName() + "' has been approved.";
        } else if (decision == OrgStatus.REJECTED) {
            msg = "Your organization '" + org.getName() + "' was rejected. This decision is final. Reason: " + reason;
        } else {
            msg = "Your organization '" + org.getName() + "' was returned for correction (round " + org.getCorrectionRounds() + " of " + maxRounds + "). Reason: " + reason;
        }
        notificationService.createNotification(org.getManagerId(), msg, "/manager/dashboard");
    }

    @Transactional
    public void reopenOrganization(Long adminId, Long orgId, String reason) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        if (org.getStatus() != OrgStatus.REJECTED) {
            throw new IllegalStateException("Only rejected organizations can be reopened.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason is required to reopen.");
        }

        org.setStatus(OrgStatus.RETURNED_FOR_CORRECTION);
        // Note: Reopening does not increment correctionRounds
        organizationRepository.save(org);

        activityLogService.logAction(adminId, "ORGANIZATION_REOPENED", "ORGANIZATION", orgId, "Reason: " + reason);
        
        notificationService.createNotification(org.getManagerId(), 
                "Your organization '" + org.getName() + "' was reopened for correction. Reason: " + reason, 
                "/manager/dashboard");
    }
}