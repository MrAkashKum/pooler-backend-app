package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.request.CreateFeedbackRequest;
import com.akash.pooler_backend.dto.response.FeedbackResponse;
import com.akash.pooler_backend.entity.PbUserEntity;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Akash Kumar
 */
public interface FeedbackService {

    FeedbackResponse create(PbUserEntity user, CreateFeedbackRequest request, HttpServletRequest httpRequest);

    List<FeedbackResponse> listAll();

    void delete(String feedbackEntityId);
}
