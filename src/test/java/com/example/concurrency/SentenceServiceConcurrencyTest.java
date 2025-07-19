package com.example.concurrency;

import com.example.concurrency.service.SentenceService;
import com.example.concurrency.service.SentenceServiceV1;
import com.example.concurrency.service.SentenceServiceV2;
import com.example.concurrency.service.SentenceServiceV3;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class SentenceServiceConcurrencyTest {

    private static final int THREAD_COUNT = 100;


    private void awaitAndShutdown(ExecutorService executor, CountDownLatch latch) {
        try {
            latch.await();
            executor.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    void runConcurrentCreateTest(SentenceService service) {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Set<Integer> ids = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    int id = service.create("문장");
                    Thread.sleep(100); // 간섭 유도
                    ids.add(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        awaitAndShutdown(executor, latch);

        assertEquals(THREAD_COUNT, ids.size(), "ID 중복 또는 누락 발생");
    }

    void runConcurrentUpdateAndDeleteTest(SentenceService service) {
        int id = service.create("초기 문장");

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        Queue<String> threadLogs = new ConcurrentLinkedQueue<>();
        AtomicBoolean updateReturnedTrue = new AtomicBoolean(false);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    if (idx == 0) {
                        boolean result = service.update(id, "수정됨 by thread-0");
                        threadLogs.add(String.format(
                                "[%d] Thread-%d: %s → %s",
                                System.nanoTime(), idx, (idx == 0 ? "UPDATE" : "DELETE"), (result ? "✅ 성공" : "❌ 실패")
                        ));
                        if (result) updateReturnedTrue.set(true);
                    } else {
                        Thread.sleep(5);
                        boolean result = service.delete(id);
                        threadLogs.add(String.format(
                                "[%d] Thread-%d: %s → %s",
                                System.nanoTime(), idx, (idx == 0 ? "UPDATE" : "DELETE"), (result ? "✅ 성공" : "❌ 실패")
                        ));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    threadLogs.add("Thread-" + idx + ": ⚠️ Interrupted");
                } finally {
                    latch.countDown();
                }
            });
        }

        awaitAndShutdown(executor, latch);

        System.out.println("\n--- 작업 실행 순서 로그 ---");
        threadLogs.forEach(System.out::println);

        long updateSuccess = threadLogs.stream()
                .filter(log -> log.contains("UPDATE") && log.contains("✅")).count();

        long deleteSuccess = threadLogs.stream()
                .filter(log -> log.contains("DELETE") && log.contains("✅")).count();

        long failedCount = threadLogs.size() - (updateSuccess + deleteSuccess);

        // 최종 상태 확인
        String finalValue = service.read(id);

        // 검증
        System.out.println("\n--- 검증 결과 ---");
        System.out.println("✅ UPDATE 성공 수: " + updateSuccess);
        System.out.println("✅ DELETE 성공 수: " + deleteSuccess);
        System.out.println("✅ 실패 작업 수: " + failedCount);
        System.out.println("✅ 최종 문장 상태: " + (finalValue == null ? "삭제됨 (OK)" : "존재함 ❌"));

        assertEquals(1, updateSuccess, "UPDATE는 정확히 1번만 성공해야 합니다");
        assertEquals(1, deleteSuccess, "DELETE도 정확히 1번만 성공해야 합니다");
        assertEquals(THREAD_COUNT - 2, failedCount, "나머지는 모두 실패해야 합니다");
        assertNull(finalValue, "최종적으로 문장은 삭제되어 있어야 합니다");
    }

    @Nested
    class V1Tests {
        SentenceService service = new SentenceServiceV1();

        @Test
        void create_ShouldFail() throws InterruptedException {
            runConcurrentCreateTest(service);
        }

        @Test
        void update_delete_MayFail() throws InterruptedException {
            runConcurrentUpdateAndDeleteTest(service);
        }
    }

    @Nested
    class V2Tests {
        SentenceService service = new SentenceServiceV2();

        @Test
        void create_ShouldFail() throws InterruptedException {
            runConcurrentCreateTest(service);
        }

        @Test
        void update_delete_MayFail() throws InterruptedException {
            runConcurrentUpdateAndDeleteTest(service);
        }
    }

    @Nested
    class V3Tests {
        SentenceService service = new SentenceServiceV3();

        @Test
        void create_ShouldFail() throws InterruptedException {
            runConcurrentCreateTest(service);
        }

        @Test
        void update_delete_MayFail() throws InterruptedException {
            runConcurrentUpdateAndDeleteTest(service);
        }
    }
}