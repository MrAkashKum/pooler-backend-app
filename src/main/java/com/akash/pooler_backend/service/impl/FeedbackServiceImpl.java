package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.dto.request.CreateFeedbackRequest;
import com.akash.pooler_backend.dto.response.FeedbackResponse;
import com.akash.pooler_backend.entity.PbFeedbackEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.exception.FeedbackNotFoundException;
import com.akash.pooler_backend.interceptors.annotation.AuditAction;
import com.akash.pooler_backend.repository.PbFeedbackRepository;
import com.akash.pooler_backend.service.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * @author Akash Kumar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private static final String PLATFORM_HEADER = "X-Platform";
    private static final String APP_VERSION_HEADER = "X-App-Version";

    private final PbFeedbackRepository feedbackRepository;

    @Override
    @Transactional
    @AuditAction("FEEDBACK_CREATE")
    public FeedbackResponse create(PbUserEntity user, CreateFeedbackRequest request, HttpServletRequest httpRequest) {
        PbFeedbackEntity entity = PbFeedbackEntity.builder()
                .entityId(newId())
                .submitterEntityId(user.getEntityId())
                .emotion(request.getEmotion().trim())
                .subject(request.getSubject().trim())
                .rating(request.getRating())
                .message(trimToNull(request.getMessage()))
                .platform(trimToNull(httpRequest.getHeader(PLATFORM_HEADER)))
                .appVersion(trimToNull(httpRequest.getHeader(APP_VERSION_HEADER)))
                .build();
        entity = feedbackRepository.save(entity);
        log.info("Feedback created feedbackId={} submitterId={} subject={}",
                entity.getEntityId(), user.getEntityId(), entity.getSubject());
        return FeedbackResponse.from(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> listAll() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    @Override
    @Transactional
    @AuditAction("FEEDBACK_DELETE")
    public void delete(String feedbackEntityId) {
        PbFeedbackEntity entity = feedbackRepository.findByEntityId(feedbackEntityId)
                .orElseThrow(() -> new FeedbackNotFoundException(feedbackEntityId));
        feedbackRepository.delete(entity);
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    private static String newId() {
        return "fb-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
