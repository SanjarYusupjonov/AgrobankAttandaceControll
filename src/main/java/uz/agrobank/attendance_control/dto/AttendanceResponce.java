package uz.agrobank.attendance_control.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponce {
    private String name;
    private String department;
    private LocalDateTime time;
    private String cardReader;
}
