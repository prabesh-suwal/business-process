package com.enterprise.person.dto;

import com.enterprise.person.entity.Person;
import com.enterprise.person.entity.Person.Address;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonDTO {
    private UUID id;
    private String personCode;

    // Identity
    private Person.Salutation salutation;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
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

    // KYC
    private Person.KycStatus kycStatus;
    private LocalDateTime kycVerifiedAt;
    private List<UUID> kycDocuments;

    // Metadata
    private Boolean isActive;
    private UUID branchId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
