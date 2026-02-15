package ru.kors.finalproject.web.api.v1;

public class ApiUnauthorizedException extends RuntimeException {
    public ApiUnauthorizedException(String message) {
        super(message);
    }
}
