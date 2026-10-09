package com.employee.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.employee.dto.EmployeeDTO;
import com.employee.services.EmployeeServices;

@RestController 
@RequestMapping("/employee")
public class EmployeeController {

    private final EmployeeServices empServices;

    public EmployeeController(EmployeeServices empServices) {
        this.empServices = empServices;
    }

    @GetMapping("/all")
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        List<EmployeeDTO> employeeDTOs = this.empServices.getAllEmployees();
        return ResponseEntity.status(HttpStatus.OK).body(employeeDTOs);
    }

}
