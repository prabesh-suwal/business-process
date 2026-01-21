package com.enterprise.person.dto;

import com.enterprise.person.entity.Person;
import com.enterprise.person.entity.Person.Address;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePersonRequest {

    // Identity
    private Person.Salutation salutation;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private LocalDate dateOfBirth;
    private Person.Gender gender;

    // Identifiers
    private String citizenshipNumber;
    private String nationalId;
    private String passportNumber;
    private String panNumber;
    private String photoUrl;

    // Contact
    private String primaryPhone;
    private String secondaryPhone;
    private String email;
    private Address currentAddress;
    private Address permanentAddress;

    // Employment
    private Person.OccupationType occupationType;
    private String employerName;
    private String designation;
    private BigDecimal monthlyIncome;
    private BigDecimal annualIncome;
    private Map<String, Object> employmentDetails;

    // Registration
    private UUID branchId;
}
