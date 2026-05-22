package uz.agrobank.attendance_control.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
public class EmployeeTimelineResponse {
    private String employeeName;
    private String departmentName;
    List<EmployeeTimelineDateResponse> dateResponses;

    @Data
    @AllArgsConstructor
    public static class EmployeeTimelineDateResponse{
        private LocalDate date;
        List<EmployeeTimelineIntervalsResponse> intervalsResponses;
    }

    @Data
    @AllArgsConstructor
    public static class EmployeeTimelineIntervalsResponse{
        private LocalTime start;
        private LocalTime end;
        private String type;
    }
}
