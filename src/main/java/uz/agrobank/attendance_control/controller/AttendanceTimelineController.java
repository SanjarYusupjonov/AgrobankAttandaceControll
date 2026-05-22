package uz.agrobank.attendance_control.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.agrobank.attendance_control.dto.EmployeeTimelineResponse;
import uz.agrobank.attendance_control.dto.TimelineResponse;
import uz.agrobank.attendance_control.service.AttendanceTimelineService;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@AllArgsConstructor
@RequestMapping("/attendance")
public class AttendanceTimelineController {
    private final AttendanceTimelineService attendanceTimelineService;

    @GetMapping("/timeline")
    public Page<TimelineResponse> getTimeline(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false, defaultValue = "") String name,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return attendanceTimelineService.getTimelines(
                departmentId, name, date, fromDate, toDate, page, size
        );
    }

    @GetMapping("/getTimelineByEmployee")
    public Page<EmployeeTimelineResponse> getTimelineByEmployee(
            @RequestParam String employeeName,
            @RequestParam String departmentName,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate
    ) {
        return attendanceTimelineService.getTimelineByEmployee(employeeName, departmentName, fromDate, toDate);
    }
}
