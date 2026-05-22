package uz.agrobank.attendance_control.service;

import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import uz.agrobank.attendance_control.dto.EmployeeTimelineResponse;
import uz.agrobank.attendance_control.dto.IntervalDto;
import uz.agrobank.attendance_control.dto.TimelineResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.util.List;

@Service
@AllArgsConstructor
public class AttendanceExcelService {

    private final AttendanceTimelineService attendanceTimelineService;

    // ── Umumiy style yaratuvchilar ──────────────────────────────────

    private CellStyle headerStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setWrapText(true);
        return s;
    }

    private CellStyle workStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private CellStyle breakStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private CellStyle lunchStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private CellStyle normalStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private CellStyle altRowStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private String formatInterval(IntervalDto iv) {
        return String.format("%s – %s (%s)",
                fmtTime(iv.getStart()),
                fmtTime(iv.getEnd()),
                translateType(iv.getType()));
    }

    private String fmtTime(LocalTime t) {
        return t != null ? t.toString().substring(0, 5) : "";
    }

    private String translateType(String type) {
        return switch (type.toLowerCase()) {
            case "work"  -> "Ish";
            case "break" -> "Tanaffus";
            case "lunch" -> "Tushlik";
            default      -> type;
        };
    }

    private long calcMinutes(List<? extends Object> intervals, String type, boolean isIntervalDto) {
        // overload qilingan qisqa helper
        return 0;
    }

    // ── 1. Attendance (barcha hodimlar) ────────────────────────────

    public byte[] exportAttendance(List<TimelineResponse> timelines) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Davomat");

            CellStyle hStyle  = headerStyle(wb);
            CellStyle wStyle  = workStyle(wb);
            CellStyle bStyle  = breakStyle(wb);
            CellStyle lStyle  = lunchStyle(wb);
            CellStyle nStyle  = normalStyle(wb);
            CellStyle aStyle  = altRowStyle(wb);

            // Sarlavha
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DAVOMAT HISOBOTI");
            CellStyle titleStyle = wb.createCellStyle();
            Font tf = wb.createFont();
            tf.setBold(true);
            tf.setFontHeightInPoints((short) 14);
            titleStyle.setFont(tf);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            // Header row
            String[] headers = {"#", "Ism", "Bo'lim", "Sana", "Interval turi", "Vaqt oralig'i"};
            Row hRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(hStyle);
            }

            int rowNum = 2;
            int idx    = 1;
            for (TimelineResponse tl : timelines) {
                List<IntervalDto> intervals = tl.getIntervals();
                int startRow = rowNum;

                for (IntervalDto iv : intervals) {
                    Row row = sheet.createRow(rowNum);
                    boolean alt = (idx % 2 == 0);

                    // # va Ism faqat birinchi qatorda
                    if (rowNum == startRow) {
                        row.createCell(0).setCellValue(idx);
                        row.createCell(1).setCellValue(tl.getName());
                        row.createCell(2).setCellValue(tl.getDepartment());
                        row.createCell(3).setCellValue(tl.getDate());
                        row.getCell(0).setCellStyle(alt ? aStyle : nStyle);
                        row.getCell(1).setCellStyle(alt ? aStyle : nStyle);
                        row.getCell(2).setCellStyle(alt ? aStyle : nStyle);
                        row.getCell(3).setCellStyle(alt ? aStyle : nStyle);
                    } else {
                        for (int c = 0; c < 4; c++) {
                            row.createCell(c).setCellStyle(alt ? aStyle : nStyle);
                        }
                    }

                    Cell typeCell = row.createCell(4);
                    typeCell.setCellValue(translateType(iv.getType()));
                    typeCell.setCellStyle(
                            "work".equals(iv.getType())  ? wStyle :
                                    "break".equals(iv.getType()) ? bStyle : lStyle
                    );

                    Cell timeCell = row.createCell(5);
                    timeCell.setCellValue(fmtTime(iv.getStart()) + " – " + fmtTime(iv.getEnd()));
                    timeCell.setCellStyle(alt ? aStyle : nStyle);

                    rowNum++;
                }

                // Merge # va Ism ustunlarini
                if (rowNum - startRow > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(startRow, rowNum - 1, 0, 0));
                    sheet.addMergedRegion(new CellRangeAddress(startRow, rowNum - 1, 1, 1));
                    sheet.addMergedRegion(new CellRangeAddress(startRow, rowNum - 1, 2, 2));
                    sheet.addMergedRegion(new CellRangeAddress(startRow, rowNum - 1, 3, 3));
                }
                idx++;
            }

            // Ustun kengliklari
            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 30 * 256);
            sheet.setColumnWidth(2, 50 * 256);
            sheet.setColumnWidth(3, 15 * 256);
            sheet.setColumnWidth(4, 15 * 256);
            sheet.setColumnWidth(5, 20 * 256);

            // Freeze header
            sheet.createFreezePane(0, 2);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── 2. EmployeeTimeline (bitta hodim) ──────────────────────────

    public byte[] exportEmployeeTimeline(EmployeeTimelineResponse employee) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Hodim Timeline");

            CellStyle hStyle = headerStyle(wb);
            CellStyle wStyle = workStyle(wb);
            CellStyle bStyle = breakStyle(wb);
            CellStyle lStyle = lunchStyle(wb);
            CellStyle nStyle = normalStyle(wb);

            // Sarlavha
            Row titleRow = sheet.createRow(0);
            Cell tc = titleRow.createCell(0);
            tc.setCellValue(employee.getEmployeeName() + " — Timeline hisoboti");
            CellStyle ts = wb.createCellStyle();
            Font tf = wb.createFont();
            tf.setBold(true); tf.setFontHeightInPoints((short) 13);
            ts.setFont(tf);
            tc.setCellStyle(ts);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            // Bo'lim
            Row deptRow = sheet.createRow(1);
            deptRow.createCell(0).setCellValue("Bo'lim: " + employee.getDepartmentName());
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));

            // Header
            String[] headers = {"Sana", "Interval turi", "Boshlanish", "Tugash", "Davomiyligi"};
            Row hRow = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(hStyle);
            }

            int rowNum = 4;
            for (EmployeeTimelineResponse.EmployeeTimelineDateResponse dr : employee.getDateResponses()) {
                int startRow = rowNum;
                List<EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse> ivs = dr.getIntervalsResponses();

                for (EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse iv : ivs) {
                    Row row = sheet.createRow(rowNum);

                    if (rowNum == startRow) {
                        Cell dateCell = row.createCell(0);
                        dateCell.setCellValue(dr.getDate().toString());
                        dateCell.setCellStyle(nStyle);
                    } else {
                        row.createCell(0).setCellStyle(nStyle);
                    }

                    Cell typeCell = row.createCell(1);
                    typeCell.setCellValue(translateType(iv.getType()));
                    typeCell.setCellStyle(
                            "work".equals(iv.getType())  ? wStyle :
                                    "break".equals(iv.getType()) ? bStyle : lStyle
                    );

                    Cell startCell = row.createCell(2);
                    startCell.setCellValue(fmtTime(iv.getStart()));
                    startCell.setCellStyle(nStyle);

                    Cell endCell = row.createCell(3);
                    endCell.setCellValue(fmtTime(iv.getEnd()));
                    endCell.setCellStyle(nStyle);

                    long dur = java.time.Duration.between(iv.getStart(), iv.getEnd()).toMinutes();
                    Cell durCell = row.createCell(4);
                    durCell.setCellValue(dur + " daq");
                    durCell.setCellStyle(nStyle);

                    rowNum++;
                }

                if (rowNum - startRow > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(startRow, rowNum - 1, 0, 0));
                }
            }

            sheet.setColumnWidth(0, 15 * 256);
            sheet.setColumnWidth(1, 15 * 256);
            sheet.setColumnWidth(2, 12 * 256);
            sheet.setColumnWidth(3, 12 * 256);
            sheet.setColumnWidth(4, 12 * 256);
            sheet.createFreezePane(0, 4);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }
}