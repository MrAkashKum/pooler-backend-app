package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.repository.PbChatMessageRepository;
import com.akash.pooler_backend.repository.PbChatSearchIndexRepository;
import com.akash.pooler_backend.entity.PbChatMessageEntity;
import com.akash.pooler_backend.entity.PbChatSearchIndexEntity;
import com.akash.pooler_backend.service.ChatSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatSearchServiceImpl implements ChatSearchService {

    private final PbChatMessageRepository messageRepository;
    private final PbChatSearchIndexRepository searchIndexRepository;

    @Override
    @Transactional
    public void indexThreadMessages(String threadId) {
        List<PbChatMessageEntity> messages = messageRepository.findByThreadAndCreatedAfter(
                threadId,
                Instant.now().minusSeconds(7200L)
        );

        if (messages.isEmpty()) {
            log.debug("No messages to index for thread: {}", threadId);
            return;
        }

        String indexedText = messages.stream()
                .map(PbChatMessageEntity::getContent)
                .collect(Collectors.joining(" "));

        List<String> messageIds = messages.stream()
                .map(PbChatMessageEntity::getEntityId)
                .toList();

        PbChatSearchIndexEntity index = PbChatSearchIndexEntity.builder()
                .threadId(threadId)
                .messageText(indexedText)
                .messageIds(messageIds)
                .build();

        searchIndexRepository.save(index);

        // Mark messages as indexed
        messages.forEach(m -> m.setIsIndexed(true));
        messageRepository.saveAll(messages);

        log.info("Indexed {} messages for thread: {}", messages.size(), threadId);
    }

    @Override
    public List<String> search(String threadId, String query) {
        PbChatSearchIndexEntity index = searchIndexRepository.findByThreadId(threadId)
                .orElse(null);

        if (index == null) {
            log.debug("No search index found for thread: {}", threadId);
            return List.of();
        }

        String lowerQuery = query.toLowerCase();
        String indexText = (index.getMessageText() != null ? index.getMessageText() : "").toLowerCase();

        if (!indexText.contains(lowerQuery)) {
            return List.of();
        }

        return index.getMessageIds();
    }

    @Override
    @Transactional
    public void archiveSearchIndex(String threadId) {
        searchIndexRepository.findByThreadId(threadId)
                .ifPresent(index -> log.info("Archiving search index for thread: {}", threadId));
    }
}
