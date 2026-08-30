package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.constants.ApiMapping;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.dto.request.AddContactRequest;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.ContactResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Friend-list / quick-invite contact book.
 *
 * @author Akash Kumar
 */
@RestController
@RequestMapping(ApiMapping.CONTACTS_API)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Contacts", description = "Manage your saved travel buddies")
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    @Operation(summary = "Add a user to your contacts")
    public ResponseEntity<ApiResponse<ContactResponse>> add(
            @CurrentUser PbUserEntity owner,
            @Valid @RequestBody AddContactRequest req) {
        return ResponseEntity.ok(ApiResponse.created(contactService.add(owner, req)));
    }

    @GetMapping
    @Operation(summary = "List all contacts (favourites first)")
    public ResponseEntity<ApiResponse<List<ContactResponse>>> list(@CurrentUser PbUserEntity owner) {
        return ResponseEntity.ok(ApiResponse.ok(contactService.list(owner)));
    }

    @PutMapping(ApiMapping.CONTACT_FAVORITE)
    @Operation(summary = "Toggle the favourite flag on a contact")
    public ResponseEntity<ApiResponse<ContactResponse>> setFavorite(
            @CurrentUser PbUserEntity owner,
            @PathVariable String contactEntityId,
            @RequestParam boolean favorite) {
        return ResponseEntity.ok(ApiResponse.ok(
                contactService.setFavorite(owner, contactEntityId, favorite)));
    }

    @DeleteMapping(ApiMapping.CONTACT_ID)
    @Operation(summary = "Remove a contact from your book")
    public ResponseEntity<ApiResponse<Void>> remove(
            @CurrentUser PbUserEntity owner,
            @PathVariable String contactEntityId) {
        contactService.remove(owner, contactEntityId);
        return ResponseEntity.ok(ApiResponse.message(ResponseMessages.CONTACT_REMOVED));
    }
}
