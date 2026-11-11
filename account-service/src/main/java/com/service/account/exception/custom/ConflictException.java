package com.service.account.exception.custom;

public class ConflictException extends RuntimeException{

    public ConflictException(String message){
        super(message);
    }

    public ConflictException(String message, Throwable cause){
        super(message, cause);
    }
}
