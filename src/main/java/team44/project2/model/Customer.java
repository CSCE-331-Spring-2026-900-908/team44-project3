package team44.project2.model;

import java.time.LocalDate;
/**
 * Immutable record representing a customer of the cafe, containing personal details,
 * contact information, accumulated reward points, and the date they joined the loyalty
 * programme.
 *
 * @param customerId   The unique primary-key identifier for the customer.
 * @param firstName    The customer's given name.
 * @param lastName     The customer's family name.
 * @param phone        The customer's contact phone number.
 * @param email        The customer's e-mail address.
 * @param rewardPoints The total loyalty reward points the customer has accumulated.
 * @param joinDate     The date on which the customer enrolled in the loyalty programme.
 */
public record Customer(
        int customerId,
        String firstName,
        String lastName,
        String phone,
        String email,
        int rewardPoints,
        LocalDate joinDate
) {}