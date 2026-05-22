package uz.agrobank.attendance_control.service;

import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uz.agrobank.attendance_control.dto.AttendanceResponce;
import uz.agrobank.attendance_control.entity.Attendance;
import uz.agrobank.attendance_control.entity.Department;
import uz.agrobank.attendance_control.entity.User;
import uz.agrobank.attendance_control.enums.UserType;
import uz.agrobank.attendance_control.mapper.AttendanceMapper;
import uz.agrobank.attendance_control.repository.AttendanceRepository;
import uz.agrobank.attendance_control.repository.DepartmentRepository;
import uz.agrobank.attendance_control.security.CustomUserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final DepartmentRepository departmentRepository;

    public List<AttendanceResponce> findAll() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        User user = userDetails.getUser();


        List<Attendance> attendanceEntityList = null;

        if (user.getType().equals(UserType.ADMIN)) {
            attendanceEntityList = attendanceRepository.findAll();
        } else {
            List<Department> departments = user.getDepartments();

            if (departments == null || departments.isEmpty()) {
                attendanceEntityList = List.of();
            } else {
                List<Long> departmentIds = departments.stream()
                        .map(Department::getId)
                        .toList();
                attendanceEntityList = attendanceRepository.findAllByDepartments(departmentIds);
            }
        }

        return AttendanceMapper.toDtoList(attendanceEntityList);
    }

    public List<AttendanceResponce> findAllByDepartment(Long departmentId) {

        List<Attendance> attendanceEntityList = attendanceRepository.findAllByDepartment(departmentId);
        return AttendanceMapper.toDtoList(attendanceEntityList);
    }

    public List<AttendanceResponce> searchByName(String name) {
        if (name != null) {
            List<Attendance> attendanceEntityList = attendanceRepository.searchByName(name);

            return AttendanceMapper.toDtoList(attendanceEntityList);
        }

        return null;
    }

    public List<AttendanceResponce> getByDateRange(LocalDateTime fromDate, LocalDateTime toDate) {
        List<Attendance> attendanceEntityList = attendanceRepository.findAllByDateRange(fromDate, toDate);

        return AttendanceMapper.toDtoList(attendanceEntityList);
    }
}
