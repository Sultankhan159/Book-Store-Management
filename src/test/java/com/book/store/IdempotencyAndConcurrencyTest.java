package com.book.store;

import com.book.store.entity.Book;
import com.book.store.entity.User;
import com.book.store.repository.BookRepository;
import com.book.store.repository.UserRepository;
import com.book.store.service.IdempotencyService;
import com.book.store.service.MyBookListService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
public class IdempotencyAndConcurrencyTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MyBookListService myBookListService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Book testBook;

    @BeforeEach
    void setUp() {
        String uniqueUsername = "testuser_" + UUID.randomUUID().toString().substring(0, 8);
        testUser = new User();
        testUser.setUsername(uniqueUsername);
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRoles("ROLE_USER");
        testUser = userRepository.save(testUser);

        testBook = new Book();
        testBook.setName("High Performance Systems");
        testBook.setAuthor("Martin Kleppmann");
        testBook.setPrice("49.99");
        testBook.setCategory("Distributed Systems");
        testBook = bookRepository.save(testBook);
    }

    @Test
    @DisplayName("API Idempotency Key prevents duplicate checkout execution")
    void testIdempotencyKeyDeduplication() {
        String idempotencyKey = UUID.randomUUID().toString();

        // 1. First checkout attempt acquires key
        IdempotencyService.IdempotencyStatus firstStatus = idempotencyService.checkAndAcquire(idempotencyKey);
        Assertions.assertEquals(IdempotencyService.IdempotencyStatus.ACQUIRED, firstStatus, "First request must acquire the key");

        // 2. Simultaneous duplicate attempt with same key must be flagged as in-flight
        IdempotencyService.IdempotencyStatus concurrentStatus = idempotencyService.checkAndAcquire(idempotencyKey);
        Assertions.assertEquals(IdempotencyService.IdempotencyStatus.PROCESSING, concurrentStatus, "Concurrent duplicate must be detected as PROCESSING");

        // 3. Complete first request
        idempotencyService.markCompleted(idempotencyKey);

        // 4. Subsequent duplicate attempt must be detected as COMPLETED
        IdempotencyService.IdempotencyStatus completedStatus = idempotencyService.checkAndAcquire(idempotencyKey);
        Assertions.assertEquals(IdempotencyService.IdempotencyStatus.COMPLETED, completedStatus, "Subsequent duplicate must be detected as COMPLETED");
    }

    @Test
    @DisplayName("JPA Optimistic Locking (@Version) prevents lost updates on concurrent book edits")
    void testOptimisticLockingOnBook() {
        // Fetch book in first session
        Book bookSession1 = bookRepository.findById(testBook.getId()).orElseThrow();
        // Fetch book in second session
        Book bookSession2 = bookRepository.findById(testBook.getId()).orElseThrow();

        // Update in session 1 and save
        bookSession1.setPrice("59.99");
        bookRepository.saveAndFlush(bookSession1);

        // Try updating in session 2 (stale version) -> must fail with OptimisticLockingFailureException
        bookSession2.setPrice("69.99");
        Assertions.assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            bookRepository.saveAndFlush(bookSession2);
        }, "Modifying stale version entity must trigger OptimisticLockingFailureException");
    }

    @Test
    @DisplayName("Concurrent cart checkout with distributed lock guarantees consistency")
    void testConcurrentCheckoutConsistency() throws InterruptedException {
        myBookListService.addToCart(testBook, testUser);

        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successfulOrders = new AtomicInteger(0);
        AtomicInteger blockedDuplicates = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    var order = myBookListService.checkout(testUser);
                    if (order != null) {
                        successfulOrders.incrementAndGet();
                    }
                } catch (Exception e) {
                    blockedDuplicates.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Exactly one checkout should successfully process the cart, other concurrent checkouts find empty cart or are locked
        Assertions.assertEquals(1, successfulOrders.get(), "Only one checkout should successfully execute for the user's cart");
    }
}
