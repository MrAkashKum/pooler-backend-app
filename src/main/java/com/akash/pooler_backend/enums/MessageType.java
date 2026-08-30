package com.akash.pooler_backend.enums;

public enum MessageType {

    TEXT("Text message"),
    LOCATION_SHARE("Location sharing"),
    TELEGRAM_ID_SHARE("Telegram ID shared"),
    FILE_UPLOAD("File uploaded"),
    REACTION("Emoji reaction"),
    TYPING_INDICATOR("Typing indicator");

    private final String description;

    MessageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
