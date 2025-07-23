package com.example.concurrency.service;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SentenceServiceV4 implements SentenceService {

    private final Map<Integer, String> store = new Hashtable<>();
    private final AtomicInteger idCounter = new AtomicInteger(0);

    @Override
    public int create(String sentence) {
        int id = idCounter.incrementAndGet();
        store.put(id, sentence);
        return id;
    }

    @Override
    public String read(int id) {
        return store.get(id);
    }

    @Override
    public boolean update(int id, String sentence) {
        synchronized (store) {
            if (!store.containsKey(id)) return false;
            store.put(id, sentence);
            return true;
        }
    }

    @Override
    public boolean delete(int id) {
        synchronized (store) {
            if (!store.containsKey(id)) return false;
            store.remove(id);
            return true;
        }
    }
}