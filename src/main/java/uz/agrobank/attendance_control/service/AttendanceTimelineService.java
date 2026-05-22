package uz.agrobank.attendance_control.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import uz.agrobank.attendance_control.dto.EmployeeTimelineResponse;
import uz.agrobank.attendance_control.dto.IntervalDto;
import uz.agrobank.attendance_control.dto.TimelineResponse;
import uz.agrobank.attendance_control.entity.AttendanceInfoClean;
import uz.agrobank.attendance_control.entity.Department;
import uz.agrobank.attendance_control.entity.User;
import uz.agrobank.attendance_control.repository.AttendanceCleanRepository;
import uz.agrobank.attendance_control.repository.DepartmentRepository;
import uz.agrobank.attendance_control.security.SecurityUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AttendanceTimelineService {

    private final AttendanceCleanRepository attendanceCleanRepository;
    private final DepartmentRepository      departmentRepository;

//    private static final LocalTime LUNCH_START  = LocalTime.of(13, 0);
//    private static final LocalTime LUNCH_END    = LocalTime.of(14, 0);
    private static final LocalTime DAY_START    = LocalTime.of(0, 0, 0);
    private static final LocalTime DAY_END      = LocalTime.of(23, 59, 59);

    // ════════════════════════════════════════════════
    //  1. Barcha hodimlar timeline (Attendance sahifasi)
    // ════════════════════════════════════════════════

    public Page<TimelineResponse> getTimelines(
            Long          departmentId,
            String        name,
            LocalDate     date,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int           page,
            int           size
    ) {
        size = Math.max(size, 10);

        LocalDateTime from = fromDate;
        LocalDateTime to   = toDate;
        if (date != null) {
            from = date.atStartOfDay();
            to   = date.atTime(23, 59, 59);
        }

        String nameTrimmed = (name != null && !name.isBlank()) ? name.trim() : null;

        User currentUser = SecurityUtils.getCurrentUser();
        List<Long> userDepIds = (currentUser.getDepartments() != null)
                ? currentUser.getDepartments().stream()
                .map(Department::getId)
                .toList()
                : List.of();

        List<AttendanceInfoClean> logs = attendanceCleanRepository
                .findByOptionalFilters(departmentId, nameTrimmed, from, to, userDepIds);

        return toPage(groupToTimelines(logs), page, size);
    }

    private List<TimelineResponse> groupToTimelines(List<AttendanceInfoClean> logs) {
        // Avval DATE bo'yicha, keyin NAME bo'yicha
        Map<LocalDate, Map<String, List<AttendanceInfoClean>>> grouped =
                logs.stream().collect(Collectors.groupingBy(
                        l -> l.getTime().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.groupingBy(
                                AttendanceInfoClean::getName,
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                ));

        List<TimelineResponse> result = new ArrayList<>();
        for (var dateEntry : grouped.entrySet()) {
            for (var nameEntry : dateEntry.getValue().entrySet()) {
                List<AttendanceInfoClean> dayLogs = nameEntry.getValue();
                result.add(buildResponse(
                        nameEntry.getKey(),
                        dayLogs.get(0).getDepartment().getName(),
                        dateEntry.getKey(),
                        dayLogs
                ));
            }
        }
        return result;
    }

    private TimelineResponse buildResponse(String name, String department,
                                           LocalDate date,
                                           List<AttendanceInfoClean> dayLogs) {
        TimelineResponse res = new TimelineResponse();
        res.setName(name);
        res.setDepartment(department);
        res.setDate(date.toString());
        res.setIntervals(buildIntervals(dayLogs));
        return res;
    }

    private List<IntervalDto> buildIntervals(List<AttendanceInfoClean> dayLogs) {
        List<AttendanceInfoClean> sorted = dayLogs.stream()
                .sorted(Comparator.comparing(AttendanceInfoClean::getTime))
                .toList();

        List<IntervalDto> raw = new ArrayList<>();

        // ── Agar birinchi harakat "CHIQISH" bo'lsa → kun 00:00 dan "KIRISH" bilan boshlangan deb hisoblaymiz
        AttendanceInfoClean first = sorted.get(0);
        boolean firstIsExit = !"KIRISH".equalsIgnoreCase(first.getCardReader());
        if (firstIsExit) {
            LocalTime end = first.getTime().toLocalTime();
            raw.add(new IntervalDto(DAY_START, end, "work"));
        }

        // ── O'rtadagi intervallar
        for (int i = 0; i < sorted.size() - 1; i++) {
            AttendanceInfoClean curr = sorted.get(i);
            AttendanceInfoClean next = sorted.get(i + 1);

            LocalTime start = curr.getTime().toLocalTime();
            LocalTime end   = next.getTime().toLocalTime();
            String    type  = "KIRISH".equalsIgnoreCase(curr.getCardReader()) ? "work" : "break";

            raw.add(new IntervalDto(start, end, type));
        }

        // ── Agar oxirgi harakat "KIRISH" bo'lsa → kun 23:59:59 gacha "work" deb hisoblaymiz
        AttendanceInfoClean last = sorted.get(sorted.size() - 1);
        boolean lastIsEntry = "KIRISH".equalsIgnoreCase(last.getCardReader());
        if (lastIsEntry) {
            LocalTime start = last.getTime().toLocalTime();
            raw.add(new IntervalDto(start, DAY_END, "work"));
        }

        return raw;
    }

//    private List<IntervalDto> applyLunchToIntervalDto(List<IntervalDto> intervals) {
//        List<IntervalDto> result     = new ArrayList<>();
//        boolean           lunchAdded = false;
//
//        for (IntervalDto iv : intervals) {
//            LocalTime s = iv.getStart();
//            LocalTime e = iv.getEnd();
//            String    t = iv.getType();
//
//            if (!e.isAfter(LUNCH_START) || !s.isBefore(LUNCH_END)) {
//                result.add(iv);
//                continue;
//            }
//
//            if (s.isBefore(LUNCH_START)) {
//                result.add(new IntervalDto(s, LUNCH_START, t));
//            }
//
//            if (!lunchAdded) {
//                result.add(new IntervalDto(LUNCH_START, LUNCH_END, "lunch"));
//                lunchAdded = true;
//            }
//
//            if (e.isAfter(LUNCH_END)) {
//                result.add(new IntervalDto(LUNCH_END, e, t));
//            }
//        }
//
//        if (!lunchAdded) {
//            result.add(new IntervalDto(LUNCH_START, LUNCH_END, "lunch"));
//        }
//
//        result.sort(Comparator.comparing(IntervalDto::getStart));
//        return result;
//    }

    private <T> Page<T> toPage(List<T> list, int page, int size) {
        int start = page * size;
        if (start >= list.size()) {
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(page, size), list.size());
        }
        return new PageImpl<>(
                list.subList(start, Math.min(start + size, list.size())),
                PageRequest.of(page, size),
                list.size()
        );
    }

    // ════════════════════════════════════════════════
    //  2. Bitta hodim timeline (EmployeeTimeline sahifasi)
    // ════════════════════════════════════════════════

    public Page<EmployeeTimelineResponse> getTimelineByEmployee(
            String employeeName,
            String departmentName,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        Department department = departmentRepository.findByName(departmentName);

        List<AttendanceInfoClean> logs =
                attendanceCleanRepository.findByNameAndDepartmentOrderByDateDescTimeAsc(
                        employeeName, department.getId(), fromDate, toDate);

        Map<LocalDate, List<AttendanceInfoClean>> groupedByDate =
                logs.stream()
                        .collect(Collectors.groupingBy(
                                l -> l.getTime().toLocalDate(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        List<EmployeeTimelineResponse.EmployeeTimelineDateResponse> dateResponses = new ArrayList<>();

        for (var entry : groupedByDate.entrySet()) {
            LocalDate date = entry.getKey();

            List<AttendanceInfoClean> dayLogs = entry.getValue().stream()
                    .sorted(Comparator.comparing(AttendanceInfoClean::getTime))
                    .toList();

            List<EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse> raw = new ArrayList<>();

            // ── Agar birinchi harakat "CHIQISH" bo'lsa → 00:00 dan "work" deb hisoblaymiz
            AttendanceInfoClean first = dayLogs.get(0);
            boolean firstIsExit = !"KIRISH".equalsIgnoreCase(first.getCardReader());
            if (firstIsExit) {
                LocalTime end = first.getTime().toLocalTime();
                raw.add(new EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse(DAY_START, end, "work"));
            }

            // ── O'rtadagi intervallar
            for (int i = 0; i < dayLogs.size() - 1; i++) {
                AttendanceInfoClean curr = dayLogs.get(i);
                AttendanceInfoClean next = dayLogs.get(i + 1);

                LocalTime start = curr.getTime().toLocalTime();
                LocalTime end   = next.getTime().toLocalTime();
                String    type  = "KIRISH".equalsIgnoreCase(curr.getCardReader()) ? "work" : "break";

                raw.add(new EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse(start, end, type));
            }

            // ── Agar oxirgi harakat "KIRISH" bo'lsa → 23:59:59 gacha "work" deb hisoblaymiz
            AttendanceInfoClean last = dayLogs.get(dayLogs.size() - 1);
            boolean lastIsEntry = "KIRISH".equalsIgnoreCase(last.getCardReader());
            if (lastIsEntry) {
                LocalTime start = last.getTime().toLocalTime();
                raw.add(new EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse(start, DAY_END, "work"));
            }

            List<EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse> finalIntervals = raw;
            finalIntervals.sort(Comparator.comparing(
                    EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse::getStart));

            dateResponses.add(new EmployeeTimelineResponse.EmployeeTimelineDateResponse(date, finalIntervals));
        }

        return new PageImpl<>(
                List.of(new EmployeeTimelineResponse(employeeName, departmentName, dateResponses)),
                PageRequest.of(0, 1),
                1
        );
    }

//    private List<EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse> applyLunch(
//            List<EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse> intervals
//    ) {
//        List<EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse> result     = new ArrayList<>();
//        boolean                                                           lunchAdded = false;
//
//        for (var iv : intervals) {
//            LocalTime s = iv.getStart();
//            LocalTime e = iv.getEnd();
//            String    t = iv.getType();
//
//            if (!e.isAfter(LUNCH_START) || !s.isBefore(LUNCH_END)) {
//                result.add(iv);
//                continue;
//            }
//
//            if (s.isBefore(LUNCH_START)) {
//                result.add(new EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse(s, LUNCH_START, t));
//            }
//
//            if (!lunchAdded) {
//                result.add(new EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse(
//                        LUNCH_START, LUNCH_END, "lunch"));
//                lunchAdded = true;
//            }
//
//            if (e.isAfter(LUNCH_END)) {
//                result.add(new EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse(LUNCH_END, e, t));
//            }
//        }
//
//        if (!lunchAdded) {
//            result.add(new EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse(
//                    LUNCH_START, LUNCH_END, "lunch"));
//        }
//
//        return result;
//    }
}