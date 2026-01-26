package com.neuralhealer.backend.service;

import com.neuralhealer.backend.exception.UnauthorizedException;
import com.neuralhealer.backend.model.dto.MessageResponse;
import com.neuralhealer.backend.model.dto.SendMessageRequest;
import com.neuralhealer.backend.model.entity.Engagement;
import com.neuralhealer.backend.model.entity.EngagementMessage;
import com.neuralhealer.backend.model.entity.User;
import com.neuralhealer.backend.notification.service.NotificationCreatorService;
import com.neuralhealer.backend.repository.EngagementMessageRepository;
import com.neuralhealer.backend.repository.EngagementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EngagementMessageService {

        private final EngagementMessageRepository messageRepository;
        private final EngagementRepository engagementRepository;
        private final NotificationCreatorService notificationCreatorService;

        @Transactional
        public MessageResponse sendMessage(User sender, UUID engagementId, SendMessageRequest request) {
                Engagement engagement = engagementRepository.findById(engagementId)
                                .orElseThrow(() -> new UnauthorizedException("Engagement not found or access denied"));

                boolean isDoctor = engagement.getDoctor().getUser().getId().equals(sender.getId());
                boolean isPatient = engagement.getPatient().getUser().getId().equals(sender.getId());

                if (!isDoctor && !isPatient) {
                        throw new UnauthorizedException("User is not part of this engagement");
                }

                User recipient = isDoctor ? engagement.getPatient().getUser() : engagement.getDoctor().getUser();

                EngagementMessage message = EngagementMessage.builder()
                                .engagement(engagement)
                                .sender(sender)
                                .recipient(recipient)
                                .content(request.content())
                                .isSystemMessage(false)
                                .sentAt(LocalDateTime.now())
                                .build();

                message = messageRepository.save(message);

                notificationCreatorService.createMessageNotification(
                                recipient.getId(),
                                sender.getId(),
                                sender.getFullName(),
                                request.content());

                return mapToResponse(message);
        }

        @Transactional(readOnly = true)
        public List<MessageResponse> getMessages(User user, UUID engagementId) {
                Engagement engagement = engagementRepository.findById(engagementId)
                                .orElseThrow(() -> new UnauthorizedException("Engagement not found"));

                if (!engagement.getDoctor().getUser().getId().equals(user.getId())
                                && !engagement.getPatient().getUser().getId().equals(user.getId())) {
                        throw new UnauthorizedException("Not authorized to view messages");
                }

                return messageRepository.findByEngagementIdOrderBySentAtAsc(engagementId).stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public void sendSystemMessage(Engagement engagement, String content) {
                EngagementMessage message = EngagementMessage.builder()
                                .engagement(engagement)
                                .sender(null) // null sender indicates system message
                                .recipient(null)
                                .content(content)
                                .isSystemMessage(true)
                                .sentAt(LocalDateTime.now())
                                .build();

                messageRepository.save(message);
        }

        private MessageResponse mapToResponse(EngagementMessage msg) {
                return new MessageResponse(
                                msg.getId(),
                                msg.getContent(),
                                msg.getSender() != null ? msg.getSender().getId() : null,
                                msg.getSender() != null
                                                ? (msg.getSender().getFirstName() + " " + msg.getSender().getLastName())
                                                : "System",
                                msg.isSystemMessage(),
                                msg.getSentAt());
        }
}
