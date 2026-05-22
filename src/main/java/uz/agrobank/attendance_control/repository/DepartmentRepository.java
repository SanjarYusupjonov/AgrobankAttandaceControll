package uz.agrobank.attendance_control.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.agrobank.attendance_control.entity.Department;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Department findByName(String name);

    @Query(value = """
        select * from department where :name is null or lower(name) ilike lower(concat('%', :name, '%'))
    """, nativeQuery = true)
    List<Department> findAllFilterByName(@Param("name") String name);

    @Query(value = """
        select * from department where id in :departmentIdList
    """, nativeQuery = true)
    List<Department> findByIdList(List<Long> departmentIdList);
}
