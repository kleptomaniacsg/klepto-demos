package com.example.demoproject;

import java.util.Set;
import java.util.regex.Pattern;

public class SensitiveFieldDetector {
    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
        "ssn", "social", "password", "pass", "pin", "cvv", "card", "credit",
        "phone", "mobile", "telephone", "email", "mail", "dob", "birth", 
        "address", "street", "zip", "postcode", "account", "iban", "swift"
    );

    private static final Pattern SENSITIVE_PATTERN = 
        Pattern.compile(".*\\b(" + String.join("|", SENSITIVE_KEYWORDS) + ")\\b.*", 
                        Pattern.CASE_INSENSITIVE);

    public static boolean isSensitive(String fieldName) {
        return SENSITIVE_PATTERN.matcher(fieldName).matches();
    }

    public static String maskValue(String value, boolean isEmail) {
        if (value == null || value.isEmpty()) return "[NULL]";
        if (isEmail && value.contains("@")) {
            return maskEmail(value);
        }
        // Generic mask: show first/last char if long enough
        if (value.length() <= 2) return "**";
        return value.charAt(0) + "***" + value.charAt(value.length() - 1);
    }

    private static String maskEmail(String email) {
        String[] parts = email.split("@", 2);
        if (parts.length != 2) return "***@***";
        String user = parts[0];
        if (user.length() <= 2) return "***@" + parts[1];
        return user.charAt(0) + "***" + user.charAt(user.length() - 1) + "@" + parts[1];
    }
}