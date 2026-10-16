package model;

import exception.InvalidSalaryException;
import interfaces.Payable;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Abstract parent of all employee types. You cannot write "new Employee(...)";
 * you must create a PermanentEmployee, ContractEmployee or Intern.
 *
 * Every field is private and final, so once an employee object is created
 * successfully its data can never become invalid later.
 */
public abstract class Employee implements Payable {

    private final int id;
    private final String name;
    private final String department;
    private final LocalDate joiningDate;
    private final double baseSalary;

    /**
     * Main constructor. All validation happens here, so invalid data is never
     * silently accepted.
     *
     * @throws InvalidSalaryException        if the base salary is zero, negative, NaN or infinite
     * @throws IllegalArgumentException      if id, name, department or joining date is invalid
     */
    protected Employee(int id, String name, String department,
                       LocalDate joiningDate, double baseSalary) throws InvalidSalaryException {
        super(); // explicit call to Object's constructor (constructor chaining up the hierarchy)

        if (id <= 0) {
            throw new IllegalArgumentException("Employee ID must be a positive number (got " + id + ").");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee name must not be blank.");
        }
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department must not be blank.");
        }
        if (joiningDate == null) {
            throw new IllegalArgumentException("Joining date must not be missing.");
        }
        if (!(baseSalary > 0) || Double.isInfinite(baseSalary)) {
            // "!(x > 0)" is also true for NaN, so NaN is rejected too.
            throw new InvalidSalaryException("Base salary must be a positive number "
                    + "(for contract staff this is the contract pay, for interns the monthly stipend). Got: "
                    + baseSalary);
        }

        this.id = id;
        this.name = name.trim();
        this.department = toTitleCase(department); // "sALES " becomes "Sales"
        this.joiningDate = joiningDate;
        this.baseSalary = baseSalary;
    }

    /**
     * Convenience constructor: joining date defaults to today.
     * this(...) forwards the work to the main constructor above (constructor chaining).
     */
    protected Employee(int id, String name, String department, double baseSalary)
            throws InvalidSalaryException {
        this(id, name, department, LocalDate.now(), baseSalary);
    }

    // ------------------------------------------------------------------
    // Getters (no setters: objects are immutable after creation)
    // ------------------------------------------------------------------

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public double getBaseSalary() {
        return baseSalary;
    }

    // ------------------------------------------------------------------
    // Methods every subclass must provide (abstract = no body here)
    // ------------------------------------------------------------------

    /** Short label such as "Permanent", "Contract" or "Intern". */
    public abstract String getEmployeeType();

    /** Plain-English description of the salary formula used by this type. */
    public abstract String describeSalaryRule();

    // calculateGrossSalary() and calculateDeductions() come from Payable and are
    // implemented by each subclass (method overriding).

    /**
     * Net salary is the SAME for every employee type, so it is written once here.
     * "final" stops subclasses from changing the formula.
     */
    @Override
    public final double calculateNetSalary() {
        return round2(calculateGrossSalary() - calculateDeductions());
    }

    // ------------------------------------------------------------------
    // Overloaded bonus methods (same name, different parameter lists).
    // They are static so a bonus can be calculated BEFORE the employee object exists.
    // Because the number of parameters differs (1, 2, 3) the compiler can never
    // confuse them, so there is no ambiguity.
    // ------------------------------------------------------------------

    /** Fixed bonus amount, e.g. calculateBonus(2500). */
    public static double calculateBonus(double fixedAmount) {
        if (isInvalidAmount(fixedAmount)) {
            throw new IllegalArgumentException("Fixed bonus must be zero or more (got " + fixedAmount + ").");
        }
        return round2(fixedAmount);
    }

    /** Percentage of an amount, e.g. calculateBonus(10, 50000) = 10% of 50000 = 5000. */
    public static double calculateBonus(double percentage, double onAmount) {
        if (Double.isNaN(percentage) || percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Bonus percentage must be between 0 and 100 (got " + percentage + ").");
        }
        if (isInvalidAmount(onAmount)) {
            throw new IllegalArgumentException("Amount used for the percentage must be zero or more (got " + onAmount + ").");
        }
        return round2(onAmount * percentage / 100.0);
    }

    /** Percentage of an amount PLUS an extra fixed amount, e.g. calculateBonus(10, 50000, 500) = 5500. */
    public static double calculateBonus(double percentage, double onAmount, double additionalAmount) {
        return round2(calculateBonus(percentage, onAmount) + calculateBonus(additionalAmount));
    }

    // ------------------------------------------------------------------
    // Helpers shared by this class and its subclasses
    // ------------------------------------------------------------------

    /** Rounds to 2 decimal places (cents). */
    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** True if the number is NaN, infinite or negative. */
    protected static boolean isInvalidAmount(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) || value < 0;
    }

    /**
     * Used by subclass constructors to check bonus, allowance etc.
     * Returns the value if it is fine, otherwise throws the checked exception.
     */
    protected static double requireValidAmount(String label, double value) throws InvalidSalaryException {
        if (isInvalidAmount(value)) {
            throw new InvalidSalaryException(label + " must be zero or more (got " + value + ").");
        }
        return value;
    }

    private static String toTitleCase(String text) {
        String[] words = text.trim().toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s[id=%d, name=%s, department=%s, joined=%s, baseSalary=%.2f]",
                getEmployeeType(), id, name, department, joiningDate, baseSalary);
    }
}
