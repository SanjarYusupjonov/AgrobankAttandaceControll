package uz.agrobank.attendance_control.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uz.agrobank.attendance_control.dto.AttendanceResponce;
import uz.agrobank.attendance_control.dto.DepartmentResponse;
import uz.agrobank.attendance_control.entity.Department;
import uz.agrobank.attendance_control.mapper.DepartmentMapper;
import uz.agrobank.attendance_control.repository.DepartmentRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    public List<DepartmentResponse> findAll(String name) {
        List<Department> departmentEntityList = departmentRepository.findAllFilterByName(name);
        return DepartmentMapper.toDtoList(departmentEntityList);
    }
}
