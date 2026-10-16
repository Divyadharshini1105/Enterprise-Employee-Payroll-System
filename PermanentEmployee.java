package model;

import exception.InvalidSalaryException;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Full-time employee.
 *
 * Gross      = base salary + bonus + allowances
 * Deductions = 20% of gross  (sample rule: 12% retirement fund + 8% income tax)
 * Net        = gross - deductions
 *
 * These are sample project rules, NOT official tax regulations.
 */
public class PermanentEmployee extends Employee {

    /** 12% retirement + 8% tax = 20% (sample rule). */
    public static final double DEDUCTION_RATE = 0.20;

    private final double bonus;
    private final double allowances;

    public PermanentEmployee(int id, String name, String department, LocalDate joiningDate,
                             double baseSalary, double bonus, double allowances)
            throws InvalidSalaryException {
        super(id, name, department, joiningDate, baseSalary); // validates the common fields
        this.bonus = requireValidAmount("Bonus", bonus);
        this.allowances = requireValidAmount("Allowances", allowances);
    }

    /** No bonus and no allowances. this(...) reuses the constructor above. */
    public PermanentEmployee(int id, String name, String department, LocalDate joiningDate,
                             double baseSalary) throws InvalidSalaryException {
        this(id, name, department, joiningDate, baseSalary, 0.0, 0.0);
    }

    public double getBonus() {
        return bonus;
    }

    public double getAllowances() {
        return allowances;
    }

    @Override
    public double calculateGrossSalary() {
        return round2(getBaseSalary() + bonus + allowances);
    }

    @Override
    public double calculateDeductions() {
        return round2(calculateGrossSalary() * DEDUCTION_RATE);
    }

    @Override
    public String getEmployeeType() {
        return "Permanent";
    }

    @Override
    public String describeSalaryRule() {
        return "Gross = base + bonus + allowances; Deductions = 20% of gross; Net = gross - deductions";
    }

    @Override
    public String toString() {
        return super.toString() + String.format(Locale.US, " {bonus=%.2f, allowances=%.2f}", bonus, allowances);
    }
}
