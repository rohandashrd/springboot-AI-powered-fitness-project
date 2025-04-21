package com.springboot.userservice.dto;

import com.springboot.userservice.model.UserRole;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
public class UserResponse {


    private String id;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private UserRole role=UserRole.USER;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
