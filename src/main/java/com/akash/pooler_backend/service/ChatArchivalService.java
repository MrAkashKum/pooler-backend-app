package com.akash.pooler_backend.service;

public interface ChatArchivalService {

    /**
     * Archive a chat thread to cold storage (JSONB or S3).
     */
    void archiveThread(String threadId);

    /**
     * Retrieve archived chat (read-only access for compliance).
     */
    String retrieveArchivedThread(String threadId);

    /**
     * Get archived message count for audit reports.
     */
    long getArchivedMessageCount(String threadId);
}
