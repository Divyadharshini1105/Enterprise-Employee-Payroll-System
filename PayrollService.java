package service;

import model.Employee;
import repository.EmployeeRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * All payroll calculations live here: single payroll, batch payroll, totals,
 * sorting and ranking.
 */
public class PayrollService {

    /** How many employees are handled in one batch. */
    public static final int DEFAULT_BATCH_SIZE = 50;

    private final EmployeeRepository<Employee> repository;
    private final LocalDate payrollDate;

    public PayrollService(EmployeeRepository<Employee> repository) {
        this(repository, LocalDate.now());
    }

    /** The payroll date is a parameter so tests can use a fixed date. */
    public PayrollService(EmployeeRepository<Employee> repository, LocalDate payrollDate) {
        this.repository = repository;
        this.payrollDate = payrollDate;
    }

    public LocalDate getPayrollDate() {
        return payrollDate;
    }

    /**
     * Calculates payroll for one employee and checks that the result makes sense.
     * The amounts are calculated ONCE and stored in a PayrollRecord.
     *
     * @throws IllegalStateException if the employee is not eligible for this payroll run
     *                               or the calculated figures are invalid
     */
    public PayrollRecord calculatePayroll(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee must not be null.");
        }
        if (employee.getJoiningDate().isAfter(payrollDate)) {
            throw new IllegalStateException("Joining date " + employee.getJoiningDate()
                    + " is after the payroll date " + payrollDate + "; employee is not yet eligible for payroll.");
        }

        PayrollRecord record = new PayrollRecord(employee);

        if (Double.isNaN(record.getGrossSalary()) || Double.isInfinite(record.getGrossSalary())
                || record.getGrossSalary() <= 0) {
            throw new IllegalStateException("Gross salary is invalid: " + record.getGrossSalary());
        }
        if (Double.isNaN(record.getDeductions()) || Double.isInfinite(record.getDeductions())
                || record.getDeductions() < 0) {
            throw new IllegalStateException("Deductions are invalid: " + record.getDeductions());
        }
        if (record.getNetSalary() < 0) {
            throw new IllegalStateException("Net salary is negative (" + record.getNetSalary()
                    + "); deductions are larger than gross salary.");
        }
        return record;
    }

    /** Overload: calculate payroll by employee ID. */
    public PayrollRecord calculatePayroll(int employeeId) {
        Optional<Employee> found = repository.findById(employeeId);
        if (!found.isPresent()) {
            throw new IllegalArgumentException("No employee found with ID " + employeeId);
        }
        return calculatePayroll(found.get());
    }

    public PayrollBatchResult processBatch() {
        return processBatch(DEFAULT_BATCH_SIZE);
    }

    /**
     * Processes every employee in batches of batchSize.
     * subList() gives a VIEW of part of the list (no copy), so memory use stays small.
     * A bad record is reported in the "rejected" list and the batch continues.
     *
     * Time: O(n) - each employee is calculated once.
     * Extra space: O(n) for the result records (O(batchSize) if you only needed totals).
     */
    public PayrollBatchResult processBatch(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be at least 1 (got " + batchSize + ").");
        }

        PayrollBatchResult result = new PayrollBatchResult();
        List<Employee> all = repository.getAll();

        for (int start = 0; start < all.size(); start += batchSize) {
            int end = Math.min(start + batchSize, all.size());
            List<Employee> batch = all.subList(start, end);
            result.incrementBatches();

            for (Employee employee : batch) {
                try {
                    result.addProcessed(calculatePayroll(employee));
                } catch (RuntimeException ex) {
                    // Boundary of one record: log the problem, keep the batch running.
                    result.addRejected(new ValidationIssue(employee.getId(), "Payroll batch", ex.getMessage()));
                } finally {
                    result.incrementAttempted(); // runs for success AND failure
                }
            }
        }
        return result;
    }

    /** All processed employees sorted by net salary. O(n log n). */
    public List<PayrollRecord> sortByNetSalary(boolean ascending) {
        List<PayrollRecord> records = processBatch().getProcessed();
        records.sort(ascending ? PayrollRecord.BY_NET_ASC : PayrollRecord.BY_NET_DESC);
        return records;
    }

    /**
     * Ranks employees by net salary, highest first, using a TreeSet.
     * A TreeSet treats two items as "the same" when the comparator returns 0.
     * Our comparator breaks ties with the employee ID, so two employees with the
     * same net salary are both kept. O(n log n).
     */
    public SortedSet<PayrollRecord> rankByNetSalary() {
        TreeSet<PayrollRecord> ranked = new TreeSet<>(PayrollRecord.BY_NET_DESC);
        ranked.addAll(processBatch().getProcessed());
        return ranked;
    }
}
