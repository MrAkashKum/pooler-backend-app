package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.request.CreateFeedbackRequest;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.FeedbackResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.interceptors.annotation.RateLimit;
import com.akash.pooler_backend.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Private app feedback. Riders can submit only; admins can read and delete.
 *
 * @author Akash Kumar
 */
@RestController
@RequestMapping(ApiMapping.FEEDBACK_API)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Feedback", description = "Private app feedback for admin review")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @RateLimit(maxRequests = 5, windowSeconds = 300)
    @Operation(summary = "Create private app feedback")
    public ResponseEntity<ApiResponse<FeedbackResponse>> create(
            @CurrentUser PbUserEntity user,
            @Valid @RequestBody CreateFeedbackRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.created(feedbackService.create(user, request, httpRequest)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all app feedback")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(feedbackService.listAll()));
    }

    @DeleteMapping(ApiMapping.FEEDBACK_ID)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete one feedback record")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String feedbackEntityId) {
        feedbackService.delete(feedbackEntityId);
        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.FEEDBACK_DELETED));
    }
}
