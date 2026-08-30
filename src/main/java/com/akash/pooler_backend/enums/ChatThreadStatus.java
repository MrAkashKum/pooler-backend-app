package com.akash.pooler_backend.enums;

public enum ChatThreadStatus {

    ACTIVE("Chat is active and accepting messages"),
    ARCHIVED("Chat is archived and read-only"),
    CLOSED("Chat is manually closed");

    private final String description;

    ChatThreadStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
