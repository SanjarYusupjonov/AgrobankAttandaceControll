package uz.agrobank.attendance_control.security;

import lombok.AllArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.agrobank.attendance_control.dto.AuthResponse;
import uz.agrobank.attendance_control.dto.LoginRequest;
import uz.agrobank.attendance_control.dto.RegisterRequest;
import uz.agrobank.attendance_control.entity.Department;
import uz.agrobank.attendance_control.entity.User;
import uz.agrobank.attendance_control.repository.DepartmentRepository;
import uz.agrobank.attendance_control.repository.UserRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final DepartmentRepository departmentRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final CustomUserDetailsService
            customUserDetailsService;

    public String register(RegisterRequest request) {

        if(userRepository.existsByUsername(
                request.getUsername()
        )) {

            return "Username already exists";
        }

        List<Department> departments = null;

        if(request.getDepartmentIdList() != null) {
            departments = departmentRepository.findByIdList(request.getDepartmentIdList());
        }

        User user = new User();

        user.setFullName(request.getFullName());

        user.setUsername(request.getUsername());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setType(request.getType());

        user.setDepartments(departments);

        userRepository.save(user);

        return "REGISTER SUCCESS";
    }

    public AuthResponse login(
            LoginRequest request
    ) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails userDetails =
                customUserDetailsService
                        .loadUserByUsername(
                                request.getUsername()
                        );

        String token =
                jwtService.generateToken(
                        userDetails
                );

        return new AuthResponse(token);
    }
}