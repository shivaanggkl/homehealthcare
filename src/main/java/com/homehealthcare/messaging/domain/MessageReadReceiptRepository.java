package com.homehealthcare.messaging.domain;

import com.homehealthcare.messaging.foundation.MessagingDeliveryState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReadReceiptRepository extends JpaRepository<MessageReadReceipt, UUID> {

    Optional<MessageReadReceipt> findByMessage_IdAndRecipientMembership_Id(UUID messageId, UUID recipientMembershipId);

    List<MessageReadReceipt> findAllByMessage_IdOrderByRecipientMembership_IdAsc(UUID messageId);

    List<MessageReadReceipt> findAllByRecipientMembership_IdAndDeliveryStateNot(
            UUID recipientMembershipId,
            MessagingDeliveryState deliveryState);

    long countByRecipientMembership_IdAndDeliveryStateNot(UUID recipientMembershipId, MessagingDeliveryState deliveryState);
}
