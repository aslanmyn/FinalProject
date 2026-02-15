package ru.kors.finalproject.service;

import org.springframework.stereotype.Service;

/**
 * Determines user role from email format:
 * - admin@kbtu.kz → ADMIN (starts with "admin")
 * - a_mustafayev@kbtu.kz → STUDENT (underscore after first letter)
 * - z.professor@kbtu.kz → PROFESSOR (period after first letter)
 */
@Service
public class UserRoleDetector {

    public UserRole detectRole(String email) {
        if (email == null || email.isBlank()) {
            return UserRole.UNKNOWN;
        }
        String localPart = email.contains("@") ? email.split("@")[0].trim().toLowerCase() : email.trim().toLowerCase();
        
        // Admin detection
        if (localPart.startsWith("admin")) {
            return UserRole.ADMIN;
        }
        
        if (localPart.length() < 2) {
            return UserRole.UNKNOWN;
        }
        
        char secondChar = localPart.charAt(1);
        
        // Student: underscore (format: a_name)
        if (secondChar == '_') {
            return UserRole.STUDENT;
        }
        
        // Professor: period (format: z.professor)
        if (secondChar == '.') {
            return UserRole.PROFESSOR;
        }
        
        return UserRole.UNKNOWN;
    }
}
