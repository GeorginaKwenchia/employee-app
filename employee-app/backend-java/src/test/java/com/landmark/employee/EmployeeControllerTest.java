package com.landmark.employee;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerTest {

    @Autowired MockMvc mvc;
    @Autowired EmployeeRepository repo;
    ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() { repo.deleteAll(); }

    @Test
    void healthCheck() throws Exception {
        mvc.perform(get("/api/health"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("healthy"))
           .andExpect(jsonPath("$.db").value("connected"));
    }

    @Test
    void listEmployeesEmpty() throws Exception {
        mvc.perform(get("/api/employees"))
           .andExpect(status().isOk())
           .andExpect(content().json("[]"));
    }

    @Test
    void createEmployee() throws Exception {
        mvc.perform(post("/api/employees")
           .contentType(MediaType.APPLICATION_JSON)
           .content(json.writeValueAsString(Map.of(
               "name", "Jane Doe", "email", "jane@example.com",
               "role", "Engineer", "department", "Engineering"))))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").exists())
           .andExpect(jsonPath("$.name").value("Jane Doe"));
    }

    @Test
    void createEmployeeMissingFields() throws Exception {
        mvc.perform(post("/api/employees")
           .contentType(MediaType.APPLICATION_JSON)
           .content(json.writeValueAsString(Map.of("name", "Incomplete"))))
           .andExpect(status().isBadRequest());
    }

    @Test
    void createDuplicateEmail() throws Exception {
        String body = json.writeValueAsString(Map.of(
            "name", "Bob", "email", "bob@example.com",
            "role", "Manager", "department", "Sales"));
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isCreated());
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isConflict());
    }

    @Test
    void getEmployeeNotFound() throws Exception {
        mvc.perform(get("/api/employees/9999"))
           .andExpect(status().isNotFound());
    }

    @Test
    void updateEmployee() throws Exception {
        Employee emp = new Employee();
        emp.setName("Charlie"); emp.setEmail("charlie@example.com");
        emp.setRole("Junior"); emp.setDepartment("Engineering");
        emp = repo.save(emp);

        mvc.perform(put("/api/employees/" + emp.getId())
           .contentType(MediaType.APPLICATION_JSON)
           .content(json.writeValueAsString(Map.of("name", "Charlie Updated", "role", "Senior"))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.name").value("Charlie Updated"))
           .andExpect(jsonPath("$.role").value("Senior"));
    }

    @Test
    void deleteEmployee() throws Exception {
        Employee emp = new Employee();
        emp.setName("Dave"); emp.setEmail("dave@example.com");
        emp.setRole("VP"); emp.setDepartment("Finance");
        emp = repo.save(emp);

        mvc.perform(delete("/api/employees/" + emp.getId()))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.message").value("Employee deleted"));

        mvc.perform(get("/api/employees/" + emp.getId()))
           .andExpect(status().isNotFound());
    }

    @Test
    void deleteEmployeeNotFound() throws Exception {
        mvc.perform(delete("/api/employees/9999"))
           .andExpect(status().isNotFound());
    }
}
