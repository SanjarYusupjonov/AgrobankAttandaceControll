package uz.agrobank.attendance_control.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uz.agrobank.attendance_control.dto.ImportResult;
import uz.agrobank.attendance_control.entity.Attendance;
import uz.agrobank.attendance_control.entity.Department;
import uz.agrobank.attendance_control.repository.AttendanceRepository;
import uz.agrobank.attendance_control.repository.DepartmentRepository;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private static final int DATA_START_ROW = 11;

    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AttendanceRepository attendanceRepository;
    private final DepartmentRepository departmentRepository;

    // Proxy orqali chaqirish uchun o'z-o'zini lazy inject qilamiz
    @Lazy
    @Autowired
    private ExcelImportService self;

    private final Map<String, Department> deptCache = new ConcurrentHashMap<>();

    /**
     * Transactionsiz — har bir qator uchun REQUIRES_NEW proxy chaqiruvi.
     * Bitta duplicate xatosi butun import ni to'xtatmaydi.
     */
    public ImportResult importFromExcel(MultipartFile file) throws Exception {
        int total    = 0;
        int inserted = 0;
        int skipped  = 0;

        deptCache.clear();

        try (InputStream is = file.getInputStream();
             Workbook wb   = WorkbookFactory.create(is)) {

            Sheet sheet = wb.getSheetAt(0);

            for (int i = DATA_START_ROW; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String        name       = readString(row, 0);
                String        deptName   = readString(row, 1);
                LocalDateTime time       = readDateTime(row, 2);
                String        cardReader = readString(row, 3);

                if (name == null || name.isBlank() || time == null) continue;
                total++;

                boolean saved = self.saveOne(name.trim(), deptName, time,
                        cardReader == null ? null : cardReader.trim());
                if (saved) inserted++;
                else       skipped++;
            }
        }

        String msg = String.format(
                "Import yakunlandi. Jami: %d, Qo'shildi: %d, O'tkazildi (takror): %d",
                total, inserted, skipped);
        log.info(msg);
        return new ImportResult(total, inserted, skipped, msg);
    }

    /**
     * Har bir qator uchun alohida transaction.
     * Duplicate bo'lsa faqat shu qator rollback bo'ladi.
     */
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveOne(String name, String deptName, LocalDateTime time, String cardReader) {
        try {
            if (name.equals("Person Not Matched") || deptName == null || deptName.isBlank()) {
                return false;
            }

            if (cardReader.endsWith("Kirish")) cardReader = "KIRISH";
            if (cardReader.endsWith("Chiqish")) cardReader = "CHIQISH";

            Department dept = resolveDepartment(deptName);

            Attendance a = new Attendance();
            a.setName(name);
            a.setTime(time);
            a.setCardReader(cardReader);
            a.setDepartment(dept);

            attendanceRepository.save(a);
            return true;
        } catch (Exception e) {
            return false; // duplicate yoki boshqa xato — skip
        }
    }

    private Department resolveDepartment(String name) {
        if (name == null || name.isBlank()) return null;
        String trimmed = name.trim();
        return deptCache.computeIfAbsent(trimmed, n -> {
            Department existing = departmentRepository.findByName(n);
            if (existing != null) return existing;
            Department d = new Department();
            d.setName(n);
            return departmentRepository.save(d);
        });
    }

    private String readString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        String val = new DataFormatter().formatCellValue(cell).trim();
        return val.isEmpty() ? null : val;
    }

    private LocalDateTime readDateTime(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue();
        }

        try {
            String val = new DataFormatter().formatCellValue(cell).trim();
            if (val.isEmpty()) return null;
            return LocalDateTime.parse(val, DATETIME_FMT);
        } catch (Exception e) {
            log.warn("Vaqtni o'qib bo'lmadi, row={} col={}", row.getRowNum(), col);
            return null;
        }
    }
}