package com.akash.pooler_backend.service;

import com.akash.pooler_backend.entity.PbUserEntity;

public interface MailService {

    void sendPasswordResetMail(PbUserEntity pbUserEntity, String resetToken);
    void sendEmailVerificationMail(PbUserEntity pbUserEntity, String verificationToken);
    void sendWelcomeMail(PbUserEntity pbUserEntity);
    void sendAccountLockedMail(PbUserEntity pbUserEntity);
}
