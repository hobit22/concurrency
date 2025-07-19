package com.example.concurrency.service;

import java.util.HashMap;
import java.util.Map;

public class SentenceServiceV2 implements SentenceService {

    private final Map<Integer, String> storage = new HashMap<>();
    private int idCounter = 1;

    @Override
    public synchronized int create(String sentence) {
        int id = idCounter++;
        storage.put(id, sentence);
        return id;
    }

    @Override
    public synchronized String read(int id) {
        return storage.get(id);
    }

    @Override
    public synchronized boolean update(int id, String newSentence) {
        if (storage.containsKey(id)) {
            storage.put(id, newSentence);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean delete(int id) {
        return storage.remove(id) != null;
    }
}