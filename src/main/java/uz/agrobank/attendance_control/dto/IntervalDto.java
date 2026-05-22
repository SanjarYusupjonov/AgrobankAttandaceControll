package uz.agrobank.attendance_control.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;

@Data
@AllArgsConstructor
public class IntervalDto {
    private LocalTime start;
    private LocalTime end;
    private String type;
}
