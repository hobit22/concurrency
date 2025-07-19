package com.example.concurrency.service;

public interface SentenceService {
    int create(String sentence);
    String read(int id);
    boolean update(int id, String newSentence);
    boolean delete(int id);
}
