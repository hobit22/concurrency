package com.example.concurrency.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SentenceServiceV3 implements SentenceService {

    private final Map<Integer, String> storage = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    @Override
    public int create(String sentence) {
        int id = idCounter.getAndIncrement(); // 원자적으로 ID 생성
        storage.put(id, sentence);            // ConcurrentHashMap → 스레드 안전
        return id;
    }

    @Override
    public String read(int id) {
        return storage.get(id);
    }

    @Override
    public boolean update(int id, String newSentence) {
        return storage.replace(id, newSentence) != null;
    }

    @Override
    public boolean delete(int id) {
        return storage.remove(id) != null;
    }
}