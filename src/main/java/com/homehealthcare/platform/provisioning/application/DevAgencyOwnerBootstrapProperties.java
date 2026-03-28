package com.homehealthcare.platform.provisioning.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.dev-bootstrap.owner")
public class DevAgencyOwnerBootstrapProperties {

    private boolean enabled;
    private String internalSuperAdminFirstName = "Platform";
    private String internalSuperAdminLastName = "Operator";
    private String internalSuperAdminEmail;
    private String agencyName;
    private String agencySlug;
    private String agencyTimezone = "America/Chicago";
    private String agencyContactEmail;
    private String ownerFirstName;
    private String ownerLastName;
    private String ownerEmail;
    private String ownerPhone;
    private String ownerPassword;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getInternalSuperAdminFirstName() {
        return internalSuperAdminFirstName;
    }

    public void setInternalSuperAdminFirstName(String internalSuperAdminFirstName) {
        this.internalSuperAdminFirstName = internalSuperAdminFirstName;
    }

    public String getInternalSuperAdminLastName() {
        return internalSuperAdminLastName;
    }

    public void setInternalSuperAdminLastName(String internalSuperAdminLastName) {
        this.internalSuperAdminLastName = internalSuperAdminLastName;
    }

    public String getInternalSuperAdminEmail() {
        return internalSuperAdminEmail;
    }

    public void setInternalSuperAdminEmail(String internalSuperAdminEmail) {
        this.internalSuperAdminEmail = internalSuperAdminEmail;
    }

    public String getAgencyName() {
        return agencyName;
    }

    public void setAgencyName(String agencyName) {
        this.agencyName = agencyName;
    }

    public String getAgencySlug() {
        return agencySlug;
    }

    public void setAgencySlug(String agencySlug) {
        this.agencySlug = agencySlug;
    }

    public String getAgencyTimezone() {
        return agencyTimezone;
    }

    public void setAgencyTimezone(String agencyTimezone) {
        this.agencyTimezone = agencyTimezone;
    }

    public String getAgencyContactEmail() {
        return agencyContactEmail;
    }

    public void setAgencyContactEmail(String agencyContactEmail) {
        this.agencyContactEmail = agencyContactEmail;
    }

    public String getOwnerFirstName() {
        return ownerFirstName;
    }

    public void setOwnerFirstName(String ownerFirstName) {
        this.ownerFirstName = ownerFirstName;
    }

    public String getOwnerLastName() {
        return ownerLastName;
    }

    public void setOwnerLastName(String ownerLastName) {
        this.ownerLastName = ownerLastName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getOwnerPhone() {
        return ownerPhone;
    }

    public void setOwnerPhone(String ownerPhone) {
        this.ownerPhone = ownerPhone;
    }

    public String getOwnerPassword() {
        return ownerPassword;
    }

    public void setOwnerPassword(String ownerPassword) {
        this.ownerPassword = ownerPassword;
    }
}
