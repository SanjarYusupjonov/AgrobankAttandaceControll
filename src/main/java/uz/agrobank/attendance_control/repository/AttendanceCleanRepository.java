package uz.agrobank.attendance_control.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.agrobank.attendance_control.entity.AttendanceInfoClean;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceCleanRepository
        extends JpaRepository<AttendanceInfoClean, Long> {

    @Query(value = """
    SELECT * FROM attendance_info_clean
    WHERE name = :name
    AND department_id = :departmentId
    AND (CAST(:fromDate AS timestamp) IS NULL OR time >= CAST(:fromDate AS timestamp))
    AND (CAST(:toDate AS timestamp) IS NULL OR time <= CAST(:toDate AS timestamp))
    ORDER BY DATE(time) DESC, time ASC
    """, nativeQuery = true)
    List<AttendanceInfoClean> findByNameAndDepartmentOrderByDateDescTimeAsc(
            @Param("name")       String name,
            @Param("departmentId") Long departmentId,
            @Param("fromDate")   LocalDateTime fromDate,
            @Param("toDate")     LocalDateTime toDate
    );

    @Query(value = """
    SELECT * FROM attendance_info_clean
    WHERE (CAST(:toDate AS timestamp) IS NULL OR time <= CAST(:toDate AS timestamp))
    AND (CAST(:fromDate AS timestamp) IS NULL OR time >= CAST(:fromDate AS timestamp))
    AND (:name IS NULL OR LOWER(name) ILIKE CONCAT('%', LOWER(:name), '%'))
    AND (:#{#userDepIds == null || #userDepIds.isEmpty()} = true OR department_id IN :userDepIds)
    AND (:departmentId IS NULL OR department_id = :departmentId)
    ORDER BY DATE(time) DESC, time ASC
    """, nativeQuery = true)
    List<AttendanceInfoClean> findByOptionalFilters(
            @Param("departmentId") Long departmentId,
            @Param("name") String name,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("userDepIds") List<Long> userDepIds
    );
}