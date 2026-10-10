package com.employee.Services.IServices;

import java.util.List;

import com.employee.DTO.EmployeeDTO;

public interface IEmployeeServices {

    public EmployeeDTO createEmployee(String name);
    public List<EmployeeDTO> getAll();

}
