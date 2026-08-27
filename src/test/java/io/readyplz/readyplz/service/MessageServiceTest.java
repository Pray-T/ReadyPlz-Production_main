package io.readyplz.readyplz.service;

import io.readyplz.readyplz.config.MessageRetentionProperties;
import io.readyplz.readyplz.domain.Member;
import io.readyplz.readyplz.domain.Message;
import io.readyplz.readyplz.dto.notification.NotificationDTO;
import io.readyplz.readyplz.repository.MemberGameRepository;
import io.readyplz.readyplz.repository.MemberRepository;
import io.readyplz.readyplz.repository.MessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

	@Mock
	private MessageRepository messageRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private MemberGameRepository memberGameRepository;

	@Mock
	private MessageRetentionProperties messageRetentionProperties;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@InjectMocks
	private MessageService messageService;

	@AfterEach
	void tearDown() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void sendMessage_publishesNotificationAfterCommit_withFrontContractPayload() {
		Member sender = Member.builder().id(1L).username("senderUser").nickname("발신닉").build();
		Member receiver = Member.builder().id(2L).username("receiverUser").nickname("수신닉").build();
		stubSuccessfulSend(sender, receiver);

		TransactionSynchronizationManager.initSynchronization();
		try {
			messageService.sendMessage(1L, 2L, "hello");

			verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());

			for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
				sync.afterCommit();
			}

			ArgumentCaptor<NotificationDTO> payloadCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
			verify(messagingTemplate).convertAndSendToUser(
					eq("receiverUser"),
					eq("/queue/notifications"),
					payloadCaptor.capture());

			NotificationDTO payload = payloadCaptor.getValue();
			assertEquals("MESSAGE", payload.type());
			assertEquals("새 메시지가 도착했습니다.", payload.message());
			assertEquals("발신닉", payload.data());
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void sendMessage_doesNotPublish_whenSaveFails() {
		Member sender = Member.builder().id(1L).username("senderUser").nickname("발신닉").build();
		Member receiver = Member.builder().id(2L).username("receiverUser").nickname("수신닉").build();

		when(memberRepository.findById(2L)).thenReturn(Optional.of(receiver));
		when(memberRepository.findById(1L)).thenReturn(Optional.of(sender));
		when(memberGameRepository.existsSharedGame(1L, 2L)).thenReturn(true);
		when(messageRepository.countConversationBetween(1L, 2L)).thenReturn(0L);
		when(messageRetentionProperties.getMaxPerConversation()).thenReturn(1000);
		when(messageRepository.save(any(Message.class))).thenThrow(new RuntimeException("DB error"));

		TransactionSynchronizationManager.initSynchronization();
		try {
			assertThrows(RuntimeException.class, () -> messageService.sendMessage(1L, 2L, "hello"));

			assertEquals(0, TransactionSynchronizationManager.getSynchronizations().size());
			verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void sendMessage_doesNotPublish_whenTransactionRolledBack() {
		Member sender = Member.builder().id(1L).username("senderUser").nickname("발신닉").build();
		Member receiver = Member.builder().id(2L).username("receiverUser").nickname("수신닉").build();
		stubSuccessfulSend(sender, receiver);

		TransactionSynchronizationManager.initSynchronization();
		try {
			messageService.sendMessage(1L, 2L, "hello");

			for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
				sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
			}

			verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void sendMessage_doesNotPublish_whenValidationFails() {
		assertThrows(IllegalArgumentException.class,
				() -> messageService.sendMessage(1L, 2L, "   "));

		verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
		verify(messageRepository, never()).save(any());
	}

	@Test
	void markConversationAsRead_delegatesToRepositoryWithReceiverAsMe() {
		when(messageRepository.markConversationAsRead(1L, 2L)).thenReturn(3);

		messageService.markConversationAsRead(1L, 2L);

		verify(messageRepository).markConversationAsRead(1L, 2L);
	}

	private void stubSuccessfulSend(Member sender, Member receiver) {
		when(memberRepository.findById(2L)).thenReturn(Optional.of(receiver));
		when(memberRepository.findById(1L)).thenReturn(Optional.of(sender));
		when(memberGameRepository.existsSharedGame(1L, 2L)).thenReturn(true);
		when(messageRepository.countConversationBetween(1L, 2L)).thenReturn(0L);
		when(messageRetentionProperties.getMaxPerConversation()).thenReturn(1000);
		when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}
}
