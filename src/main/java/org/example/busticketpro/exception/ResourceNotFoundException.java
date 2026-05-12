package org.example.busticketpro.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " không tìm thấy với ID: " + id);
    }
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
