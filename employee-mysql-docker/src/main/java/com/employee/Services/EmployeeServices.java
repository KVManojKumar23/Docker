package com.employee.Services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.employee.DTO.EmployeeDTO;
import com.employee.Entity.Employee;
import com.employee.Repository.EmployeeRepository;
import com.employee.Services.IServices.IEmployeeServices;

@Service 
public class EmployeeServices implements IEmployeeServices {

    private final EmployeeRepository employeeRepository;

    public EmployeeServices(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    private EmployeeDTO convertEntityToDTO(Employee emp) {
        return new EmployeeDTO(emp.getEmpId(), emp.getName());
    }

    @Override
    public EmployeeDTO createEmployee(String name) {
        Employee emp = new Employee();
        emp.setName(name);
        employeeRepository.save(emp);
        return new EmployeeDTO(emp.getEmpId(), emp.getName());
    }

    @Override
    public List<EmployeeDTO> getAll() {
        List<Employee> emps = this.employeeRepository.findAll();
        return emps.stream().map(this::convertEntityToDTO).collect(Collectors.toList());
    }

}
