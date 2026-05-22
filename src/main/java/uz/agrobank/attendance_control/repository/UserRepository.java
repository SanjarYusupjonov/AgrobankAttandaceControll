package uz.agrobank.attendance_control.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.agrobank.attendance_control.entity.User;
import uz.agrobank.attendance_control.enums.UserType;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query(value = """
    SELECT DISTINCT u.*
    FROM users u
    JOIN user_departments ud ON ud.user_id = u.id
    WHERE u.type = :type
      AND (
            :query IS NULL
            OR LOWER(u.full_name) ILIKE CONCAT('%', LOWER(:query), '%')
            OR LOWER(u.username) ILIKE CONCAT('%', LOWER(:query), '%')
      )
      AND (
            :departmentId IS NULL
            OR ud.department_id = :departmentId
      )
""", nativeQuery = true)
    Page<User> findAllDepartmentHeads(
            @Param("type") String type,
            @Param("query") String query,
            @Param("departmentId") Long departmentId,
            Pageable pageable
    );
}