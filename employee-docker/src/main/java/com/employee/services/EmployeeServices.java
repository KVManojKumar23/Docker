package com.employee.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.employee.dto.EmployeeDTO;
import com.employee.services.services_interface.IEmployeeServices;

@Service
public class EmployeeServices implements IEmployeeServices {

    @Override
    public List<EmployeeDTO> getAllEmployees() {
        List<EmployeeDTO> employeeList = List.of(
                new EmployeeDTO(1, "Manoj", "emp1@example.com", "1234567890", "Mysore"),
                new EmployeeDTO(2, "Raj", "emp2@example.com", "0987654321", "Bangalore"));
        return employeeList;
    }
}
