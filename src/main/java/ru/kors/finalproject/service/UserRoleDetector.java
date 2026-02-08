package ru.kors.finalproject.service;

import org.springframework.stereotype.Service;

/**
 * Determines user role from email format:
 * - a_mustafayev@kbtu.kz → STUDENT (underscore after first letter)
 * - z.teacher@kbtu.kz → TEACHER (period after first letter)
 */
@Service
public class UserRoleDetector {

    public UserRole detectRole(String email) {
        if (email == null || email.isBlank()) {
            return UserRole.UNKNOWN;
        }
        String localPart = email.contains("@") ? email.split("@")[0].trim() : email.trim();
        if (localPart.length() < 2) {
            return UserRole.UNKNOWN;
        }
        char secondChar = localPart.charAt(1);
        if (secondChar == '_') {
            return UserRole.STUDENT;
        }
        if (secondChar == '.') {
            return UserRole.TEACHER;
        }
        return UserRole.UNKNOWN;
    }
}
