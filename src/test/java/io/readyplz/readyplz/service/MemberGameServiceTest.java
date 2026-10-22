package io.readyplz.readyplz.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.readyplz.readyplz.controller.GameController;
import io.readyplz.readyplz.domain.Game;
import io.readyplz.readyplz.domain.Member;
import io.readyplz.readyplz.domain.MemberGame;
import io.readyplz.readyplz.repository.GameRepository;
import io.readyplz.readyplz.repository.MemberGameRepository;
import io.readyplz.readyplz.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
@ActiveProfiles("test")
@Import(MemberGameService.class)
class MemberGameServiceTest {

    private static final long COUNT_BARRIER_WAIT_MS = 400L;

    @Autowired
    private MemberGameService memberGameService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberGameRepository memberGameRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private SqlGate sqlGate;
    private Logger hibernateSqlLogger;
    private Level previousSqlLevel;

    @BeforeEach
    void attachSqlLog() {
        sqlGate = new SqlGate();
        sqlGate.start();
        hibernateSqlLogger = (Logger) LoggerFactory.getLogger("org.hibernate.SQL");
        previousSqlLevel = hibernateSqlLogger.getLevel();
        hibernateSqlLogger.setLevel(Level.DEBUG);
        hibernateSqlLogger.addAppender(sqlGate);
    }

    @AfterEach
    void detachSqlLog() {
        if (sqlGate != null) {
            sqlGate.disarm();
            sqlGate.stop();
        }
        if (hibernateSqlLogger != null) {
            hibernateSqlLogger.detachAppender(sqlGate);
            hibernateSqlLogger.setLevel(previousSqlLevel);
        }
    }

    @Test
    void addGameToCollection_rejectsSixthGameInSameTransaction() {
        Member owner = persistMember("limit-owner");
        Member other = persistMember("limit-other");
        for (int i = 0; i < MemberGameService.MAX_SELECTED_GAMES; i++) {
            own(owner, persistGame(82000 + i, "Limit Game " + i));
        }
        Game extra = persistGame(82010, "Limit Extra");
        own(other, persistGame(82011, "Limit Other Game"));

        sqlGate.clear();
        ResponseEntity<String> response = controllerFor(owner).addGameToCollection(extra.getId(), auth(owner));

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertEquals(MemberGameService.SELECTION_LIMIT_MESSAGE, response.getBody());
        assertUserMessageDoesNotLeakExceptionText(response.getBody());
        assertEquals(MemberGameService.MAX_SELECTED_GAMES, memberGameRepository.countByMemberId(owner.getId()));
        assertFalse(memberGameRepository.existsByMemberIdAndGameId(owner.getId(), extra.getId()));
        assertEquals(1L, memberGameRepository.countByMemberId(other.getId()));
        assertTrue(sqlGate.sawForUpdate(), () -> "회원 행 잠금 쿼리가 없습니다: " + sqlGate.snapshot());
        assertTrue(sqlGate.sawMemberCountQuery(), () -> "회원 id 개수 쿼리가 없습니다: " + sqlGate.snapshot());
    }

    @Test
    void addGameToCollection_missingMemberKeepsExistingMessage() {
        Game game = persistGame(82100, "Missing Member Game");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberGameService.addGameToCollection(-1L, game.getId()));

        assertEquals("회원을 찾을 수 없습니다.", ex.getMessage());
    }

    @Test
    void addGameToCollection_missingGameKeepsExistingMessage() {
        Member owner = persistMember("miss-game");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memberGameService.addGameToCollection(owner.getId(), -1L));

        assertEquals("해당 게임을 찾을 수 없습니다.", ex.getMessage());
        assertEquals(0L, memberGameRepository.countByMemberId(owner.getId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void addGameToCollection_duplicateReturnsExistingMessageAndKeepsOneRow() {
        Member owner = persistMember("dup-owner");
        Member other = persistMember("dup-other");
        Game game = persistGame(83001, "Duplicate Game");
        own(other, persistGame(83002, "Duplicate Other"));
        try {
            GameController controller = controllerFor(owner);
            Authentication authentication = auth(owner);

            ResponseEntity<String> first = controller.addGameToCollection(game.getId(), authentication);
            ResponseEntity<String> second = controller.addGameToCollection(game.getId(), authentication);

            assertEquals(HttpStatus.OK.value(), first.getStatusCode().value());
            assertEquals("게임이 컬렉션에 추가되었습니다.", first.getBody());
            assertEquals(HttpStatus.OK.value(), second.getStatusCode().value());
            assertEquals(MemberGameService.ALREADY_OWNED_MESSAGE, second.getBody());
            assertUserMessageDoesNotLeakExceptionText(second.getBody());
            assertEquals(1L, memberGameRepository.countByMemberId(owner.getId()));
            assertEquals(1L, memberGameRepository.countByMemberId(other.getId()));
            assertTrue(memberGameRepository.existsByMemberIdAndGameId(owner.getId(), game.getId()));
        } finally {
            cleanup();
        }
    }

    @Test
    @Timeout(30)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void addGameToCollection_concurrentAddsStopAtFive() throws Exception {
        Member owner = persistMember("race-owner");
        Member other = persistMember("race-other");
        for (int i = 0; i < MemberGameService.MAX_SELECTED_GAMES - 1; i++) {
            own(owner, persistGame(84000 + i, "Race Owned " + i));
        }
        own(other, persistGame(84010, "Race Other"));
        Game firstNew = persistGame(84011, "Race New A");
        Game secondNew = persistGame(84012, "Race New B");
        long otherCount = memberGameRepository.countByMemberId(other.getId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            sqlGate.armCountBarrier(2);
            List<Future<String>> futures = new ArrayList<>();
            for (Long gameId : List.of(firstNew.getId(), secondNew.getId())) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        return "START_TIMEOUT";
                    }
                    try {
                        memberGameService.addGameToCollection(owner.getId(), gameId);
                        return "OK";
                    } catch (IllegalArgumentException ex) {
                        return "LIMIT:" + ex.getMessage();
                    }
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<String> results = new ArrayList<>();
            for (Future<String> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            sqlGate.disarm();

            long successes = results.stream().filter("OK"::equals).count();
            long limits = results.stream().filter(result -> result.startsWith("LIMIT:")).count();
            assertEquals(1L, successes, () -> results + " sql=" + sqlGate.snapshot());
            assertEquals(1L, limits, () -> results + " sql=" + sqlGate.snapshot());
            String limitResult = results.stream().filter(result -> result.startsWith("LIMIT:")).findFirst().orElseThrow();
            assertEquals("LIMIT:" + MemberGameService.SELECTION_LIMIT_MESSAGE, limitResult);
            assertUserMessageDoesNotLeakExceptionText(limitResult);
            assertEquals((long) MemberGameService.MAX_SELECTED_GAMES, memberGameRepository.countByMemberId(owner.getId()));
            assertEquals(otherCount, memberGameRepository.countByMemberId(other.getId()));
            assertTrue(sqlGate.forUpdateCount() >= 2, () -> "두 요청 모두 회원 행 잠금 쿼리를 타야 합니다: " + sqlGate.snapshot());
        } finally {
            sqlGate.disarm();
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            cleanup();
        }
    }

    @Test
    @Timeout(30)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void addGameToCollection_waitsForThatMemberLockOnly() throws Exception {
        Member owner = persistMember("lock-owner");
        Member other = persistMember("lock-other");
        for (int i = 0; i < MemberGameService.MAX_SELECTED_GAMES - 1; i++) {
            own(owner, persistGame(85000 + i, "Lock Owned " + i));
        }
        own(other, persistGame(85010, "Lock Other A"));
        own(other, persistGame(85011, "Lock Other B"));
        Game fifth = persistGame(85012, "Lock Fifth");
        Game sixth = persistGame(85013, "Lock Sixth");
        long otherCount = memberGameRepository.countByMemberId(other.getId());

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> holder = executor.submit(() -> transactionTemplate.execute(status -> {
            memberRepository.findByIdForUpdate(owner.getId()).orElseThrow();
            locked.countDown();
            try {
                if (!release.await(15, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("member lock was not released");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
            return null;
        }));

        try {
            assertTrue(locked.await(10, TimeUnit.SECONDS), "holder did not acquire the member lock");

            Future<Boolean> otherLock = executor.submit(() -> transactionTemplate.execute(status -> {
                memberRepository.findByIdForUpdate(other.getId()).orElseThrow();
                return true;
            }));
            assertEquals(Boolean.TRUE, otherLock.get(5, TimeUnit.SECONDS));
            assertEquals(otherCount, memberGameRepository.countByMemberId(other.getId()));

            sqlGate.clear();
            Future<String> add = executor.submit(() -> {
                memberGameService.addGameToCollection(owner.getId(), fifth.getId());
                return "OK";
            });
            assertTrue(waitUntil(() -> sqlGate.forUpdateCount() >= 1, 5_000),
                    () -> "추가 요청이 회원 행 잠금 쿼리에 도달하지 않았습니다: " + sqlGate.snapshot());
            long blockedUntil = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(250);
            while (System.nanoTime() < blockedUntil) {
                if (add.isDone()) {
                    fail("회원 행 잠금이 유지되는 동안 추가가 끝났습니다: " + describe(add));
                }
                Thread.sleep(20);
            }

            release.countDown();
            assertEquals("OK", add.get(10, TimeUnit.SECONDS));
            assertEquals((long) MemberGameService.MAX_SELECTED_GAMES, memberGameRepository.countByMemberId(owner.getId()));
            assertEquals(otherCount, memberGameRepository.countByMemberId(other.getId()));

            ResponseEntity<String> rejected = controllerFor(owner).addGameToCollection(sixth.getId(), auth(owner));
            assertEquals(HttpStatus.BAD_REQUEST.value(), rejected.getStatusCode().value());
            assertEquals(MemberGameService.SELECTION_LIMIT_MESSAGE, rejected.getBody());
            assertUserMessageDoesNotLeakExceptionText(rejected.getBody());
            assertEquals((long) MemberGameService.MAX_SELECTED_GAMES, memberGameRepository.countByMemberId(owner.getId()));
            assertFalse(memberGameRepository.existsByMemberIdAndGameId(owner.getId(), sixth.getId()));
            assertEquals(otherCount, memberGameRepository.countByMemberId(other.getId()));
        } finally {
            release.countDown();
            try {
                holder.get(10, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // 행 잠금을 풀고 정리한다.
            }
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            cleanup();
        }
    }

    private static void assertUserMessageDoesNotLeakExceptionText(String body) {
        String normalized = body.toLowerCase(Locale.ROOT);
        assertFalse(body.contains("Exception"));
        assertFalse(normalized.contains("constraint"));
        assertFalse(normalized.contains("sql"));
        assertFalse(normalized.contains("uk_member_game"));
    }

    private GameController controllerFor(Member member) {
        MemberService memberService = mock(MemberService.class);
        when(memberService.findByUsername(member.getUsername())).thenReturn(member);
        return new GameController(mock(GameService.class), memberService, memberGameService);
    }

    private static Authentication auth(Member member) {
        return new UsernamePasswordAuthenticationToken(member.getUsername(), "n/a");
    }

    private Member persistMember(String username) {
        return memberRepository.save(Member.builder()
                .username(username)
                .password("password")
                .email(username + "@test.com")
                .nickname(username)
                .country("KR")
                .build());
    }

    private Game persistGame(int appid, String name) {
        return gameRepository.save(Game.builder()
                .appid(appid)
                .name(name)
                .build());
    }

    private void own(Member member, Game game) {
        memberGameRepository.save(MemberGame.builder()
                .member(member)
                .game(game)
                .build());
    }

    private void cleanup() {
        memberGameRepository.deleteAll();
        memberRepository.deleteAll();
        gameRepository.deleteAll();
    }

    private static boolean waitUntil(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(20);
        }
        return condition.getAsBoolean();
    }

    private static String describe(Future<String> future) {
        try {
            return String.valueOf(future.get(1, TimeUnit.SECONDS));
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    /**
     * 개수 쿼리 로그에서 잠깐 멈춰, 잠금이 없으면 두 트랜잭션이 같은 개수를 보게 한다.
     * 잠금이 있으면 두 번째 요청은 개수 쿼리 전에 회원 행에서 기다린다.
     */
    private static final class SqlGate extends UnsynchronizedAppenderBase<ch.qos.logback.classic.spi.ILoggingEvent> {
        private final AtomicBoolean countBarrierEnabled = new AtomicBoolean(false);
        private final List<String> sql = new ArrayList<>();
        private volatile CountDownLatch countBarrier;

        void clear() {
            synchronized (sql) {
                sql.clear();
            }
        }

        void armCountBarrier(int participants) {
            countBarrier = new CountDownLatch(participants);
            countBarrierEnabled.set(true);
        }

        void disarm() {
            countBarrierEnabled.set(false);
        }

        List<String> snapshot() {
            synchronized (sql) {
                return List.copyOf(sql);
            }
        }

        boolean sawForUpdate() {
            return forUpdateCount() > 0;
        }

        int forUpdateCount() {
            synchronized (sql) {
                int count = 0;
                for (String statement : sql) {
                    if (statement.toLowerCase(Locale.ROOT).contains("for update")) {
                        count++;
                    }
                }
                return count;
            }
        }

        boolean sawMemberCountQuery() {
            synchronized (sql) {
                return sql.stream().anyMatch(SqlGate::isMemberCount);
            }
        }

        @Override
        protected void append(ch.qos.logback.classic.spi.ILoggingEvent event) {
            String message = event.getFormattedMessage();
            if (message == null || message.isBlank()) {
                return;
            }
            CountDownLatch barrier;
            boolean waitForOtherCount;
            synchronized (sql) {
                sql.add(message);
                barrier = countBarrier;
                waitForOtherCount = countBarrierEnabled.get() && barrier != null && isMemberCount(message);
            }
            if (!waitForOtherCount) {
                return;
            }
            barrier.countDown();
            try {
                barrier.await(COUNT_BARRIER_WAIT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        private static boolean isMemberCount(String statement) {
            String normalized = statement.toLowerCase(Locale.ROOT);
            // member_game_id 안에 game_id가 들어 있으므로, 게임 조건은 바인딩된 game_id 컬럼으로만 가른다.
            boolean filtersByGame = normalized.contains("game_id=?") || normalized.contains("game_id =");
            return normalized.contains("member_game")
                    && normalized.contains("count(")
                    && normalized.contains("member_id")
                    && !filtersByGame;
        }
    }
}
