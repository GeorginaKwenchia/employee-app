package com.landmark.employee;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class EmployeeController {

    private final EmployeeRepository repo;
    private final DataSource dataSource;

    public EmployeeController(EmployeeRepository repo, DataSource dataSource) {
        this.repo = repo;
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try (Connection c = dataSource.getConnection()) {
            c.createStatement().execute("SELECT 1");
            return ResponseEntity.ok(Map.of("status", "healthy", "db", "connected"));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("status", "unhealthy", "db", "disconnected"));
        }
    }

    @GetMapping("/employees")
    public List<Employee> list() {
        return repo.findAllByOrderByNameAsc();
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<Employee> get(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/employees")
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        String name = body.get("name"), email = body.get("email"),
               role = body.get("role"), department = body.get("department");
        if (name == null || email == null || role == null || department == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
        }
        if (repo.findByEmail(email).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already exists"));
        }
        Employee emp = new Employee();
        emp.setName(name); emp.setEmail(email);
        emp.setRole(role); emp.setDepartment(department);
        emp.setDob(body.get("dob")); emp.setPhotoUrl(body.get("photo_url"));
        return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(emp));
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return repo.findById(id).map(emp -> {
            if (body.containsKey("name")) emp.setName(body.get("name"));
            if (body.containsKey("email")) emp.setEmail(body.get("email"));
            if (body.containsKey("role")) emp.setRole(body.get("role"));
            if (body.containsKey("department")) emp.setDepartment(body.get("department"));
            if (body.containsKey("dob")) emp.setDob(body.get("dob"));
            if (body.containsKey("photo_url")) emp.setPhotoUrl(body.get("photo_url"));
            return ResponseEntity.ok(repo.save(emp));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Employee deleted"));
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        List<Employee> all = repo.findAll();
        Map<String, Long> departments = new HashMap<>();
        for (Employee e : all) departments.merge(e.getDepartment(), 1L, Long::sum);
        Employee latest = all.stream().max(Comparator.comparing(Employee::getId)).orElse(null);
        return Map.of(
            "total_employees", all.size(),
            "departments", departments,
            "latest_hire", latest != null ? latest : "null",
            "version", "2.0.0"
        );
    }
}
