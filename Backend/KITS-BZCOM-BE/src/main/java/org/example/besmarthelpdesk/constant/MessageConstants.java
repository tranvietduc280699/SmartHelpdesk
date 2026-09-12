package org.example.besmarthelpdesk.constant;

public final class MessageConstants {

    private MessageConstants() {
        // Prevent instantiation
    }

    // Success Messages
    public static final String SUCCESS = "success";
    public static final String LOGIN_SUCCESS = "Login successful";
    public static final String LOGOUT_SUCCESS = "Logout successful";
    public static final String REGISTER_SUCCESS = "Member registered successfully";
    public static final String REFRESH_SUCCESS = "Token refreshed successfully";
    public static final String REQUEST_CREATED = "Request created successfully";
    public static final String REQUEST_ASSIGNED = "Developer assigned successfully";
    public static final String STATUS_UPDATED = "Request status updated successfully";
    public static final String ALERT_READ_SUCCESS = "Alert marked as read";

    // Error Messages
    public static final String EMAIL_REGISTERED = "Email is already registered";
    public static final String MEMBER_NOT_FOUND = "Member not found with ID: ";
    public static final String COMPANY_NOT_FOUND = "Company not found with ID: ";
    public static final String REQUEST_NOT_FOUND = "Request not found with ID: ";
    public static final String ALERT_NOT_FOUND = "Alert not found with ID: ";
    public static final String ACCESS_DENIED = "Access denied";
    public static final String UNEXPECTED_ERROR = "An unexpected error occurred";
    public static final String UNAUTHORIZED = "Invalid email or password";
    public static final String INVALID_STATUS_TRANSITION = "Invalid status transition from %s to %s";
    public static final String NO_DEVELOPER_AVAILABLE = "No eligible developer available for assignment";
    public static final String ONLY_CLIENT_CAN_CREATE = "Only clients can create requests";
    public static final String CLIENT_HAS_NO_COMPANY = "Client must belong to a company to create requests";
    
    public static final String VALIDATION_ERROR = "Invalid input parameters";
    public static final String SYSTEM_ERROR = "System internal error";
    public static final String NOT_FOUND = "Resource not found";
    public static final String REFRESH_TOKEN_BLANK = "Refresh token cannot be blank";
    public static final String REFRESH_TOKEN_INVALID = "Refresh token is invalid or expired";
    public static final String BAD_REQUEST = "Bad request";

    // Validation Messages
    public static final String EMAIL_BLANK = "Email cannot be blank";
    public static final String EMAIL_INVALID = "Email must be a valid email format";
    public static final String PASSWORD_BLANK = "Password cannot be blank";
    public static final String NAME_BLANK = "Name cannot be blank";
    public static final String ROLE_NULL = "Role cannot be null";
    public static final String TITLE_BLANK = "Title cannot be blank";
    public static final String CATEGORY_NULL = "Category cannot be null";
    public static final String STATUS_NULL = "Status cannot be null";
    public static final String DESCRIPTION_BLANK = "Description cannot be blank";
}
