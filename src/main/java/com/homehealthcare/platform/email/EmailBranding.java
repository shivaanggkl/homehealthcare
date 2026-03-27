package com.homehealthcare.platform.email;

public record EmailBranding(
        String platformName,
        String agencyName,
        String supportEmail,
        String primaryColor,
        String logoUrl) {

    static EmailBranding defaultBranding(SystemEmailProperties properties, String agencyName) {
        return new EmailBranding(
                properties.getPlatformName(),
                agencyName,
                properties.getSupportEmail(),
                properties.getPrimaryColor(),
                properties.getLogoUrl());
    }
}
