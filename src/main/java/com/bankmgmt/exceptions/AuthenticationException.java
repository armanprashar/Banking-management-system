package com.bankmgmt.exceptions;

/** Thrown when credentials or recovery verification fails (distinct from domain validation errors). */
public class AuthenticationException extends Exception {

    public AuthenticationException(String message) {
        super(message);
    }
}
