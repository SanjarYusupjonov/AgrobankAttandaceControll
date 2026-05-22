package uz.agrobank.attendance_control.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uz.agrobank.attendance_control.entity.Department;

import java.util.List;

@Data
@Builder
public class DepartHeadResponse {
    private Long id;
    private String fullName;
    private String userName;
    private List<Department> departments;
}
