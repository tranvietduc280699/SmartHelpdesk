package org.example.besmarthelpdesk.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.besmarthelpdesk.constant.MessageConstants;
import org.example.besmarthelpdesk.enums.Role;

import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = MessageConstants.EMAIL_BLANK)
    @Email(message = MessageConstants.EMAIL_INVALID)
    private String email;

    @NotBlank(message = MessageConstants.PASSWORD_BLANK)
    @ToString.Exclude
    private String password;

    @NotBlank(message = MessageConstants.NAME_BLANK)
    private String name;

    @NotNull(message = MessageConstants.ROLE_NULL)
    private Role role;

    private String phone;

    private String companyId;

    public RegisterRequest(String email, String password, String name, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }
}
