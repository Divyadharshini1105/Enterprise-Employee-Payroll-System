package service;

import model.Employee;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Result of one payroll run: the employees that were processed successfully,
 * the records that were rejected (with reasons), and running totals.
 * Totals are added up while the batch runs, so no extra loop is needed later.
 */
public class PayrollBatchResult {

    /** Totals for one department. */
    public static class DepartmentTotal {
        private int employeeCount;
        private double gross;
        private double deductions;
        private double net;

        public int getEmployeeCount() {
            return employeeCount;
        }

        public double getGross() {
            return Employee.round2(gross);
        }

        public double getDeductions() {
            return Employee.round2(deductions);
        }

        public double getNet() {
            return Employee.round2(net);
        }
    }

    private final List<PayrollRecord> processed = new ArrayList<>();
    private final List<ValidationIssue> rejected = new ArrayList<>();
    // TreeMap keeps departments sorted alphabetically (ignoring upper/lower case).
    private final TreeMap<String, DepartmentTotal> departmentTotals =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private double totalGross;
    private double totalDeductions;
    private double totalNet;
    private int batchesRun;
    private int attempted;

    // Package-private mutators: only PayrollService fills the result.
    void addProcessed(PayrollRecord record) {
        processed.add(record);
        totalGross += record.getGrossSalary();
        totalDeductions += record.getDeductions();
        totalNet += record.getNetSalary();

        DepartmentTotal total = departmentTotals.computeIfAbsent(
                record.getEmployee().getDepartment(), key -> new DepartmentTotal());
        total.employeeCount++;
        total.gross += record.getGrossSalary();
        total.deductions += record.getDeductions();
        total.net += record.getNetSalary();
    }

    void addRejected(ValidationIssue issue) {
        rejected.add(issue);
    }

    void incrementAttempted() {
        attempted++;
    }

    void incrementBatches() {
        batchesRun++;
    }

    /** The live list of successful records. The caller owns this result, so it may sort the list. */
    public List<PayrollRecord> getProcessed() {
        return processed;
    }

    public List<ValidationIssue> getRejected() {
        return rejected;
    }

    public TreeMap<String, DepartmentTotal> getDepartmentTotals() {
        return departmentTotals;
    }

    public double getTotalGross() {
        return Employee.round2(totalGross);
    }

    public double getTotalDeductions() {
        return Employee.round2(totalDeductions);
    }

    public double getTotalNet() {
        return Employee.round2(totalNet);
    }

    public int getBatchesRun() {
        return batchesRun;
    }

    public int getAttempted() {
        return attempted;
    }
}
