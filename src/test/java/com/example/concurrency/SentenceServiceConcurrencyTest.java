package com.example.concurrency;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

public class SentenceServiceConcurrencyTest {

    private static final int THREAD_COUNT = 100;

    private void runConcurrentCreateTest(SentenceService service) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Set<Integer> ids = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    int id = service.create("문장");
                    ids.add(id);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(THREAD_COUNT, ids.size(), "중복 없이 모든 ID가 생성되어야 함");
    }

    private void runConcurrentUpdateTest(SentenceService service) throws InterruptedException {
        int id = service.create("초기 문장");

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    service.update(id, "수정된 문장 " + idx);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertNotNull(service.read(id), "수정 이후 문장이 null이 아니어야 함");
    }

    private void runConcurrentDeleteTest(SentenceService service) throws InterruptedException {
        int id = service.create("삭제할 문장");

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    service.delete(id); // 여러 스레드가 동시에 삭제 시도
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertNull(service.read(id), "삭제된 후에는 문장이 존재하지 않아야 함");
    }

    @Test
    void testAllImplementations() throws InterruptedException {
        List<SentenceService> implementations = List.of(
                new SentenceServiceV1(), // HashMap + int (비스레드 안전)
                new SentenceServiceV2(), // synchronized + HashMap
                new SentenceServiceV3(), // AtomicInteger + ConcurrentHashMap
                new SentenceServiceV4(), // ReentrantLock
                new SentenceServiceV5(), // ReadWriteLock
                new SentenceServiceV6()  // Collections.synchronizedMap
        );

        for (SentenceService service : implementations) {
            runConcurrentCreateTest(service);
            runConcurrentUpdateTest(service);
            runConcurrentDeleteTest(service);
        }
    }
}