package uz.agrobank.attendance_control.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.agrobank.attendance_control.entity.Attendance;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query("""
        SELECT a
        FROM Attendance a
        WHERE a.department.id = :dpId
        """)
    List<Attendance> findAllByDepartment(@Param("dpName") Long dpId);

    @Query(value = """
            SELECT a
            FROM Attendance a
            WHERE a.time BETWEEN :fromDate AND :toDate
            """)
    List<Attendance> findAllByDateRange(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = """
            SELECT a
            FROM Attendance a
            WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%'))
            """)
    List<Attendance> searchByName(@Param("name") String name);


    @Query(value = """
        select * from attendance_info where department_id in :departmentIds
    """, nativeQuery = true)
    List<Attendance> findAllByDepartments(@Param("departmentIds") List<Long> departmentIds);
}
