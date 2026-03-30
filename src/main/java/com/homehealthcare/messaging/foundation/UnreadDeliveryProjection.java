package com.homehealthcare.messaging.foundation;

public record UnreadDeliveryProjection(
        long unreadThreadCount,
        long unreadMessageCount,
        long escalatedThreadCount,
        long activeBroadcastCount) {
}
