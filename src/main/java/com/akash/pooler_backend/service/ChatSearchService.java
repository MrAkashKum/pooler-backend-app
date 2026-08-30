package com.akash.pooler_backend.service;

import java.util.List;

public interface ChatSearchService {

    /**
     * Index thread messages for full-text search before archival.
     */
    void indexThreadMessages(String threadId);

    /**
     * Search messages within a chat (FTS5 or PostgreSQL native).
     */
    List<String> search(String threadId, String query);

    /**
     * Archive search index when chat expires.
     */
    void archiveSearchIndex(String threadId);
}
