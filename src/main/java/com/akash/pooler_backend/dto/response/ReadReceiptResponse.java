package com.akash.pooler_backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReadReceiptResponse {
    private String messageEntityId;
    private List<String> readByUserIds;
}
