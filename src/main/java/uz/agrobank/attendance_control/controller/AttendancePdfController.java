package uz.agrobank.attendance_control.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.agrobank.attendance_control.dto.EmployeeTimelineResponse;
import uz.agrobank.attendance_control.dto.TimelineResponse;
import uz.agrobank.attendance_control.service.AttendancePdfService;
import uz.agrobank.attendance_control.service.AttendanceTimelineService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/pdf")
public class AttendancePdfController {

    private final AttendancePdfService      pdfService;
    private final AttendanceTimelineService timelineService;

    @GetMapping("/timeline/export")
    public ResponseEntity<byte[]> exportTimeline(
            @RequestParam(required = false) Long          departmentId,
            @RequestParam(required = false) String        name,
            @RequestParam(required = false) LocalDate     date,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(required = false) String        departmentName,
            @RequestParam(required = false) String        dateRangeLabel
    ) throws IOException {
        List<TimelineResponse> all = timelineService
                .getTimelines(departmentId, name, date, fromDate, toDate, 0, Integer.MAX_VALUE)
                .getContent();
        byte[] bytes = pdfService.exportAttendance(all, departmentName, dateRangeLabel);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"davomat.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/getTimelineByEmployee/export")
    public ResponseEntity<byte[]> exportEmployeeTimeline(
            @RequestParam String                  employeeName,
            @RequestParam String                  departmentName,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(required = false) String        dateRangeLabel
    ) throws IOException {
        EmployeeTimelineResponse employee = timelineService
                .getTimelineByEmployee(employeeName, departmentName, fromDate, toDate)
                .getContent().get(0);
        byte[] bytes = pdfService.exportEmployeeTimeline(employee, dateRangeLabel);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + employeeName + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}