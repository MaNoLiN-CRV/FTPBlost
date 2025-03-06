package org.manolin.ftpblost.exceptions;

public class FTPException extends Exception {

    public FTPException(String message) {
        super(message);
    }

    public FTPException(String message, Throwable cause) {
        super(message, cause);
    }
}