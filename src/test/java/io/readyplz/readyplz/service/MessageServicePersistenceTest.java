package io.readyplz.readyplz.service;

import io.readyplz.readyplz.domain.Member;
import io.readyplz.readyplz.domain.Message;
import io.readyplz.readyplz.domain.Role;
import io.readyplz.readyplz.dto.summary.MessageSummaryDTO;
import io.readyplz.readyplz.repository.MemberRepository;
import io.readyplz.readyplz.repository.MessageRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DataJpaTest
@ActiveProfiles("test")
@Import(MessageService.class)
class MessageServicePersistenceTest {

	@MockBean
	private SimpMessagingTemplate messagingTemplate;

	@Autowired
	private MessageService messageService;

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void sendMessage_storesSenderAndReceiverWithoutInitializingMessageCollections() {
		Role admin = Role.builder().name("ROLE_ADMIN").build();
		entityManager.persist(admin);

		Member sender = member("senderUser", "sender@test.com", "발신닉");
		Member receiver = member("receiverUser", "receiver@test.com", "수신닉");
		receiver.getRoles().add(admin);
		entityManager.persist(sender);
		entityManager.persist(receiver);
		entityManager.flush();

		Long senderId = sender.getId();
		Long receiverId = receiver.getId();
		entityManager.clear();

		Member managedSender = memberRepository.findById(senderId).orElseThrow();
		Member managedReceiver = memberRepository.findById(receiverId).orElseThrow();

		Message saved = messageService.sendMessage(senderId, receiverId, "hello");

		assertFalse(Hibernate.isInitialized(managedSender.getSentMessages()));
		assertFalse(Hibernate.isInitialized(managedSender.getReceivedMessages()));
		assertFalse(Hibernate.isInitialized(managedReceiver.getSentMessages()));
		assertFalse(Hibernate.isInitialized(managedReceiver.getReceivedMessages()));
		verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());

		assertNotNull(saved.getId());
		entityManager.flush();
		entityManager.clear();

		Message stored = messageRepository.findById(saved.getId()).orElseThrow();
		assertEquals(senderId, stored.getSender().getId());
		assertEquals(receiverId, stored.getReceiver().getId());
		assertEquals("hello", stored.getContent());

		Page<MessageSummaryDTO> sent = messageRepository.findBySenderId(senderId, PageRequest.of(0, 10));
		assertEquals(1, sent.getTotalElements());
		assertEquals("hello", sent.getContent().get(0).getContent());
		assertEquals(senderId, sent.getContent().get(0).getSenderId());
		assertEquals(receiverId, sent.getContent().get(0).getReceiverId());

		Page<MessageSummaryDTO> received = messageRepository.findByReceiverId(receiverId, PageRequest.of(0, 10));
		assertEquals(1, received.getTotalElements());
		assertEquals("hello", received.getContent().get(0).getContent());
		assertEquals(senderId, received.getContent().get(0).getSenderId());
		assertEquals(receiverId, received.getContent().get(0).getReceiverId());

		Page<MessageSummaryDTO> conversation = messageService.getConversation(
				senderId, receiverId, PageRequest.of(0, 10));
		assertEquals(1, conversation.getTotalElements());
		assertEquals("hello", conversation.getContent().get(0).getContent());
		assertEquals(senderId, conversation.getContent().get(0).getSenderId());
		assertEquals(receiverId, conversation.getContent().get(0).getReceiverId());
	}

	private Member member(String username, String email, String nickname) {
		return Member.builder()
				.username(username)
				.password("password")
				.email(email)
				.nickname(nickname)
				.country("KR")
				.build();
	}
}
