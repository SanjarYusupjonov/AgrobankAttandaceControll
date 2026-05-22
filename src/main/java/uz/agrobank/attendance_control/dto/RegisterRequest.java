package uz.agrobank.attendance_control.dto;

import lombok.Data;
import uz.agrobank.attendance_control.enums.UserType;

import java.util.List;

@Data
public class RegisterRequest {

    private String fullName;

    private String username;

    private String password;

    private UserType type;

    private List<Long> departmentIdList;
}