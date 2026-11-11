package com.service.transaction.exception.custom;

public class BadRequestException extends RuntimeException{

    public BadRequestException(String message){
        super(message);
    }

    public BadRequestException(String message, Throwable cause){
        super(message, cause);
    }
}
