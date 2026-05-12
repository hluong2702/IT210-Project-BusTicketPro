package org.example.busticketpro.service;

import org.example.busticketpro.dto.request.RegisterRequest;
import org.example.busticketpro.dto.request.UpdateProfileRequest;
import org.example.busticketpro.entity.User;
import org.example.busticketpro.enums.Role;

import java.util.List;

public interface UserService {
    User register(RegisterRequest request);
    User findById(Long id);
    User findByUsername(String username);
    User updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, String oldPassword, String newPassword);
    List<User> findByRole(Role role);
    long countByRole(Role role);
}
