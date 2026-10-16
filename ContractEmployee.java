package model;

import exception.InvalidSalaryException;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Employee hired for a fixed contract.
 * The agreed contract pay is stored in the parent's baseSalary field.
 *
 * Gross      = contract pay + bonus
 * Deductions = 10% of gross (sample flat withholding rule)
 * Net        = gross - deductions
 *
 * These are sample project rules, NOT official tax regulations.
 */
public class ContractEmployee extends Employee {

    public static final double DEDUCTION_RATE = 0.10;

    private final double bonus;

    public ContractEmployee(int id, String name, String department, LocalDate joiningDate,
                            double contractPay, double bonus) throws InvalidSalaryException {
        super(id, name, department, joiningDate, contractPay);
        this.bonus = requireValidAmount("Bonus", bonus);
    }

    /** No bonus. */
    public ContractEmployee(int id, String name, String department, LocalDate joiningDate,
                            double contractPay) throws InvalidSalaryException {
        this(id, name, department, joiningDate, contractPay, 0.0);
    }

    public double getContractPay() {
        return getBaseSalary();
    }

    public double getBonus() {
        return bonus;
    }

    @Override
    public double calculateGrossSalary() {
        return round2(getContractPay() + bonus);
    }

    @Override
    public double calculateDeductions() {
        return round2(calculateGrossSalary() * DEDUCTION_RATE);
    }

    @Override
    public String getEmployeeType() {
        return "Contract";
    }

    @Override
    public String describeSalaryRule() {
        return "Gross = contract pay + bonus; Deductions = 10% of gross; Net = gross - deductions";
    }

    @Override
    public String toString() {
        return super.toString() + String.format(Locale.US, " {contractPay=%.2f, bonus=%.2f}", getContractPay(), bonus);
    }
}
