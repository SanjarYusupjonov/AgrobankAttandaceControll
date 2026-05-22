package uz.agrobank.attendance_control.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.agrobank.attendance_control.dto.AttendanceResponce;
import uz.agrobank.attendance_control.service.AttendanceService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/getAll")
    public List<AttendanceResponce> getAllAttendanceInfo() {
        return attendanceService.findAll();
    }

    @GetMapping("/getAllByDepartment")
    public List<AttendanceResponce> getAllAttendanceInfoByDepartment(
            @RequestParam Long departmentId
    ) {
        return attendanceService.findAllByDepartment(departmentId);
    }

    @GetMapping("/searchByName")
    public List<AttendanceResponce> searchByName(@RequestParam String name) {
        return attendanceService.searchByName(name);
    }

    @GetMapping("/getByDateRange")
    public List<AttendanceResponce> getByDateRange(
            @RequestParam LocalDateTime fromDate,
            @RequestParam LocalDateTime toDate
    ) {
        return attendanceService.getByDateRange(fromDate, toDate);
    }
}