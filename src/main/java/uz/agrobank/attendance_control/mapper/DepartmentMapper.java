package uz.agrobank.attendance_control.mapper;

import uz.agrobank.attendance_control.dto.DepartmentResponse;
import uz.agrobank.attendance_control.entity.Department;

import java.util.List;

public class DepartmentMapper {
    public static DepartmentResponse toDto(Department department) {
        DepartmentResponse dto = new DepartmentResponse();
        dto.setId(department.getId());
        dto.setName(department.getName());
        return dto;
    }

    public static List<DepartmentResponse> toDtoList(List<Department> entityList) {
        return entityList.stream()
                .map(DepartmentMapper::toDto)
                .toList();
    }
}
