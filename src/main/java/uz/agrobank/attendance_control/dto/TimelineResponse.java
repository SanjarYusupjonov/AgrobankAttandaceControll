package uz.agrobank.attendance_control.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimelineResponse {
    private String name;
    private String date;
    private String department;
    private List<IntervalDto> intervals;
}