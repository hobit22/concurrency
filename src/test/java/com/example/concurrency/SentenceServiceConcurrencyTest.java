package com.example.concurrency;

import com.example.concurrency.service.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    void runPerformanceTest(SentenceService service, String label) throws InterruptedException {
        final int id = service.create("초기 문장");

        int threadCount = 100;
        int opsPerThread = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long start = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    service.update(id, "수정 by thread-" + idx);
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        long end = System.currentTimeMillis();

        long totalOps = threadCount * opsPerThread;
        long durationMs = end - start;
        double opsPerSec = (totalOps * 1000.0) / durationMs;

        System.out.printf("⏱️ [%s - update 경쟁] 시간: %dms, 처리량: %.2f ops/sec%n", label, durationMs, opsPerSec);
    }

    @Nested
    class V1Tests {
        SentenceService service = new SentenceServiceV1();

        @Test
        void createTest() throws InterruptedException {
            runConcurrentCreateTest(service);
        }

        @Test
        void updateDeleteTest() throws InterruptedException {
            runConcurrentUpdateAndDeleteTest(service);
        }
    }

    @Nested
    class V2Tests {
        SentenceService service = new SentenceServiceV2();

        @Test
        void createTest() throws InterruptedException {
            runConcurrentCreateTest(service);
        }

        @Test
        void updateDeleteTest() throws InterruptedException {
            runConcurrentUpdateAndDeleteTest(service);
        }
    }

    @Nested
    class V3Tests {
        SentenceService service = new SentenceServiceV3();

        @Test
        void createTest() throws InterruptedException {
            runConcurrentCreateTest(service);
        }

        @Test
        void updateDeleteTest() throws InterruptedException {
            runConcurrentUpdateAndDeleteTest(service);
        }
    }

    @Nested
    class V4Tests {
        SentenceService service = new SentenceServiceV4();

        @Test
        void createTest() throws InterruptedException {
            runConcurrentCreateTest(service);
        }

        @Test
        void updateDeleteTest() throws InterruptedException {
            runConcurrentUpdateAndDeleteTest(service);
        }
    }

    @Test
    void performanceTestV1() throws InterruptedException {
        runPerformanceTest(new SentenceServiceV1(), "V1");
    }

    @Test
    void performanceTestV2() throws InterruptedException {
        runPerformanceTest(new SentenceServiceV2(), "V2");
    }

    @Test
    void performanceTestV3() throws InterruptedException {
        runPerformanceTest(new SentenceServiceV3(), "V3");
    }

    @Test
    void performanceTestV4() throws InterruptedException {
        runPerformanceTest(new SentenceServiceV4(), "V4");
    }
}