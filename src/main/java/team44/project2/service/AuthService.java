package team44.project2.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import team44.project2.model.Employee;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Service class responsible for handling authentication logic, including verifying
 * user credentials against stored data in the database.
 * <p>
 * This service provides methods for authenticating users during login and can be called
 * by the LoginController to validate user credentials and retrieve employee information
 * for session management and role-based access control throughout the application.
 * * @author Serhii Honcharenko
 */
@ApplicationScoped
public class AuthService {

    // SQL query to find an active employee by their email address for authentication purposes
    private static final String FIND_ACTIVE_BY_EMAIL = """
            SELECT *
            FROM employees
            WHERE email = ?
            AND is_active = true
            """;

    @Inject
    DataSource dataSource;

    /**
     * Authenticates an employee based on their email and raw password.
     * * @param email    The email address of the employee attempting to log in.
     *
     * @param password The raw, unhashed password provided by the user.
     * @return The populated {@code Employee} object if authentication is successful;
     * {@code null} if the credentials are invalid or an error occurs.
     */
    public Employee authenticate(String email, String password) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_ACTIVE_BY_EMAIL)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (verifyPassword(password, storedHash)) {
                    return mapEmployee(rs, storedHash);
                }
            }
        } catch (Exception e) {
            Log.error("Authentication query failed", e);
        }

        return null;
    }

    /**
     * Maps the current row of a database result set to an Employee object.
     * * @param rs         The JDBC {@code ResultSet} containing the employee's data.
     *
     * @param storedHash The hashed password retrieved from the database.
     * @return A newly constructed {@code Employee} instance.
     * @throws Exception If a database access error occurs or the column labels are incorrect.
     */
    private Employee mapEmployee(ResultSet rs, String storedHash) throws Exception {
        return new Employee(
                rs.getInt("employee_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("role"),
                rs.getDate("start_date") != null
                        ? rs.getDate("start_date").toLocalDate()
                        : null,
                rs.getString("email"),
                storedHash,
                rs.getBoolean("is_active")
        );
    }

    /**
     * Verifies a raw password against a stored hash and salt combination.
     * * @param rawPassword The plain text password to verify.
     *
     * @param storedHash The stored password hash string in the format "salt:expectedHash".
     * @return {@code true} if the password matches the hash; {@code false} otherwise.
     * @throws RuntimeException If the SHA-256 hashing algorithm is not available on the system.
     */
    private boolean verifyPassword(String rawPassword, String storedHash) {
        String[] parts = storedHash.split(":");
        if (parts.length != 2) return false;

        String salt = parts[0];
        String expectedHash = parts[1];

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((salt + rawPassword).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).equals(expectedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts a byte array into a hexadecimal string representation.
     * * @param bytes The array of bytes to convert.
     *
     * @return The resulting hexadecimal string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}