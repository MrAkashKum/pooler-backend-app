package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.service.FileUploadService;
import com.akash.pooler_backend.support.ArchitectureAssertions;
import org.junit.jupiter.api.Test;

class FileUploadServiceImplTest {

    @Test
    void followsServiceImplementationContract() {
        ArchitectureAssertions.assertServiceImplementation(FileUploadServiceImpl.class, FileUploadService.class);
    }
}
