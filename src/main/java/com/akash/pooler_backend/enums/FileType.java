package com.akash.pooler_backend.enums;

public enum FileType {

    IMAGE("Image file"),
    DOCUMENT("Document file"),
    OTHER("Other file type");

    private final String description;

    FileType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
