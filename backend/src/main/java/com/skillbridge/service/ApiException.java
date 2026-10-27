package com.skillbridge.service;

/** Error with an HTTP status code; turned into a JSON response by the controller advice. */
public class ApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int status;

    public ApiException(int status, String message) { super(message); this.status = status; }
    public int getStatus() { return status; }

    public static ApiException badRequest(String m)   { return new ApiException(400, m); }
    public static ApiException unauthorized(String m) { return new ApiException(401, m); }
    /** 403: used when the user IS logged in but may not do this (for example a wrong password when deleting). */
    public static ApiException forbidden(String m)    { return new ApiException(403, m); }
    public static ApiException notFound(String m)     { return new ApiException(404, m); }
    public static ApiException conflict(String m)     { return new ApiException(409, m); }
}
