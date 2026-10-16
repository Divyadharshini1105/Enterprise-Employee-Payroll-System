package model;

import exception.InvalidSalaryException;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Intern paid a monthly stipend (stored in the parent's baseSalary field).
 *
 * Gross      = monthly stipend + eligible bonus
 * Deductions = 5% of gross (sample flat insurance/administration rule)
 * Net        = gross - deductions
 *
 * These are sample project rules, NOT official tax regulations.
 */
public class Intern extends Employee {

    public static final double DEDUCTION_RATE = 0.05;

    private final double eligibleBonus;

    public Intern(int id, String name, String department, LocalDate joiningDate,
                  double monthlyStipend, double eligibleBonus) throws InvalidSalaryException {
        super(id, name, department, joiningDate, monthlyStipend);
        this.eligibleBonus = requireValidAmount("Eligible bonus", eligibleBonus);
    }

    /** No bonus. */
    public Intern(int id, String name, String department, LocalDate joiningDate,
                  double monthlyStipend) throws InvalidSalaryException {
        this(id, name, department, joiningDate, monthlyStipend, 0.0);
    }

    public double getMonthlyStipend() {
        return getBaseSalary();
    }

    public double getEligibleBonus() {
        return eligibleBonus;
    }

    @Override
    public double calculateGrossSalary() {
        return round2(getMonthlyStipend() + eligibleBonus);
    }

    @Override
    public double calculateDeductions() {
        return round2(calculateGrossSalary() * DEDUCTION_RATE);
    }

    @Override
    public String getEmployeeType() {
        return "Intern";
    }

    @Override
    public String describeSalaryRule() {
        return "Gross = monthly stipend + eligible bonus; Deductions = 5% of gross; Net = gross - deductions";
    }

    @Override
    public String toString() {
        return super.toString() + String.format(Locale.US, " {stipend=%.2f, eligibleBonus=%.2f}",
                getMonthlyStipend(), eligibleBonus);
    }
}
