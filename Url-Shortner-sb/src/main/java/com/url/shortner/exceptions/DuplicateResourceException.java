package com.url.shortner.exceptions;

// A resource the client tried to create already exists (e.g. username/email already registered)
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}