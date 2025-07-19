package com.example.concurrency.service;

import java.util.HashMap;
import java.util.Map;

public class SentenceServiceV1 implements SentenceService{
    private final Map<Integer, String> storage = new HashMap<>();
    private int idCounter = 1;

    @Override
    public int create(String sentence) {
        int id = idCounter++; // 동기화 안됨 → 멀티스레드에서 경쟁 조건 발생 가능
        storage.put(id, sentence);
        return id;
    }

    @Override
    public String read(int id) {
        return storage.get(id);
    }

    @Override
    public boolean update(int id, String newSentence) {
        if (storage.containsKey(id)) {
            storage.put(id, newSentence);
            return true;
        }
        return false;
    }

    @Override
    public boolean delete(int id) {
        return storage.remove(id) != null;
    }
}
