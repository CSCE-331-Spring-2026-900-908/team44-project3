package team44.project2.service.employee;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import team44.project2.model.Employee;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class responsible for CRUD operations on employee records, providing methods
 * to retrieve all employees, add new employees, update existing ones, and delete them.
 */
@ApplicationScoped
public class EmployeeService {

    private static final Logger LOG = Logger.getLogger(EmployeeService.class);

    @Inject
    DataSource dataSource;
    //sql queries for employee operations
    private static final String GET_ALL_EMPLOYEES = """
        SELECT employee_id,
               first_name,
               last_name,
               role,
               start_date,
               email,
               password_hash,
               is_active
        FROM employees
        ORDER BY last_name, first_name
        """;

    private static final String INSERT_EMPLOYEE = """
        INSERT INTO employees
        (first_name, last_name, role, start_date, email, password_hash, is_active)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        RETURNING employee_id
        """;

    private static final String UPDATE_EMPLOYEE = """
        UPDATE employees
        SET first_name = ?,
            last_name = ?,
            role = ?,
            start_date = ?,
            email = ?,
            password_hash = ?,
            is_active = ?
        WHERE employee_id = ?
        """;

    private static final String UPDATE_EMPLOYEE_NO_PASSWORD = """
        UPDATE employees
        SET first_name = ?,
            last_name = ?,
            role = ?,
            start_date = ?,
            email = ?,
            is_active = ?
        WHERE employee_id = ?
        """;

    private static final String DELETE_EMPLOYEE = """
        DELETE FROM employees
        WHERE employee_id = ?
        """;

    /**
     * Fetches all employees from the database, ordered by last name then first name.
     *
     * @return A list of all {@link team44.project2.model.Employee} records; empty if
     *         none exist or a database error occurs.
     */
    public List<Employee> getAllEmployees() {

        List<Employee> employees = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_ALL_EMPLOYEES);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                employees.add(mapEmployee(rs));
            }

        } catch (SQLException e) {
            LOG.error("Error fetching employees", e);
        }

        return employees;
    }

    public Employee addEmployee(Employee employee) {
        String hashedPassword = hashPassword(employee.passwordHash());

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_EMPLOYEE)) {

            ps.setString(1, employee.firstName());
            ps.setString(2, employee.lastName());
            ps.setString(3, employee.role());
            ps.setDate(4, Date.valueOf(employee.startDate()));
            ps.setString(5, employee.email());
            ps.setString(6, hashedPassword);
            ps.setBoolean(7, employee.isActive());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Employee(rs.getInt("employee_id"), employee.firstName(),
                        employee.lastName(), employee.role(), employee.startDate(),
                        employee.email(), hashedPassword, employee.isActive());
            }
        } catch (SQLException e) {
            LOG.error("Error inserting employee", e);
            throw new RuntimeException("Failed to create employee", e);
        }
        throw new RuntimeException("Failed to create employee - no ID returned");
    }

    public Employee updateEmployee(Employee employee) {
        boolean changePassword = employee.passwordHash() != null && !employee.passwordHash().isEmpty();

        try (Connection conn = dataSource.getConnection()) {
            if (changePassword) {
                String hashedPassword = hashPassword(employee.passwordHash());
                try (PreparedStatement ps = conn.prepareStatement(UPDATE_EMPLOYEE)) {
                    ps.setString(1, employee.firstName());
                    ps.setString(2, employee.lastName());
                    ps.setString(3, employee.role());
                    ps.setDate(4, Date.valueOf(employee.startDate()));
                    ps.setString(5, employee.email());
                    ps.setString(6, hashedPassword);
                    ps.setBoolean(7, employee.isActive());
                    ps.setInt(8, employee.employeeId());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(UPDATE_EMPLOYEE_NO_PASSWORD)) {
                    ps.setString(1, employee.firstName());
                    ps.setString(2, employee.lastName());
                    ps.setString(3, employee.role());
                    ps.setDate(4, Date.valueOf(employee.startDate()));
                    ps.setString(5, employee.email());
                    ps.setBoolean(6, employee.isActive());
                    ps.setInt(7, employee.employeeId());
                    ps.executeUpdate();
                }
            }

            return new Employee(employee.employeeId(), employee.firstName(),
                    employee.lastName(), employee.role(), employee.startDate(),
                    employee.email(), "", employee.isActive());
        } catch (SQLException e) {
            LOG.error("Error updating employee", e);
            throw new RuntimeException("Failed to update employee", e);
        }
    }

    public void deleteEmployee(int employeeId) {

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_EMPLOYEE)) {

            ps.setInt(1, employeeId);
            ps.executeUpdate();

        } catch (SQLException e) {
            LOG.error("Error deleting employee", e);
            throw new RuntimeException("Failed to delete employee", e);
        }
    }

    private Employee mapEmployee(ResultSet rs) throws SQLException {
        java.sql.Date sqlDate = rs.getDate("start_date");
        java.time.LocalDate startDate = (sqlDate != null) ? sqlDate.toLocalDate() : null;

        return new Employee(
                rs.getInt("employee_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("role"),
                startDate,
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getBoolean("is_active")
        );
    }

    private String hashPassword(String plainPassword) {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        String salt = bytesToHex(saltBytes);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((salt + plainPassword).getBytes(StandardCharsets.UTF_8));
            return salt + ":" + bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}