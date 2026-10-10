package com.employee.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.employee.DTO.EmployeeDTO;
import com.employee.Services.IServices.IEmployeeServices;

@RestController 
@RequestMapping("/employees")
public class EmployeeController {

    private final IEmployeeServices employeeServices;   
   
    public EmployeeController(IEmployeeServices employeeServices) {
        this.employeeServices = employeeServices;
    }

    @GetMapping("/all")
    public ResponseEntity<List<EmployeeDTO>> getALl() {
        List<EmployeeDTO> emps = this.employeeServices.getAll();
        return ResponseEntity.status(HttpStatus.OK).body(emps);
    }

    @GetMapping("/create/{name}")
    public ResponseEntity<EmployeeDTO> createEmployee(@PathVariable String name) {
        EmployeeDTO emp = this.employeeServices.createEmployee(name);
        return ResponseEntity.status(HttpStatus.CREATED).body(emp);
    }

}
