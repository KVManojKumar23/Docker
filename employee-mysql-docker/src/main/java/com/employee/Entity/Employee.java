package com.employee.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table (name = "employees")
@Data 
@AllArgsConstructor 
@NoArgsConstructor 
public class Employee {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int empId;
    @Column (name = "name")
    private String name;
     


}
