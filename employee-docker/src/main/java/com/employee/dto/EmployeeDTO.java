package com.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor 
@NoArgsConstructor 
public class EmployeeDTO {

    private long empId;
    private String empName;
    private String empEmail;
    private String empPhone;
    private String empAddress;

}
