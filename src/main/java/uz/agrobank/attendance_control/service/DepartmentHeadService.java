package uz.agrobank.attendance_control.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.agrobank.attendance_control.dto.CreateDepartmentHeadRequest;
import uz.agrobank.attendance_control.dto.DepartHeadResponse;
import uz.agrobank.attendance_control.entity.Department;
import uz.agrobank.attendance_control.entity.User;
import uz.agrobank.attendance_control.enums.UserType;
import uz.agrobank.attendance_control.repository.DepartmentRepository;
import uz.agrobank.attendance_control.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class DepartmentHeadService {

    private final UserRepository userRepository;

    private final DepartmentRepository departmentRepository;

    private final PasswordEncoder passwordEncoder;

    public Page<DepartHeadResponse> getAllDepHeads(String query, Long departmentId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<User> depHeads =
                userRepository.findAllDepartmentHeads(UserType.DEPARTMENT_HEAD.name(),query, departmentId, pageable);

        return depHeads.map(user ->
                DepartHeadResponse.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .userName(user.getUsername())
                        .departments(user.getDepartments())
                        .build()
        );
    }

    public String create(CreateDepartmentHeadRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            return "USERNAME_EXISTS";
        }

        List<Department> departments = null;
        if (request.getDepartmentIdList() != null) {
            departments = departmentRepository.findByIdList(request.getDepartmentIdList());
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setType(UserType.DEPARTMENT_HEAD);   // always DEPARTMENT_HEAD
        user.setDepartments(departments);

        userRepository.save(user);

        return "SUCCESS";
    }

    public String delete(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return "NOT_FOUND";
        if (user.getType() != UserType.DEPARTMENT_HEAD) return "FORBIDDEN";
        userRepository.deleteById(id);
        return "SUCCESS";
    }
}
