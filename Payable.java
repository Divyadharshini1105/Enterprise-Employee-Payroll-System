package interfaces;

/**
 * Anything that can be paid a salary.
 * Net Salary = Gross Salary - Deductions
 *
 * Employee implements calculateNetSalary() once (as a final method), so every
 * employee type uses the same net formula and results can never be inconsistent.
 */
public interface Payable {

    double calculateGrossSalary();

    double calculateDeductions();

    double calculateNetSalary();
}
