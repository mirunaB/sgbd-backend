package com.sgbd.sgbd.service.exception;

import org.springframework.http.HttpStatus;

public class ServiceException extends RuntimeException {

    private ExceptionType type;
    private HttpStatus httpStatus;

    public ServiceException() {
    }

    public ServiceException(String message, ExceptionType type, HttpStatus httpStatus) {

        super(message);
        this.type = type;
        this.httpStatus = httpStatus;
    }

    public ServiceException(String message, Throwable cause, ExceptionType type, HttpStatus httpStatus) {

        super(message, cause);
        this.type = type;
        this.httpStatus = httpStatus;
    }

    public ExceptionType getType() {
        return type;
    }

    public void setType(ExceptionType type) {
        this.type = type;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}
