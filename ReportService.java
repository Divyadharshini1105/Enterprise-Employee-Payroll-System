package service;

import model.Employee;
import repository.EmployeeRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Prints every report in a table-like console format.
 * Money is always shown with two decimals.
 */
public class ReportService {

    private static final String EMPLOYEE_LINE = "-".repeat(77);
    private static final String PAYROLL_LINE = "-".repeat(91);

    private final EmployeeRepository<Employee> repository;
    private final PayrollService payrollService;
    private final ValidationService validationService;

    public ReportService(EmployeeRepository<Employee> repository, PayrollService payrollService,
                         ValidationService validationService) {
        this.repository = repository;
        this.payrollService = payrollService;
        this.validationService = validationService;
    }

    /** 1234.5 becomes "1,234.50". Locale.US keeps the format the same on every computer. */
    public static String money(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    // ------------------------------------------------------------------
    // Employee lists
    // ------------------------------------------------------------------

    public void printAllEmployees() {
        printEmployeeTable("All Employees (" + repository.size() + ")", repository.getAll());
    }

    public void printEmployeeTable(String title, List<? extends Employee> employees) {
        System.out.println();
        System.out.println("=== " + title + " ===");
        if (employees.isEmpty()) {
            System.out.println("No employees found.");
            return;
        }
        System.out.println(EMPLOYEE_LINE);
        System.out.printf("%-6s %-10s %-18s %-12s %-12s %14s%n",
                "ID", "Type", "Name", "Department", "Joined", "Base Salary");
        System.out.println(EMPLOYEE_LINE);
        for (Employee e : employees) {
            System.out.printf("%-6s %-10s %-18.18s %-12.12s %-12s %14s%n",
                    e.getId(), e.getEmployeeType(), e.getName(), e.getDepartment(),
                    e.getJoiningDate(), money(e.getBaseSalary()));
        }
        System.out.println(EMPLOYEE_LINE);
    }

    /** Groups employees by department. TreeMap keeps the departments in alphabetical order. */
    public TreeMap<String, List<Employee>> groupByDepartment() {
        TreeMap<String, List<Employee>> byDepartment = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Employee e : repository.getAll()) {
            byDepartment.computeIfAbsent(e.getDepartment(), key -> new ArrayList<>()).add(e);
        }
        return byDepartment;
    }

    public void printDepartmentReport() {
        TreeMap<String, List<Employee>> byDepartment = groupByDepartment();
        if (byDepartment.isEmpty()) {
            System.out.println();
            System.out.println("=== Department-wise Report ===");
            System.out.println("No employees found.");
            return;
        }
        for (Map.Entry<String, List<Employee>> entry : byDepartment.entrySet()) {
            printEmployeeTable("Department: " + entry.getKey() + " (" + entry.getValue().size() + ")",
                    entry.getValue());
        }
    }

    // ------------------------------------------------------------------
    // Payroll tables
    // ------------------------------------------------------------------

    public void printSorted(boolean ascending) {
        List<PayrollRecord> records = payrollService.sortByNetSalary(ascending);
        printPayrollTable("Employees sorted by net salary (" + (ascending ? "lowest first" : "highest first") + ")",
                records, false);
    }

    public void printRanking() {
        printPayrollTable("Employee Ranking by Net Salary (equal salaries share a rank)",
                payrollService.rankByNetSalary(), true);
    }

    private void printPayrollTable(String title, Collection<PayrollRecord> records, boolean showRank) {
        System.out.println();
        System.out.println("=== " + title + " ===");
        if (records.isEmpty()) {
            System.out.println("No payroll records available.");
            return;
        }
        String prefix = showRank ? String.format("%-5s ", "Rank") : "";
        String line = showRank ? "-".repeat(97) : PAYROLL_LINE;
        System.out.println(line);
        System.out.printf("%s%-6s %-10s %-18s %-12s %13s %13s %13s%n", prefix,
                "ID", "Type", "Name", "Department", "Gross", "Deductions", "Net");
        System.out.println(line);

        int position = 0;
        int rank = 0;
        double previousNet = Double.NaN;
        for (PayrollRecord r : records) {
            position++;
            String rankText = "";
            if (showRank) {
                // Same net salary as the previous employee means the same rank (1, 2, 2, 4 ...).
                if (position == 1 || Double.compare(r.getNetSalary(), previousNet) != 0) {
                    rank = position;
                }
                previousNet = r.getNetSalary();
                rankText = String.format("%-5s ", rank);
            }
            Employee e = r.getEmployee();
            System.out.printf("%s%-6s %-10s %-18.18s %-12.12s %13s %13s %13s%n", rankText,
                    e.getId(), e.getEmployeeType(), e.getName(), e.getDepartment(),
                    money(r.getGrossSalary()), money(r.getDeductions()), money(r.getNetSalary()));
        }
        System.out.println(line);
    }

    public void printHighestAndLowest() {
        List<PayrollRecord> sorted = payrollService.sortByNetSalary(false);
        System.out.println();
        if (sorted.isEmpty()) {
            System.out.println("=== Highest and Lowest Paid ===");
            System.out.println("No payroll records available.");
            return;
        }
        double highestNet = sorted.get(0).getNetSalary();
        double lowestNet = sorted.get(sorted.size() - 1).getNetSalary();

        List<PayrollRecord> highest = new ArrayList<>();
        List<PayrollRecord> lowest = new ArrayList<>();
        for (PayrollRecord r : sorted) {
            if (Double.compare(r.getNetSalary(), highestNet) == 0) {
                highest.add(r);
            }
            if (Double.compare(r.getNetSalary(), lowestNet) == 0) {
                lowest.add(r);
            }
        }
        printPayrollTable("Highest Paid (net " + money(highestNet) + ")", highest, false);
        printPayrollTable("Lowest Paid (net " + money(lowestNet) + ")", lowest, false);
    }

    public void printIndividualPayroll(PayrollRecord r) {
        Employee e = r.getEmployee();
        System.out.println();
        System.out.println("=== Payroll for Employee " + e.getId() + " ===");
        System.out.println("Name        : " + e.getName());
        System.out.println("Type        : " + e.getEmployeeType());
        System.out.println("Department  : " + e.getDepartment());
        System.out.println("Rule        : " + e.describeSalaryRule());
        System.out.println("Gross salary: " + money(r.getGrossSalary()));
        System.out.println("Deductions  : " + money(r.getDeductions()));
        System.out.println("Net salary  : " + money(r.getNetSalary()));
    }

    // ------------------------------------------------------------------
    // Totals
    // ------------------------------------------------------------------

    public void printDepartmentPayrollTotals() {
        printDepartmentTotals(payrollService.processBatch());
    }

    private void printDepartmentTotals(PayrollBatchResult result) {
        System.out.println();
        System.out.println("=== Department-wise Payroll Totals ===");
        if (result.getDepartmentTotals().isEmpty()) {
            System.out.println("No payroll records available.");
            return;
        }
        String line = "-".repeat(77);
        System.out.println(line);
        System.out.printf("%-14s %9s %16s %16s %16s%n", "Department", "Employees", "Gross", "Deductions", "Net");
        System.out.println(line);
        for (Map.Entry<String, PayrollBatchResult.DepartmentTotal> entry : result.getDepartmentTotals().entrySet()) {
            PayrollBatchResult.DepartmentTotal t = entry.getValue();
            System.out.printf("%-14.14s %9s %16s %16s %16s%n", entry.getKey(), t.getEmployeeCount(),
                    money(t.getGross()), money(t.getDeductions()), money(t.getNet()));
        }
        System.out.println(line);
    }

    public void printCompanyPayroll() {
        PayrollBatchResult result = payrollService.processBatch();
        System.out.println();
        System.out.println("=== Total Company Payroll ===");
        System.out.println("Employees processed : " + result.getProcessed().size());
        System.out.println("Records rejected    : " + result.getRejected().size());
        System.out.println("Total gross salary  : " + money(result.getTotalGross()));
        System.out.println("Total deductions    : " + money(result.getTotalDeductions()));
        System.out.println("Total net payroll   : " + money(result.getTotalNet()));
        printDepartmentTotals(result);
    }

    public void printBatchResult(PayrollBatchResult result) {
        System.out.println();
        System.out.println("=== Batch Payroll Result ===");
        System.out.println("Batches run        : " + result.getBatchesRun());
        System.out.println("Records attempted  : " + result.getAttempted());
        System.out.println("Processed OK       : " + result.getProcessed().size());
        System.out.println("Rejected           : " + result.getRejected().size());

        if (!result.getProcessed().isEmpty()) {
            printPayrollTable("Processed successfully", result.getProcessed(), false);
        }
        if (!result.getRejected().isEmpty()) {
            printIssues("Rejected in this batch", result.getRejected());
        }
        System.out.println("Total gross      : " + money(result.getTotalGross()));
        System.out.println("Total deductions : " + money(result.getTotalDeductions()));
        System.out.println("Total net payroll: " + money(result.getTotalNet()));
    }

    // ------------------------------------------------------------------
    // Validation report
    // ------------------------------------------------------------------

    public void printInvalidRecords() {
        List<ValidationIssue> issues = validationService.validateAll();
        if (issues.isEmpty()) {
            System.out.println();
            System.out.println("=== Invalid Employee Records ===");
            System.out.println("No problems found. All records are valid.");
            return;
        }
        printIssues("Invalid Employee Records (" + issues.size() + ")", issues);
    }

    private void printIssues(String title, List<ValidationIssue> issues) {
        System.out.println();
        System.out.println("=== " + title + " ===");
        for (ValidationIssue issue : issues) {
            System.out.println(" - " + issue);
        }
    }
}
