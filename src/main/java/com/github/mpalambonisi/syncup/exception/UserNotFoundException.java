package com.github.mpalambonisi.syncup.exception;

public class UserNotFoundException extends RuntimeException{

    public UserNotFoundException(String message){
        super(message);
    }
}
