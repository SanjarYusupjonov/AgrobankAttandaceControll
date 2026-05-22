package uz.agrobank.attendance_control.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.agrobank.attendance_control.dto.EmployeeTimelineResponse;
import uz.agrobank.attendance_control.dto.ImportResult;
import uz.agrobank.attendance_control.dto.TimelineResponse;
import uz.agrobank.attendance_control.service.AttendanceExcelService;
import uz.agrobank.attendance_control.service.AttendanceTimelineService;
import uz.agrobank.attendance_control.service.ExcelImportService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/excel")
public class AttendanceExcelController {
    private final AttendanceExcelService    excelService;
    private final AttendanceTimelineService attendanceTimelineService;
    private final ExcelImportService        excelImportService;

    /**
     * AgroBank "Person Authentication Record" Excel faylini import qilish.
     *
     * POST /excel/import
     * Content-Type: multipart/form-data
     * Field: "file"
     *
     * Natija:
     *  - Department mavjud bo'lmasa yaratiladi
     *  - Attendance_info ga yoziladi
     *  - DB trigger orqali attendance_info_clean avtomatik yangilanadi
     */
    @PostMapping("/import")
    public ResponseEntity<ImportResult> importExcel(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ImportResult(0, 0, 0, "Fayl bo'sh yubormang"));
        }
        try {
            ImportResult result = excelImportService.importFromExcel(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ImportResult(0, 0, 0, "Xato: " + e.getMessage()));
        }
    }

    @GetMapping("/timeline/export")
    public ResponseEntity<byte[]> exportTimeline(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate
    ) throws IOException {
        List<TimelineResponse> all = attendanceTimelineService
                .getTimelines(departmentId, name, date, fromDate, toDate, 0, Integer.MAX_VALUE)
                .getContent();

        byte[] bytes = excelService.exportAttendance(all);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"attendance.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/getTimelineByEmployee/export")
    public ResponseEntity<byte[]> exportEmployeeTimeline(
            @RequestParam String employeeName,
            @RequestParam String departmentName,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate
    ) throws IOException {
        EmployeeTimelineResponse employee = attendanceTimelineService
                .getTimelineByEmployee(employeeName, departmentName, fromDate, toDate)
                .getContent()
                .get(0);

        byte[] bytes = excelService.exportEmployeeTimeline(employee);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + employeeName + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
