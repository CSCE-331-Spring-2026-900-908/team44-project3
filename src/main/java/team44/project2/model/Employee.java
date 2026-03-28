package team44.project2.model;

import java.time.LocalDate;
/**
 * Immutable record representing a cafe employee, containing personal details, job role,
 * start date, email address, a salted+hashed password, and whether the account is
 * currently active.
 *
 * @param employeeId   The unique primary-key identifier for the employee.
 * @param firstName    The employee's given name.
 * @param lastName     The employee's family name.
 * @param role         The job role, e.g. {@code "CASHIER"} or {@code "MANAGER"}.
 * @param startDate    The date on which the employee started working at the cafe.
 * @param email        The employee's work e-mail address used for login.
 * @param passwordHash The salted and hashed password credential.
 * @param isActive     {@code true} if the account is active; {@code false} if disabled.
 */
public record Employee(
        int employeeId,
        String firstName,
        String lastName,
        String role,
        LocalDate startDate,
        String email,
        String passwordHash,
        boolean isActive
) {}