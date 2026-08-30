package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.request.AddContactRequest;
import com.akash.pooler_backend.dto.response.ContactResponse;
import com.akash.pooler_backend.entity.PbUserEntity;

import java.util.List;

/**
 * @author Akash Kumar
 */
public interface ContactService {

    ContactResponse add(PbUserEntity owner, AddContactRequest req);

    List<ContactResponse> list(PbUserEntity owner);

    ContactResponse setFavorite(PbUserEntity owner, String contactEntityId, boolean favorite);

    void remove(PbUserEntity owner, String contactEntityId);
}
