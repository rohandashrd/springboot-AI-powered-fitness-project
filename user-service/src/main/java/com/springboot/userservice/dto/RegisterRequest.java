package com.springboot.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email required!!")
    @Email(message = "Invalid Email format!!")
    private String email;

    @NotBlank(message = "Password required!!")
    @Size(min = 6,message = "Password must have atleast 6 characters")
    private String password;

    private String firstName;
    private String lastName;
}
