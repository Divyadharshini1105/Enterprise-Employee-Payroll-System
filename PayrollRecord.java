package service;

import model.Employee;

import java.util.Comparator;

/**
 * The payroll figures of ONE employee, calculated once and then reused for
 * sorting, ranking and reports (so we never recalculate the same salary again and again).
 */
public final class PayrollRecord {

    /** Lowest net salary first. Ties are broken by employee ID so nobody is treated as a duplicate. */
    public static final Comparator<PayrollRecord> BY_NET_ASC =
            Comparator.comparingDouble(PayrollRecord::getNetSalary)
                    .thenComparingInt(record -> record.getEmployee().getId());

    /** Highest net salary first. Ties are still broken by employee ID (ascending). */
    public static final Comparator<PayrollRecord> BY_NET_DESC =
            Comparator.comparingDouble(PayrollRecord::getNetSalary).reversed()
                    .thenComparingInt(record -> record.getEmployee().getId());

    private final Employee employee;
    private final double grossSalary;
    private final double deductions;
    private final double netSalary;

    // Package-private: only PayrollService creates records, after it has validated them.
    PayrollRecord(Employee employee) {
        this.employee = employee;
        this.grossSalary = employee.calculateGrossSalary();
        this.deductions = employee.calculateDeductions();
        this.netSalary = employee.calculateNetSalary();
    }

    public Employee getEmployee() {
        return employee;
    }

    public double getGrossSalary() {
        return grossSalary;
    }

    public double getDeductions() {
        return deductions;
    }

    public double getNetSalary() {
        return netSalary;
    }
}
