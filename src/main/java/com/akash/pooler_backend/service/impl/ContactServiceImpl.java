package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.dto.request.AddContactRequest;
import com.akash.pooler_backend.dto.response.ContactResponse;
import com.akash.pooler_backend.entity.PbContactEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.exception.ContactAlreadyExistsException;
import com.akash.pooler_backend.exception.ContactNotFoundException;
import com.akash.pooler_backend.exception.ContactSelfNotAllowedException;
import com.akash.pooler_backend.exception.UserNotFoundException;
import com.akash.pooler_backend.interceptors.annotation.AuditAction;
import com.akash.pooler_backend.repository.PbContactRepository;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Akash Kumar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final PbContactRepository contactRepository;
    private final PbUserRepository userRepository;

    @Override
    @Transactional
    @AuditAction("CONTACT_ADD")
    public ContactResponse add(PbUserEntity owner, AddContactRequest req) {
        if (owner.getEntityId().equals(req.getContactUserEntityId())) {
            throw new ContactSelfNotAllowedException();
        }
        PbUserEntity contactUser = userRepository.findByEntityId(req.getContactUserEntityId())
                .orElseThrow(() -> new UserNotFoundException(req.getContactUserEntityId()));

        if (contactRepository.existsByOwnerEntityIdAndContactUserEntityId(
                owner.getEntityId(), req.getContactUserEntityId())) {
            throw new ContactAlreadyExistsException();
        }

        PbContactEntity entity = PbContactEntity.builder()
                .entityId(newId())
                .ownerEntityId(owner.getEntityId())
                .contactUserEntityId(req.getContactUserEntityId())
                .nickname(req.getNickname())
                .favorite(req.isFavorite())
                .build();
        entity = contactRepository.save(entity);
        log.info("User {} added contact {}", owner.getEntityId(), req.getContactUserEntityId());
        return ContactResponse.from(entity, contactUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactResponse> list(PbUserEntity owner) {
        List<PbContactEntity> contacts = contactRepository
                .findAllByOwnerEntityIdOrderByFavoriteDescCreatedAtDesc(owner.getEntityId());
        if (contacts.isEmpty()) return List.of();

        // Single batch fetch of contact users
        Set<String> contactUserIds = contacts.stream()
                .map(PbContactEntity::getContactUserEntityId)
                .collect(Collectors.toSet());
        Map<String, PbUserEntity> usersById = userRepository.findAll().stream()
                .filter(user -> contactUserIds.contains(user.getEntityId()))
                .collect(Collectors.toMap(PbUserEntity::getEntityId, Function.identity(), (first, duplicate) -> first));

        return contacts.stream()
                .map(c -> ContactResponse.from(c, usersById.get(c.getContactUserEntityId())))
                .toList();
    }

    @Override
    @Transactional
    @AuditAction("CONTACT_FAVORITE")
    public ContactResponse setFavorite(PbUserEntity owner, String contactEntityId, boolean favorite) {
        PbContactEntity entity = loadOwned(owner, contactEntityId);
        entity.setFavorite(favorite);
        entity = contactRepository.save(entity);
        PbUserEntity contactUser = userRepository
                .findByEntityId(entity.getContactUserEntityId())
                .orElse(null);
        return ContactResponse.from(entity, contactUser);
    }

    @Override
    @Transactional
    @AuditAction("CONTACT_REMOVE")
    public void remove(PbUserEntity owner, String contactEntityId) {
        PbContactEntity entity = loadOwned(owner, contactEntityId);
        contactRepository.delete(entity);
        log.info("User {} removed contact {}", owner.getEntityId(), contactEntityId);
    }

    private PbContactEntity loadOwned(PbUserEntity owner, String contactEntityId) {
        return contactRepository
                .findByEntityIdAndOwnerEntityId(contactEntityId, owner.getEntityId())
                .orElseThrow(ContactNotFoundException::new);
    }

    private static String newId() {
        return "ctc-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
