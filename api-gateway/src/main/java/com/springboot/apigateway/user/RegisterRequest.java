package com.springboot.apigateway.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    private String keyCloakId;

    @NotBlank(message = "Email required!!")
    @Email(message = "Invalid Email format!!")
    private String email;

    @NotBlank(message = "Password required!!")
    @Size(min = 6,message = "Password must have atleast 6 characters")
    private String password;

    private String firstName;
    private String lastName;
}
