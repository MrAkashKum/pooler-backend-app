package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;
import com.akash.pooler_backend.constants.ResponseMessages;

public class FileUploadException extends BaseException {

    public FileUploadException() {
        super(ErrorCode.FILE_UPLOAD_TOO_LARGE, ResponseMessages.FILE_UPLOAD_FAILED);
    }

    public FileUploadException(String message) {
        super(ErrorCode.FILE_UPLOAD_TOO_LARGE, message);
    }

    public FileUploadException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
