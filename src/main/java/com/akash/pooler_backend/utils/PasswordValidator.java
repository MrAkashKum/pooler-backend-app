package com.akash.pooler_backend.utils;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable password strength validator.
 * Called from services rather than relying solely on @Pattern constraints
 * so the same rules apply to both API-driven and programmatic flows.
 */
@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 72;  // BCrypt max

    /**
     * Validates password strength and returns a list of violation messages.
     * An empty list means the password is valid.
     */
    public List<String> validate(String password) {
        List<String> violations = new ArrayList<>();
        if (password == null || password.isBlank()) {
            violations.add("Password is required");
            return violations;
        }
        if (password.length() < MIN_LENGTH) violations.add("Minimum " + MIN_LENGTH + " characters");
        if (password.length() > MAX_LENGTH) violations.add("Maximum " + MAX_LENGTH + " characters");
        if (!password.matches(".*[A-Z].*")) violations.add("At least one uppercase letter");
        if (!password.matches(".*[a-z].*")) violations.add("At least one lowercase letter");
        if (!password.matches(".*\\d.*"))   violations.add("At least one digit");
        if (!password.matches(".*[@$!%*?&].*")) violations.add("At least one special character (@$!%*?&)");
        return violations;
    }

    public boolean isValid(String password) {
        return validate(password).isEmpty();
    }
}
