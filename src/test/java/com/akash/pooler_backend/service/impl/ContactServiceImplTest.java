package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.ContactService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class ContactServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(ContactServiceImpl.class, ContactService.class);
    }
}
