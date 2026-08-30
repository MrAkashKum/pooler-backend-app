package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.config.AppProperties;
import com.akash.pooler_backend.constants.ResponseMessages;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.exception.MailDispatchException;
import com.akash.pooler_backend.service.MailService;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {
    private static final String TEMPLATE_VARIABLE_USER_NAME = "userName";

    private final Resend resendClient;
    private final TemplateEngine templateEngine;
    private final AppProperties props;

    @Override
    @Async("mailExecutor")
    public void sendPasswordResetMail(PbUserEntity pbUserEntity, String resetToken) {
        String resetLink = props.getFrontendBaseUrl() + "/reset-password?token=" + resetToken;
        Context ctx = buildContext(pbUserEntity, Map.of(
                "resetLink",resetLink,
                "expiryMinutes", props.getPasswordReset().getTokenExpiryMinutes(),
                TEMPLATE_VARIABLE_USER_NAME, displayName(pbUserEntity)
        ));
        sendHtmlMail(pbUserEntity.getEmail(), "Reset Your Password", "mail/password-reset", ctx);
        log.info("Password reset mail dispatched for userId={}", pbUserEntity.getEntityId());

    }

    @Override
    @Async("mailExecutor")
    public void sendEmailVerificationMail(PbUserEntity pbUserEntity, String verificationToken) {
        String verifyLink = props.getFrontendBaseUrl() + "/verify-email?token=" + verificationToken;
        Context ctx = buildContext(pbUserEntity, Map.of(
                "verifyLink", verifyLink,
                "expiryMinutes", props.getEmailVerification().getTokenExpiryMinutes(),
                TEMPLATE_VARIABLE_USER_NAME, displayName(pbUserEntity)
        ));
        sendHtmlMail(pbUserEntity.getEmail(), "Activate your " + props.getName() + " account", "mail/email-verification", ctx);
        log.info("Email verification mail dispatched for userId={}", pbUserEntity.getEntityId());
    }

    @Override
    @Async("mailExecutor")
    public void sendWelcomeMail(PbUserEntity pbUserEntity) {
        Context ctx = buildContext(pbUserEntity, Map.of(
                TEMPLATE_VARIABLE_USER_NAME, displayName(pbUserEntity),
                "loginLink", props.getFrontendBaseUrl() + "/sign-in"
        ));
        sendHtmlMail(pbUserEntity.getEmail(), "Welcome to " + props.getName(), "mail/welcome", ctx);
        log.info("Welcome mail dispatched for userId={}", pbUserEntity.getEntityId());

    }

    @Override
    @Async("mailExecutor")
    public void sendAccountLockedMail(PbUserEntity pbUserEntity) {
        Context ctx = buildContext(pbUserEntity, Map.of(
                TEMPLATE_VARIABLE_USER_NAME, displayName(pbUserEntity),
                "lockMinutes", props.getSecurity().getLockDurationMinutes(),
                "supportEmail", props.getMail().getFrom()
        ));
        sendHtmlMail(pbUserEntity.getEmail(), "Account Security Alert", "mail/account-locked", ctx);
        log.info("Account locked mail dispatched for userId={}", pbUserEntity.getEntityId());

    }

    // ── Generic mail dispatcher ────────────────────────────────────────

    private void sendHtmlMail(String to, String subject, String template, Context ctx) {
        try {
            String html = templateEngine.process(template, ctx);
            CreateEmailOptions email = CreateEmailOptions.builder()
                    .from(props.getMail().getFromName() + " <" + props.getMail().getFrom() + ">")
                    .to(to)
                    .subject(subject)
                    .html(html)
                    .build();
            CreateEmailResponse response = resendClient.emails().send(email);
            log.info("mailDispatched provider=resend template={} emailId={}", template, response.getId());
        } catch (Exception e) {
            log.error("mailDispatchFailed className={} methodName={} template={} exceptionType={} origin={}",
                    getClass().getSimpleName(), "sendHtmlMail", template, e.getClass().getSimpleName(), origin(e));
            throw new MailDispatchException(ResponseMessages.MAIL_SEND_FAILED, e);
        }
    }

    private Context buildContext(PbUserEntity pbUserEntity, Map<String, Object> extras) {
        Context ctx = new Context();
        ctx.setVariable("pbUserEntity", pbUserEntity);
        ctx.setVariable("appName", props.getName());
        ctx.setVariable("baseUrl", props.getBaseUrl());
        extras.forEach(ctx::setVariable);
        return ctx;
    }

    private static String displayName(PbUserEntity user) {
        String firstName = user.getFirstName();
        return firstName == null || firstName.isBlank() ? "there" : firstName.trim();
    }

    private static String origin(Exception exception) {
        StackTraceElement[] stackTrace = exception.getStackTrace();
        if (stackTrace.length == 0) {
            return "unknown";
        }
        StackTraceElement element = stackTrace[0];
        return element.getClassName() + "." + element.getMethodName() + ":" + element.getLineNumber();
    }
}
