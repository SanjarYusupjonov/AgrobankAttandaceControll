package uz.agrobank.attendance_control.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateDepartmentHeadRequest {

    private String fullName;

    private String username;

    private String password;

    private List<Long> departmentIdList;
}