package uz.agrobank.attendance_control.dto;

import lombok.Data;

@Data
public class LoginRequest {

    private String username;

    private String password;
}