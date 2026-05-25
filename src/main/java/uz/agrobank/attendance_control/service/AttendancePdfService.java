package uz.agrobank.attendance_control.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uz.agrobank.attendance_control.dto.EmployeeTimelineResponse;
import uz.agrobank.attendance_control.dto.IntervalDto;
import uz.agrobank.attendance_control.dto.TimelineResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@AllArgsConstructor
public class AttendancePdfService {

    // ── Brand colors ────────────────────────────────────────────────
    private static final DeviceRgb COLOR_HEADER_BG   = new DeviceRgb(0,   51,  102);
    private static final DeviceRgb COLOR_HEADER_TEXT  = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb COLOR_WORK         = new DeviceRgb(34,  197, 94);
    private static final DeviceRgb COLOR_BREAK        = new DeviceRgb(249, 115, 22);
    private static final DeviceRgb COLOR_LUNCH        = new DeviceRgb(234, 179, 8);
    private static final DeviceRgb COLOR_TRACK_BG     = new DeviceRgb(241, 245, 249);
    private static final DeviceRgb COLOR_GRID         = new DeviceRgb(203, 213, 225);
    private static final DeviceRgb COLOR_ROW_ALT      = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb COLOR_LATE_DOT     = new DeviceRgb(239, 68,  68);

    private static final float BAR_HEIGHT = 14f;

    // ════════════════════════════════════════════════════════════════
    //  1. All Employees — Landscape A4
    // ════════════════════════════════════════════════════════════════

    public byte[] exportAttendance(List<TimelineResponse> timelines,
                                   String departmentName,
                                   String dateRangeLabel) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter   writer = new PdfWriter(out);
        PdfDocument pdf    = new PdfDocument(writer);
        pdf.setFlushUnusedObjects(false);
        Document    doc    = new Document(pdf, PageSize.A4.rotate(), false);
        doc.setMargins(28, 28, 28, 28);

        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // ── Cover header ─────────────────────────────────────────────
        addAllEmployeesHeader(doc, bold, regular, departmentName, dateRangeLabel, timelines.size());

        // ── Single table: ruler as repeating header + all rows ───────
        Table mainTable = new Table(UnitValue.createPercentArray(new float[]{5, 22, 20, 10, 43}))
                .useAllAvailableWidth()
                .setMarginBottom(0);

        addRulerAsHeaderCells(mainTable, bold);

        for (int i = 0; i < timelines.size(); i++) {
            addAttendanceRowCells(mainTable, timelines.get(i), i + 1, i % 2 != 0, bold, regular);
        }
        doc.add(mainTable);

        // ── Legend + footer ──────────────────────────────────────────
        doc.add(buildLegend(regular));
        addFooter(pdf, regular, timelines.size());

        doc.close();
        return out.toByteArray();
    }

    // ════════════════════════════════════════════════════════════════
    //  2. Single Employee — Portrait A4
    // ════════════════════════════════════════════════════════════════

    public byte[] exportEmployeeTimeline(EmployeeTimelineResponse employee,
                                         String dateRangeLabel) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter   writer = new PdfWriter(out);
        PdfDocument pdf    = new PdfDocument(writer);
        pdf.setFlushUnusedObjects(false);
        Document    doc    = new Document(pdf, PageSize.A4, false);
        doc.setMargins(28, 28, 28, 28);

        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // ── Employee header card ──────────────────────────────────────
        addEmployeeHeader(doc, bold, regular, employee, dateRangeLabel);

        // ── Stats row ─────────────────────────────────────────────────
        doc.add(buildStatsRow(employee, bold, regular));
        doc.add(new Paragraph(" ").setFontSize(4));

        // ── Single table: ruler as repeating header + all date rows ──
        List<EmployeeTimelineResponse.EmployeeTimelineDateResponse> dates =
                employee.getDateResponses().stream()
                        .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                        .toList();

        Table mainTable = new Table(UnitValue.createPercentArray(new float[]{18, 12, 70}))
                .useAllAvailableWidth()
                .setMarginBottom(0);

        addEmployeeRulerAsHeaderCells(mainTable, bold);

        for (int i = 0; i < dates.size(); i++) {
            addEmployeeDateRowCells(mainTable, dates.get(i), i % 2 != 0, bold, regular);
        }
        doc.add(mainTable);

        // ── Detailed interval table ────────────────────────────────────
        doc.add(new Paragraph(" ").setFontSize(6));
        doc.add(buildIntervalTable(dates, bold, regular));

        // ── Legend + footer ───────────────────────────────────────────
        doc.add(buildLegend(regular));
        addFooter(pdf, regular, dates.size());

        doc.close();
        return out.toByteArray();
    }

    // ════════════════════════════════════════════════════════════════
    //  Header builders
    // ════════════════════════════════════════════════════════════════

    private void addAllEmployeesHeader(Document doc, PdfFont bold, PdfFont regular,
                                       String dept, String dateRange, int total) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth()
                .setBackgroundColor(COLOR_HEADER_BG)
                .setBorder(Border.NO_BORDER)
                .setMarginBottom(8);

        Cell left = new Cell()
                .add(new Paragraph("AGROBANK").setFont(bold).setFontSize(9)
                        .setFontColor(new DeviceRgb(180, 220, 160)))
                .add(new Paragraph("DAVOMAT HISOBOTI").setFont(bold).setFontSize(16)
                        .setFontColor(COLOR_HEADER_TEXT))
                .setBorder(Border.NO_BORDER)
                .setPadding(10);

        String deptText  = dept      != null && !dept.isBlank()      ? dept      : "Barcha bo'limlar";
        String rangeText = dateRange != null && !dateRange.isBlank() ? dateRange : "Barcha sanalar";

        Cell right = new Cell()
                .add(new Paragraph("Bo'lim: " + deptText).setFont(regular).setFontSize(9)
                        .setFontColor(COLOR_HEADER_TEXT))
                .add(new Paragraph("Sana:    " + rangeText).setFont(regular).setFontSize(9)
                        .setFontColor(COLOR_HEADER_TEXT))
                .add(new Paragraph("Jami:    " + total + " qayd").setFont(regular).setFontSize(9)
                        .setFontColor(COLOR_HEADER_TEXT))
                .setBorder(Border.NO_BORDER)
                .setPadding(10)
                .setTextAlignment(TextAlignment.RIGHT);

        header.addCell(left);
        header.addCell(right);
        doc.add(header);
    }

    private void addEmployeeHeader(Document doc, PdfFont bold, PdfFont regular,
                                   EmployeeTimelineResponse emp, String dateRange) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{15, 85}))
                .useAllAvailableWidth()
                .setBackgroundColor(COLOR_HEADER_BG)
                .setBorder(Border.NO_BORDER)
                .setMarginBottom(8);

        String initial = emp.getEmployeeName() != null && !emp.getEmployeeName().isEmpty()
                ? String.valueOf(emp.getEmployeeName().charAt(0)).toUpperCase() : "?";

        Cell avatarCell = new Cell()
                .add(new Paragraph(initial)
                        .setFont(bold).setFontSize(28)
                        .setFontColor(new DeviceRgb(74, 222, 128))
                        .setTextAlignment(TextAlignment.CENTER))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(Border.NO_BORDER)
                .setPadding(14)
                .setBackgroundColor(new DeviceRgb(10, 60, 30));

        String rangeText = dateRange != null && !dateRange.isBlank() ? dateRange : "Barcha sanalar";

        Cell infoCell = new Cell()
                .add(new Paragraph("AGROBANK — HODIM TIMELINE").setFont(bold).setFontSize(8)
                        .setFontColor(new DeviceRgb(180, 220, 160)))
                .add(new Paragraph(emp.getEmployeeName()).setFont(bold).setFontSize(16)
                        .setFontColor(COLOR_HEADER_TEXT))
                .add(new Paragraph(emp.getDepartmentName()).setFont(regular).setFontSize(10)
                        .setFontColor(new DeviceRgb(180, 200, 220)))
                .add(new Paragraph("Sana oralig'i: " + rangeText).setFont(regular).setFontSize(8)
                        .setFontColor(new DeviceRgb(150, 180, 200)))
                .setBorder(Border.NO_BORDER)
                .setPadding(10);

        header.addCell(avatarCell);
        header.addCell(infoCell);
        doc.add(header);
    }

    // ════════════════════════════════════════════════════════════════
    //  Stats row (single employee)
    // ════════════════════════════════════════════════════════════════

    private Table buildStatsRow(EmployeeTimelineResponse emp, PdfFont bold, PdfFont regular) {
        long totalWork = 0, totalBreak = 0;
        int  lateDays  = 0;

        for (var dr : emp.getDateResponses()) {
            for (var iv : dr.getIntervalsResponses()) {
                long dur = minutesBetween(iv.getStart(), iv.getEnd());
                if ("work".equalsIgnoreCase(iv.getType()))       totalWork  += dur;
                else if ("break".equalsIgnoreCase(iv.getType())) totalBreak += dur;
            }
        }

        for (var dr : emp.getDateResponses()) {
            boolean isLate = dr.getIntervalsResponses().stream()
                    .filter(iv -> "work".equalsIgnoreCase(iv.getType()))
                    .map(iv -> iv.getStart())
                    .filter(t -> t != null)
                    .min(java.util.Comparator.naturalOrder())
                    .map(t -> t.isAfter(LocalTime.of(9, 0)))
                    .orElse(false);
            if (isLate) lateDays++;
        }

        int totalDays = emp.getDateResponses().size();

        Table stats = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .useAllAvailableWidth()
                .setMarginBottom(4);

        stats.addCell(statCell("Jami ish vaqti", formatDuration(totalWork),  COLOR_WORK,  bold, regular));
        stats.addCell(statCell("Tanaffus",        formatDuration(totalBreak), COLOR_BREAK, bold, regular));
        stats.addCell(statCell("Ish kunlari",     totalDays + " kun",         new DeviceRgb(99, 102, 241), bold, regular));
        stats.addCell(statCell("Kechikish",       lateDays  + " kun",         COLOR_LATE_DOT, bold, regular));

        return stats;
    }

    private Cell statCell(String label, String value, DeviceRgb accent,
                          PdfFont bold, PdfFont regular) {
        return new Cell()
                .add(new Paragraph(value).setFont(bold).setFontSize(14).setFontColor(accent))
                .add(new Paragraph(label).setFont(regular).setFontSize(8)
                        .setFontColor(new DeviceRgb(150, 150, 150)))
                .setPadding(8)
                .setMargin(3)
                .setBorder(new SolidBorder(accent, 1f))
                .setTextAlignment(TextAlignment.CENTER);
    }

    // ════════════════════════════════════════════════════════════════
    //  Ruler as repeating table header cells
    // ════════════════════════════════════════════════════════════════

    /** All-employees (5-column table) */
    private void addRulerAsHeaderCells(Table table, PdfFont bold) {
        for (int i = 0; i < 4; i++) {
            table.addHeaderCell(new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setBackgroundColor(COLOR_HEADER_BG)
                    .setPadding(2));
        }
        table.addHeaderCell(new Cell()
                .add(buildTickTable(bold))
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(COLOR_HEADER_BG)
                .setPadding(0));
    }

    /** Single-employee (3-column table) */
    private void addEmployeeRulerAsHeaderCells(Table table, PdfFont bold) {
        table.addHeaderCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(COLOR_HEADER_BG)
                .setPadding(2));
        table.addHeaderCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(COLOR_HEADER_BG)
                .setPadding(2));
        table.addHeaderCell(new Cell()
                .add(buildTickTable(bold))
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(COLOR_HEADER_BG)
                .setPadding(0));
    }

    /** Shared hour-tick sub-table used by both rulers */
    private Table buildTickTable(PdfFont bold) {
        Table ticks = new Table(UnitValue.createPercentArray(buildTickPercents()))
                .useAllAvailableWidth();

        for (int h = 0; h <= 24; h++) {
            boolean isMajor = (h % 6 == 0);
            boolean isMinor = (h % 3 == 0);

            ticks.addCell(new Cell()
                    .add(new Paragraph(String.format("%02d", h))
                            .setFont(bold)
                            .setFontSize(isMajor ? 7f : isMinor ? 6f : 5f)
                            .setFontColor(isMajor
                                    ? new DeviceRgb(255, 255, 255)
                                    : isMinor
                                      ? new DeviceRgb(200, 220, 200)
                                      : new DeviceRgb(150, 170, 150))
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(COLOR_HEADER_BG)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(1));
        }
        return ticks;
    }

    private float[] buildTickPercents() {
        float[] pcts = new float[25];
        for (int i = 0; i < 25; i++) pcts[i] = 1f / 25f * 100f;
        return pcts;
    }

    // ════════════════════════════════════════════════════════════════
    //  All-employees: add row cells directly to main table
    // ════════════════════════════════════════════════════════════════

    private void addAttendanceRowCells(Table table, TimelineResponse tl, int idx,
                                       boolean alt, PdfFont bold, PdfFont regular) {
        DeviceRgb bg = alt ? COLOR_ROW_ALT : new DeviceRgb(255, 255, 255);

        table.addCell(dataCell(String.valueOf(idx), regular, 8, bg, false));
        table.addCell(dataCell(tl.getName(),        bold,    8, bg, false));
        table.addCell(dataCell(tl.getDepartment(),  regular, 7, bg, false));
        table.addCell(dataCell(formatDateShort(tl.getDate()), regular, 7, bg, false));

        Cell barCell = new Cell()
                .setBackgroundColor(bg)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COLOR_GRID, 0.5f))
                .setPaddingTop(7).setPaddingBottom(7)
                .setPaddingLeft(2).setPaddingRight(2);
        barCell.setNextRenderer(new TimelineBarRenderer(barCell, tl.getIntervals()));
        table.addCell(barCell);
    }

    // ════════════════════════════════════════════════════════════════
    //  Single-employee: add date-row cells directly to main table
    // ════════════════════════════════════════════════════════════════

    private void addEmployeeDateRowCells(Table table,
                                         EmployeeTimelineResponse.EmployeeTimelineDateResponse dr,
                                         boolean alt, PdfFont bold, PdfFont regular) {

        DeviceRgb bg = alt ? COLOR_ROW_ALT : new DeviceRgb(255, 255, 255);

        long workMin = 0, breakMin = 0;
        for (var iv : dr.getIntervalsResponses()) {
            long dur = minutesBetween(iv.getStart(), iv.getEnd());
            if ("work".equalsIgnoreCase(iv.getType()))       workMin  += dur;
            else if ("break".equalsIgnoreCase(iv.getType())) breakMin += dur;
        }
        boolean isLate = isLateEmployee(dr.getIntervalsResponses());

        table.addCell(dataCell(formatDateShort(dr.getDate().toString()), bold, 8, bg, isLate));
        table.addCell(dataCell(formatDuration(workMin) + " / " + formatDuration(breakMin),
                regular, 7, bg, false));

        List<IntervalDto> intervals = dr.getIntervalsResponses().stream()
                .map(iv -> new IntervalDto(iv.getStart(), iv.getEnd(), iv.getType()))
                .toList();

        Cell barCell = new Cell()
                .setBackgroundColor(bg)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COLOR_GRID, 0.5f))
                .setPaddingTop(7).setPaddingBottom(7)
                .setPaddingLeft(2).setPaddingRight(2);
        barCell.setNextRenderer(new TimelineBarRenderer(barCell, intervals));
        table.addCell(barCell);
    }

    // ════════════════════════════════════════════════════════════════
    //  Interval detail table (single employee)
    // ════════════════════════════════════════════════════════════════

    private Table buildIntervalTable(
            List<EmployeeTimelineResponse.EmployeeTimelineDateResponse> dates,
            PdfFont bold, PdfFont regular) {

        Table wrapper = new Table(1).useAllAvailableWidth();
        wrapper.addCell(new Cell()
                .add(new Paragraph("INTERVALLAR JADVALI")
                        .setFont(bold).setFontSize(9).setFontColor(COLOR_HEADER_TEXT))
                .setBackgroundColor(COLOR_HEADER_BG)
                .setBorder(Border.NO_BORDER)
                .setPadding(6));

        Table table = new Table(UnitValue.createPercentArray(new float[]{20, 18, 16, 16, 30}))
                .useAllAvailableWidth();

        String[] headers = {"Sana", "Tur", "Boshlanish", "Tugash", "Davomiyligi"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(8).setFontColor(COLOR_HEADER_TEXT))
                    .setBackgroundColor(COLOR_HEADER_BG)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(5));
        }

        int rowIdx = 0;
        for (var dr : dates) {
            boolean firstRow = true;
            for (var iv : dr.getIntervalsResponses()) {
                DeviceRgb bg = (rowIdx % 2 == 0) ? new DeviceRgb(255, 255, 255) : COLOR_ROW_ALT;
                long dur     = minutesBetween(iv.getStart(), iv.getEnd());

                table.addCell(tableCell(
                        firstRow ? formatDateShort(dr.getDate().toString()) : "", regular, 8, bg));

                DeviceRgb typeColor = typeColor(iv.getType());
                Cell typeCell = new Cell()
                        .add(new Paragraph(translateType(iv.getType()))
                                .setFont(bold).setFontSize(8).setFontColor(typeColor))
                        .setBackgroundColor(bg)
                        .setBorder(Border.NO_BORDER)
                        .setBorderBottom(new SolidBorder(COLOR_GRID, 0.3f))
                        .setPadding(4);
                table.addCell(typeCell);

                table.addCell(tableCell(fmtTime(iv.getStart()), regular, 8, bg));
                table.addCell(tableCell(fmtTime(iv.getEnd()),   regular, 8, bg));
                table.addCell(tableCell(formatDuration(dur),    regular, 8, bg));

                firstRow = false;
                rowIdx++;
            }
        }

        // Grand total footer — 5 columns: span 3 for label, 1 for work, 1 for break
        long grandWork = dates.stream()
                .flatMap(d -> d.getIntervalsResponses().stream())
                .filter(iv -> "work".equalsIgnoreCase(iv.getType()))
                .mapToLong(iv -> minutesBetween(iv.getStart(), iv.getEnd())).sum();
        long grandBreak = dates.stream()
                .flatMap(d -> d.getIntervalsResponses().stream())
                .filter(iv -> "break".equalsIgnoreCase(iv.getType()))
                .mapToLong(iv -> minutesBetween(iv.getStart(), iv.getEnd())).sum();

        table.addFooterCell(new Cell(1, 3)
                .add(new Paragraph("JAMI").setFont(bold).setFontSize(8)
                        .setFontColor(COLOR_HEADER_TEXT))
                .setBackgroundColor(COLOR_HEADER_BG).setBorder(Border.NO_BORDER).setPadding(5));
        table.addFooterCell(new Cell()
                .add(new Paragraph(formatDuration(grandWork)).setFont(bold).setFontSize(8)
                        .setFontColor(new DeviceRgb(134, 239, 172)))
                .setBackgroundColor(COLOR_HEADER_BG).setBorder(Border.NO_BORDER).setPadding(5));
        table.addFooterCell(new Cell()
                .add(new Paragraph(formatDuration(grandBreak)).setFont(bold).setFontSize(8)
                        .setFontColor(new DeviceRgb(253, 186, 116)))
                .setBackgroundColor(COLOR_HEADER_BG).setBorder(Border.NO_BORDER).setPadding(5));

        wrapper.addCell(new Cell().add(table).setBorder(Border.NO_BORDER).setPadding(0));
        return wrapper;
    }

    // ════════════════════════════════════════════════════════════════
    //  Legend
    // ════════════════════════════════════════════════════════════════

    private Table buildLegend(PdfFont regular) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34}))
                .useAllAvailableWidth()
                .setMarginTop(10);
        t.addCell(legendCell("■  Ish vaqti",      COLOR_WORK,  regular));
        t.addCell(legendCell("■  Tanaffus",        COLOR_BREAK, regular));
        t.addCell(legendCell("■  Tushlik (13–14)", COLOR_LUNCH, regular));
        return t;
    }

    private Cell legendCell(String text, DeviceRgb color, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(8).setFontColor(color))
                .setBorder(new SolidBorder(COLOR_GRID, 0.5f))
                .setPadding(5)
                .setTextAlignment(TextAlignment.CENTER);
    }

    // ════════════════════════════════════════════════════════════════
    //  Footer (page numbers + timestamp)
    // ════════════════════════════════════════════════════════════════

    private void addFooter(PdfDocument pdf, PdfFont regular, int total) throws IOException {
        int    pageCount = pdf.getNumberOfPages();
        String ts = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        for (int i = 1; i <= pageCount; i++) {
            PdfPage   page   = pdf.getPage(i);
            PdfCanvas canvas = new PdfCanvas(page);
            Rectangle rect   = page.getPageSize();

            canvas.beginText()
                    .setFontAndSize(regular, 7)
                    .moveText(rect.getLeft() + 28, rect.getBottom() + 14)
                    .showText("Agrobank — Davomat tizimi  |  Chop etilgan: " + ts +
                            "  |  Jami yozuvlar: " + total)
                    .endText();

            canvas.beginText()
                    .setFontAndSize(regular, 7)
                    .moveText(rect.getRight() - 60, rect.getBottom() + 14)
                    .showText("Sahifa " + i + " / " + pageCount)
                    .endText();

            canvas.release();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Inner renderer — draws the coloured timeline bar on a canvas
    // ════════════════════════════════════════════════════════════════

    private class TimelineBarRenderer extends com.itextpdf.layout.renderer.CellRenderer {

        private final List<IntervalDto> intervals;

        TimelineBarRenderer(Cell modelElement, List<IntervalDto> intervals) {
            super(modelElement);
            this.intervals = intervals;
        }

        @Override
        public com.itextpdf.layout.renderer.IRenderer getNextRenderer() {
            return new TimelineBarRenderer((Cell) modelElement, intervals);
        }

        @Override
        public void draw(com.itextpdf.layout.renderer.DrawContext drawContext) {
            super.draw(drawContext);

            PdfCanvas canvas = drawContext.getCanvas();
            Rectangle bounds = getOccupiedAreaBBox();

            float x      = bounds.getLeft()   + 2;
            float y      = bounds.getBottom() + 7;
            float width  = bounds.getWidth()  - 4;
            float height = BAR_HEIGHT;

            // Track background
            canvas.saveState()
                    .setFillColor(COLOR_TRACK_BG)
                    .rectangle(x, y, width, height)
                    .fill();

            // Grid lines every 3 hours
            canvas.setStrokeColor(COLOR_GRID).setLineWidth(0.3f);
            for (int h = 0; h <= 24; h += 3) {
                float gx = x + (h / 24f) * width;
                canvas.moveTo(gx, y).lineTo(gx, y + height);
            }
            canvas.stroke();

            // Segments
            for (IntervalDto iv : intervals) {
                if (iv.getStart() == null || iv.getEnd() == null) continue;
                int startMin = toMin(iv.getStart());
                int endMin   = toMin(iv.getEnd());
                if (endMin <= startMin) continue;

                float segX = x + (startMin / 1440f) * width;
                float segW = Math.max(((endMin - startMin) / 1440f) * width, 1f);

                DeviceRgb color = switch (iv.getType().toLowerCase()) {
                    case "work"  -> COLOR_WORK;
                    case "break" -> COLOR_BREAK;
                    case "lunch" -> COLOR_LUNCH;
                    default      -> COLOR_GRID;
                };

                canvas.saveState()
                        .setFillColor(color)
                        .roundRectangle(segX, y, segW, height, 2)
                        .fill()
                        .restoreState();
            }

            canvas.restoreState();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════

    private Cell dataCell(String text, PdfFont font, float size,
                          DeviceRgb bg, boolean red) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(size)
                        .setFontColor(red ? COLOR_LATE_DOT : new DeviceRgb(0, 0, 0)))
                .setBackgroundColor(bg)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COLOR_GRID, 0.3f))
                .setPadding(4)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell tableCell(String text, PdfFont font, float size, DeviceRgb bg) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(size))
                .setBackgroundColor(bg)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COLOR_GRID, 0.3f))
                .setPadding(4);
    }

    private boolean isLateEmployee(
            List<EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse> ivs) {
        return ivs.stream()
                .filter(iv -> "work".equalsIgnoreCase(iv.getType()))
                .map(EmployeeTimelineResponse.EmployeeTimelineIntervalsResponse::getStart)
                .filter(t -> t != null)
                .min(java.util.Comparator.naturalOrder())
                .map(t -> t.isAfter(LocalTime.of(9, 0)))
                .orElse(false);
    }

    private int toMin(LocalTime t) {
        return t == null ? 0 : t.getHour() * 60 + t.getMinute();
    }

    private long minutesBetween(LocalTime start, LocalTime end) {
        if (start == null || end == null) return 0;
        return Math.max(java.time.Duration.between(start, end).toMinutes(), 0);
    }

    private String fmtTime(LocalTime t) {
        return t == null ? "" : t.toString().substring(0, 5);
    }

    private String formatDuration(long minutes) {
        long h = minutes / 60, m = minutes % 60;
        if (h == 0) return m + "m";
        return m == 0 ? h + "s" : h + "s " + m + "m";
    }

    private String translateType(String type) {
        if (type == null) return "";
        return switch (type.toLowerCase()) {
            case "work"  -> "Ish";
            case "break" -> "Tanaffus";
            case "lunch" -> "Tushlik";
            default      -> type;
        };
    }

    private DeviceRgb typeColor(String type) {
        if (type == null) return new DeviceRgb(150, 150, 150);
        return switch (type.toLowerCase()) {
            case "work"  -> COLOR_WORK;
            case "break" -> COLOR_BREAK;
            case "lunch" -> COLOR_LUNCH;
            default      -> new DeviceRgb(150, 150, 150);
        };
    }

    private String formatDateShort(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return "";
        try {
            String[] parts  = dateStr.split("-");
            String[] months = {"Yan","Feb","Mar","Apr","May","Iyn","Iyl","Avg","Sen","Okt","Noy","Dek"};
            return parts[2] + " " + months[Integer.parseInt(parts[1]) - 1] + " " + parts[0];
        } catch (Exception e) { return dateStr; }
    }
}