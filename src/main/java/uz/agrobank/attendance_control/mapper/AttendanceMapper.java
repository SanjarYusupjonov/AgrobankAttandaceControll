package uz.agrobank.attendance_control.mapper;

import uz.agrobank.attendance_control.dto.AttendanceResponce;
import uz.agrobank.attendance_control.entity.Attendance;

import java.util.List;

public class AttendanceMapper {
    public static List<AttendanceResponce> toDtoList(List<Attendance> entityList) {
        return entityList.stream()
                .map(entity -> AttendanceResponce.builder()
                        .name(entity.getName())
                        .department(entity.getDepartment().getName())
                        .time(entity.getTime())
                        .cardReader(entity.getCardReader())
                        .build())
                .toList();
    }
}
