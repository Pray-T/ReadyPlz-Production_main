package Readyplz.io.ReadyPlz.repository;

import Readyplz.io.ReadyPlz.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


public interface MessageRepository extends JpaRepository<Message, Long> {
    
    // 사용자가 보낸 메시지 목록 조회 (페이지네이션) - sender/receiver JOIN FETCH로 N+1 방지
    @Query(
        value = "select new Readyplz.io.ReadyPlz.dto.SummaryDTO.MessageSummaryDTO(m.id, m.content, m.createdAt, m.isRead, s.id, s.nickname, r.id, r.nickname) " +
                "from Message m join m.sender s join m.receiver r where s.id = :memberId",
        countQuery = "select count(m) from Message m where m.sender.id = :memberId"
    )
    Page<Readyplz.io.ReadyPlz.dto.SummaryDTO.MessageSummaryDTO> findBySenderId(@Param("memberId") Long memberId, Pageable pageable);
    
    // 사용자가 받은 메시지 목록 조회 (페이지네이션) - sender/receiver JOIN FETCH로 N+1 방지
    @Query(
        value = "select new Readyplz.io.ReadyPlz.dto.SummaryDTO.MessageSummaryDTO(m.id, m.content, m.createdAt, m.isRead, s.id, s.nickname, r.id, r.nickname) " +
                "from Message m join m.sender s join m.receiver r where r.id = :memberId",
        countQuery = "select count(m) from Message m where m.receiver.id = :memberId"
    )
    Page<Readyplz.io.ReadyPlz.dto.SummaryDTO.MessageSummaryDTO> findByReceiverId(@Param("memberId") Long memberId, Pageable pageable);
    
    // 두 사용자 간의 대화 내용 조회 (페이지네이션) - sender/receiver JOIN FETCH로 N+1 방지
    @Query(
        value = "select new Readyplz.io.ReadyPlz.dto.SummaryDTO.MessageSummaryDTO(m.id, m.content, m.createdAt, m.isRead, s.id, s.nickname, r.id, r.nickname) " +
                "from Message m join m.sender s join m.receiver r " +
                "where (s.id = ?1 and r.id = ?2) or (s.id = ?2 and r.id = ?1)",
        countQuery = "select count(m) from Message m where (m.sender.id = ?1 and m.receiver.id = ?2) or (m.sender.id = ?2 and m.receiver.id = ?1)"
    )
    Page<Readyplz.io.ReadyPlz.dto.SummaryDTO.MessageSummaryDTO> findConversation(Long memberId1, Long memberId2, Pageable pageable);
    
    // 사용자별 대화방 목록 (상대 사용자 기준으로 묶어서, 마지막 메시지와 미읽음 수 포함)
    @Query(value = "\n" +
            "SELECT \n" +
            "  cp.other_member_id, \n" +
            "  u.nickname AS other_member_nickname, \n" +
            "  m2.content AS last_message_content, \n" +
            "  m2.created_at AS last_message_time, \n" +
            "  CASE WHEN m2.sender_id = :userId THEN TRUE ELSE FALSE END AS is_last_from_me, \n" +
            "  (SELECT COUNT(*) FROM message mu \n" +
            "     WHERE mu.receiver_id = :userId \n" +
            "       AND mu.sender_id = cp.other_member_id \n" +
            "       AND mu.is_read = FALSE) AS unread_count \n" +
            "FROM ( \n" +
            "  SELECT m.receiver_id AS other_member_id \n" +
            "  FROM message m \n" +
            "  WHERE m.sender_id = :userId \n" +
            "  UNION \n" +
            "  SELECT m.sender_id AS other_member_id \n" +
            "  FROM message m \n" +
            "  WHERE m.receiver_id = :userId AND m.sender_id IS NOT NULL \n" +
            ") cp \n" +
            "JOIN members u ON u.member_id = cp.other_member_id \n" +
            "JOIN message m2 ON m2.id = ( \n" +
            "  SELECT m3.id \n" +
            "  FROM message m3 \n" +
            "  WHERE ((m3.sender_id = :userId AND m3.receiver_id = cp.other_member_id) \n" +
            "         OR (m3.sender_id = cp.other_member_id AND m3.receiver_id = :userId)) \n" +
            "  ORDER BY m3.created_at DESC, m3.id DESC \n" +
            "  LIMIT 1 \n" +
            ") \n" +
            "WHERE NOT EXISTS ( \n" +
            "  SELECT 1 FROM member_roles mr \n" +
            "  JOIN role r ON r.id = mr.role_id \n" +
            "  WHERE mr.member_id = cp.other_member_id AND r.name = 'ROLE_ADMIN' \n" +
            ") \n" +
            "ORDER BY m2.created_at DESC, m2.id DESC",
            countQuery = "\n" +
            "SELECT COUNT(*) FROM ( \n" +
            "  SELECT cp.other_member_id \n" +
            "  FROM ( \n" +
            "    SELECT m.receiver_id AS other_member_id \n" +
            "    FROM message m \n" +
            "    WHERE m.sender_id = :userId \n" +
            "    UNION \n" +
            "    SELECT m.sender_id AS other_member_id \n" +
            "    FROM message m \n" +
            "    WHERE m.receiver_id = :userId AND m.sender_id IS NOT NULL \n" +
            "  ) cp \n" +
            "  WHERE NOT EXISTS ( \n" +
            "    SELECT 1 FROM member_roles mr \n" +
            "    JOIN role r ON r.id = mr.role_id \n" +
            "    WHERE mr.member_id = cp.other_member_id AND r.name = 'ROLE_ADMIN' \n" +
            "  ) \n" +
            ") x",
            nativeQuery = true)
    Page<Object[]> findConversationsForUser(@Param("userId") Long userId, Pageable pageable);
    
    
    // 전체 메시지 개수 조회
    @Query("SELECT COUNT(m) FROM Message m")
    Long countTotalMessages();
    
    // 가장 오래된 메시지들을 삭제 (MySQL용)
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM message WHERE id IN (SELECT id FROM (SELECT id FROM message ORDER BY created_at ASC LIMIT :limit) AS temp)", nativeQuery = true)
    void deleteOldestMessages(@Param("limit") int limit);

    // 특정 회원이 보낸/받은 모든 메시지 삭제 (외래키 제약 회피용 분리 삭제)
    @Modifying
    @Transactional
    @Query("delete from Message m where m.sender.id = :memberId")
    void deleteAllBySenderId(@Param("memberId") Long memberId);

    @Modifying
    @Transactional
    @Query("delete from Message m where m.receiver.id = :memberId")
    void deleteAllByReceiverId(@Param("memberId") Long memberId);
 } 