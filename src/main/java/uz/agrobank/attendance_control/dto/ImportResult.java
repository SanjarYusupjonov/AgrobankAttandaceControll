package uz.agrobank.attendance_control.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImportResult {
    private int totalRows;
    private int inserted;
    private int skipped;   // duplicate (name, time) — o'tkazib yuborildi
    private String message;
}
