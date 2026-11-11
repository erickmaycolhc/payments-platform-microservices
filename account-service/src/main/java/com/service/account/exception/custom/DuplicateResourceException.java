package com.service.account.exception.custom;

public class DuplicateResourceException extends RuntimeException{

    public DuplicateResourceException(String message){
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause){
        super(message, cause);
    }
}
