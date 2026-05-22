package uz.agrobank.attendance_control.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.agrobank.attendance_control.dto.CreateDepartmentHeadRequest;
import uz.agrobank.attendance_control.dto.DepartHeadResponse;
import uz.agrobank.attendance_control.service.DepartmentHeadService;

@RestController
@AllArgsConstructor
@RequestMapping("/heads")
public class DepartmentHeadController {

    private final DepartmentHeadService departmentHeadService;

    @GetMapping("/getAllByFilter")
    public Page<DepartHeadResponse> getAllDepHeads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long departmentId
    ) {
        return departmentHeadService.getAllDepHeads(query, departmentId, page, size);
    }

    @PostMapping("/create")
    public ResponseEntity<String> create(
            @RequestBody CreateDepartmentHeadRequest request
    ) {
        String result = departmentHeadService.create(request);
        if ("USERNAME_EXISTS".equals(result))
            return ResponseEntity.badRequest().body("Bu username allaqachon mavjud");
        return ResponseEntity.ok("Bo'lim boshlig'i muvaffaqiyatli qo'shildi");
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        String result = departmentHeadService.delete(id);
        if ("NOT_FOUND".equals(result))
            return ResponseEntity.notFound().build();
        if ("FORBIDDEN".equals(result))
            return ResponseEntity.badRequest().body("Faqat bo'lim boshliqlari o'chirilishi mumkin");
        return ResponseEntity.ok("Muvaffaqiyatli o'chirildi");
    }
}