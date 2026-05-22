package uz.agrobank.attendance_control.dto;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
public class DepartmentResponse {
    private Long id;
    private String name;
}
