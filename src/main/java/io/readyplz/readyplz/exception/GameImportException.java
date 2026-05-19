package io.readyplz.readyplz.exception;

import lombok.Getter;

@Getter
public class GameImportException extends RuntimeException {

    private final int batchNumber;

    public GameImportException(int batchNumber, String message, Throwable cause) {
        super(message, cause);
        this.batchNumber = batchNumber;
    }
}
