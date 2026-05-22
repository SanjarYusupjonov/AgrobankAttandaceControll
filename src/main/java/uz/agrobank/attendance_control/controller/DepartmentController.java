package uz.agrobank.attendance_control.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.agrobank.attendance_control.dto.DepartmentResponse;
import uz.agrobank.attendance_control.service.DepartmentService;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/department")
public class DepartmentController {
    private final DepartmentService departmentService;

    @GetMapping("/getAll")
    public List<DepartmentResponse> getAllAttendanceInfo(
            @RequestParam(required = false) String name
    ) {
        return departmentService.findAll(name);
    }
}
