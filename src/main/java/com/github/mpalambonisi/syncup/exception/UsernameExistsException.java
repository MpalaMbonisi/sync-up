package com.github.mpalambonisi.syncup.exception;

public class UsernameExistsException extends RuntimeException{

    public UsernameExistsException(String message){
        super(message);
    }
}
