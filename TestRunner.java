package test;

import exception.InvalidSalaryException;
import main.Main;
import model.ContractEmployee;
import model.Employee;
import model.Intern;
import model.PermanentEmployee;
import repository.EmployeeRepository;
import service.PayrollBatchResult;
import service.PayrollRecord;
import service.PayrollService;
import service.ReportService;
import service.ValidationService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Simple test program (no JUnit needed).
 * Run with:  java -cp out test.TestRunner
 * Every line starts with [PASS] or [FAIL].
 */
public class TestRunner {

    private static final LocalDate PAYROLL_DATE = LocalDate.of(2025, 12, 31);

    private static int passed = 0;
    private static int failed = 0;

    /** A Runnable that is allowed to throw checked exceptions. */
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    /** An employee whose calculation is deliberately wrong (net becomes negative). */
    private static class BrokenEmployee extends Employee {
        BrokenEmployee(int id) throws InvalidSalaryException {
            super(id, "Broken Record", "Operations", LocalDate.of(2024, 1, 1), 1000);
        }

        @Override
        public double calculateGrossSalary() {
            return 100;
        }

        @Override
        public double calculateDeductions() {
            return 150;
        }

        @Override
        public String getEmployeeType() {
            return "Broken";
        }

        @Override
        public String describeSalaryRule() {
            return "Test-only: deductions are larger than gross";
        }
    }

    public static void main(String[] args) throws Exception {
        testValidEmployees();
        testSalaryCalculations();
        testBonusOverloading();
        testRuntimePolymorphism();
        testDuplicateIdRejected();
        testInvalidSalaryRejected();
        testSearchAndFilter();
        testEqualNetSalaries();
        testSortedDepartmentReport();
        testBatchPayrollWithInvalidRecords();
        testInvalidMenuInputs();
        testMenuFlowWithBadInputs();
        testEmptyCollections();
        testPayrollTotals();
        testRepositoryConsistency();

        System.out.println();
        System.out.println("==============================================");
        System.out.println("Tests passed: " + passed + "   Tests failed: " + failed);
        System.out.println("==============================================");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------
    // Test cases
    // ------------------------------------------------------------------

    private static void testValidEmployees() throws Exception {
        section("T1  One valid employee of each type");
        PermanentEmployee p = new PermanentEmployee(101, "Asha Kumar", "engineering", LocalDate.of(2022, 1, 10), 50000, 5000, 5000);
        ContractEmployee c = new ContractEmployee(102, "Ravi Shah", "Sales", LocalDate.of(2023, 3, 1), 45000, 5000);
        Intern i = new Intern(103, "Meena Iyer", "FINANCE", LocalDate.of(2024, 6, 1), 20000, 1000);
        check("Permanent created and department normalised to 'Engineering'", "Engineering".equals(p.getDepartment()));
        check("Contract created, contract pay is 45000", near(c.getContractPay(), 45000));
        check("Intern created, department normalised to 'Finance'", "Finance".equals(i.getDepartment()));
        PermanentEmployee shortForm = new PermanentEmployee(104, "Short Form", "Sales", LocalDate.of(2024, 1, 1), 30000);
        check("Constructor chaining: 5-argument constructor gives bonus 0 and allowances 0",
                near(shortForm.getBonus(), 0) && near(shortForm.getAllowances(), 0));
    }

    private static void testSalaryCalculations() throws Exception {
        section("T2  Different salary calculations");
        EmployeeRepository<Employee> repo = sampleRepo();
        Employee p = repo.findById(101).get();
        Employee c = repo.findById(102).get();
        Employee i = repo.findById(103).get();
        check("Permanent 50000+5000+5000: gross 60000, deductions 12000, net 48000",
                near(p.calculateGrossSalary(), 60000) && near(p.calculateDeductions(), 12000) && near(p.calculateNetSalary(), 48000));
        check("Contract 45000+5000: gross 50000, deductions 5000, net 45000",
                near(c.calculateGrossSalary(), 50000) && near(c.calculateDeductions(), 5000) && near(c.calculateNetSalary(), 45000));
        check("Intern 20000+1000: gross 21000, deductions 1050, net 19950",
                near(i.calculateGrossSalary(), 21000) && near(i.calculateDeductions(), 1050) && near(i.calculateNetSalary(), 19950));
    }

    private static void testBonusOverloading() {
        section("T3  Bonus method overloading");
        check("calculateBonus(2500) = 2500 (fixed)", near(Employee.calculateBonus(2500), 2500));
        check("calculateBonus(10, 50000) = 5000 (10% of 50000)", near(Employee.calculateBonus(10, 50000), 5000));
        check("calculateBonus(10, 50000, 500) = 5500 (percentage + extra)", near(Employee.calculateBonus(10, 50000, 500), 5500));
        check("calculateBonus(-5) is rejected", throwsType(IllegalArgumentException.class, () -> Employee.calculateBonus(-5)));
        check("calculateBonus(150, 1000) is rejected (percentage over 100)",
                throwsType(IllegalArgumentException.class, () -> Employee.calculateBonus(150, 1000)));
        check("calculateBonus(10, -1000) is rejected (negative amount)",
                throwsType(IllegalArgumentException.class, () -> Employee.calculateBonus(10, -1000)));
        check("calculateBonus(NaN) is rejected", throwsType(IllegalArgumentException.class, () -> Employee.calculateBonus(Double.NaN)));
    }

    private static void testRuntimePolymorphism() throws Exception {
        section("T4  Runtime polymorphism");
        List<Employee> employees = new ArrayList<>(sampleRepo().getAll());
        StringBuilder types = new StringBuilder();
        StringBuilder nets = new StringBuilder();
        for (Employee e : employees) { // e is an Employee reference; Java picks the right subclass method at run time
            types.append(e.getEmployeeType()).append(' ');
            nets.append(e.calculateNetSalary()).append(' ');
        }
        check("Same call, three behaviours: types = " + types.toString().trim(),
                "Permanent Contract Intern".equals(types.toString().trim()));
        check("Net salaries through Employee references = " + nets.toString().trim(),
                "48000.0 45000.0 19950.0".equals(nets.toString().trim()));
    }

    private static void testDuplicateIdRejected() throws Exception {
        section("T5  Duplicate employee ID rejection");
        EmployeeRepository<Employee> repo = sampleRepo();
        Employee duplicate = new Intern(101, "Copy Cat", "Sales", LocalDate.of(2024, 1, 1), 10000);
        check("Adding a second employee with ID 101 throws IllegalArgumentException",
                throwsType(IllegalArgumentException.class, () -> repo.add(duplicate)));
        check("Repository still holds exactly 3 employees", repo.size() == 3);
        check("exists(101) is true, exists(555) is false", repo.exists(101) && !repo.exists(555));
    }

    private static void testInvalidSalaryRejected() {
        section("T6  Invalid data rejection");
        LocalDate d = LocalDate.of(2024, 1, 1);
        check("Negative base salary -> InvalidSalaryException",
                throwsType(InvalidSalaryException.class, () -> new PermanentEmployee(1, "A", "Sales", d, -5000)));
        check("Zero contract pay -> InvalidSalaryException",
                throwsType(InvalidSalaryException.class, () -> new ContractEmployee(2, "B", "Sales", d, 0)));
        check("Negative stipend -> InvalidSalaryException",
                throwsType(InvalidSalaryException.class, () -> new Intern(3, "C", "Sales", d, -1)));
        check("Negative bonus -> InvalidSalaryException",
                throwsType(InvalidSalaryException.class, () -> new PermanentEmployee(4, "D", "Sales", d, 1000, -1, 0)));
        check("Negative allowances -> InvalidSalaryException",
                throwsType(InvalidSalaryException.class, () -> new PermanentEmployee(5, "E", "Sales", d, 1000, 0, -1)));
        check("NaN salary -> InvalidSalaryException",
                throwsType(InvalidSalaryException.class, () -> new Intern(6, "F", "Sales", d, Double.NaN)));
        check("Blank name -> IllegalArgumentException",
                throwsType(IllegalArgumentException.class, () -> new Intern(7, "   ", "Sales", d, 1000)));
        check("Missing department -> IllegalArgumentException",
                throwsType(IllegalArgumentException.class, () -> new Intern(8, "G", null, d, 1000)));
        check("Missing joining date -> IllegalArgumentException",
                throwsType(IllegalArgumentException.class, () -> new Intern(9, "H", "Sales", null, 1000)));
        check("ID 0 -> IllegalArgumentException",
                throwsType(IllegalArgumentException.class, () -> new Intern(0, "I", "Sales", d, 1000)));
    }

    private static void testSearchAndFilter() throws Exception {
        section("T7  Search and department filtering");
        EmployeeRepository<Employee> repo = sampleRepo();
        check("Search ID 102 finds Ravi Shah", repo.findById(102).isPresent() && "Ravi Shah".equals(repo.findById(102).get().getName()));
        check("Search ID 999 finds nothing", !repo.findById(999).isPresent());
        check("Filter 'sales' (any letter case) returns 1 employee", repo.findByDepartment("sales").size() == 1);
        check("Filter 'Legal' returns an empty list", repo.findByDepartment("Legal").isEmpty());
        check("Filter null returns an empty list", repo.findByDepartment(null).isEmpty());
    }

    private static void testEqualNetSalaries() throws Exception {
        section("T8  Employees with equal net salaries");
        EmployeeRepository<Employee> repo = sampleRepo();
        // 40000 + 10000 + 6250 = 56250 gross, 20% = 11250, net 45000 (same net as contract employee 102)
        repo.add(new PermanentEmployee(201, "Tie Breaker", "Operations", LocalDate.of(2023, 5, 5), 40000, 10000, 6250));
        PayrollService payroll = new PayrollService(repo, PAYROLL_DATE);

        SortedSet<PayrollRecord> ranking = payroll.rankByNetSalary();
        List<Integer> ids = new ArrayList<>();
        for (PayrollRecord r : ranking) {
            ids.add(r.getEmployee().getId());
        }
        check("Ranking keeps all 4 employees", ranking.size() == 4);
        check("Order is 101, 102, 201, 103 (tie on 45000 broken by ID) -> " + ids,
                ids.equals(List.of(101, 102, 201, 103)));

        TreeSet<PayrollRecord> netOnly = new TreeSet<>(Comparator.comparingDouble(PayrollRecord::getNetSalary));
        netOnly.addAll(payroll.processBatch().getProcessed());
        check("A comparator using ONLY net salary would lose one employee (size " + netOnly.size() + " instead of 4)",
                netOnly.size() == 3);
    }

    private static void testSortedDepartmentReport() throws Exception {
        section("T9  Sorted department report (TreeMap)");
        EmployeeRepository<Employee> repo = new EmployeeRepository<>();
        LocalDate d = LocalDate.of(2024, 1, 1);
        repo.add(new Intern(1, "S1", "sales", d, 10000));
        repo.add(new Intern(2, "E1", "engineering", d, 10000));
        repo.add(new Intern(3, "O1", "OPERATIONS", d, 10000));
        repo.add(new Intern(4, "F1", "Finance", d, 10000));
        repo.add(new Intern(5, "E2", "  ENGINEERING ", d, 10000));
        PayrollService payroll = new PayrollService(repo, PAYROLL_DATE);
        ReportService report = new ReportService(repo, payroll, new ValidationService(repo, payroll));
        List<String> keys = new ArrayList<>(report.groupByDepartment().keySet());
        check("Departments are in alphabetical order -> " + keys,
                keys.equals(List.of("Engineering", "Finance", "Operations", "Sales")));
        check("Engineering contains 2 employees (different letter case/spaces are merged)",
                report.groupByDepartment().get("Engineering").size() == 2);
        check("'human   RESOURCES' is stored as 'Human Resources'", titleCaseCheck());
    }

    private static void testBatchPayrollWithInvalidRecords() throws Exception {
        section("T10 Batch payroll with valid and invalid records");
        EmployeeRepository<Employee> repo = sampleRepo();
        repo.add(new Intern(301, "Future Hire", "Sales", LocalDate.of(2026, 2, 1), 10000)); // joins after payroll date
        repo.add(new BrokenEmployee(302)); // deductions larger than gross
        PayrollService payroll = new PayrollService(repo, PAYROLL_DATE);

        PayrollBatchResult result = payroll.processBatch(2); // 5 employees in batches of 2
        check("3 batches were run (2 + 2 + 1)", result.getBatchesRun() == 3);
        check("5 records attempted", result.getAttempted() == 5);
        check("3 processed, 2 rejected", result.getProcessed().size() == 3 && result.getRejected().size() == 2);
        check("Rejected IDs are 301 and 302",
                result.getRejected().get(0).getEmployeeId() == 301 && result.getRejected().get(1).getEmployeeId() == 302);
        check("Reason for 301 mentions 'not yet eligible'", result.getRejected().get(0).getReason().contains("not yet eligible"));
        check("Reason for 302 mentions 'negative'", result.getRejected().get(1).getReason().contains("negative"));
        check("Totals contain only processed employees (net 112950.00)", near(result.getTotalNet(), 112950));
        check("Batch size 0 is rejected", throwsType(IllegalArgumentException.class, () -> payroll.processBatch(0)));
    }

    private static void testInvalidMenuInputs() throws Exception {
        section("T11 Invalid menu inputs");
        String out = runMenu("abc", "99", "0", "-5", "", "14");
        check("Text 'abc' gives a friendly message", out.contains("Invalid input 'abc'"));
        check("Number 99 is rejected", out.contains("Invalid choice 99"));
        check("Number 0 is rejected", out.contains("Invalid choice 0"));
        check("Negative number -5 is rejected", out.contains("Invalid choice -5"));
        check("Empty line is rejected", out.contains("Invalid input ''"));
        check("Program kept running and exited normally with 14", out.contains("Goodbye!"));

        String eof = runMenu("abc"); // input ends without choosing 14
        check("End of input stops the program cleanly instead of crashing", eof.contains("No more input available"));
    }

    private static void testMenuFlowWithBadInputs() throws Exception {
        section("T12 Menu flow: malformed input, rejections, search, payroll, validation");
        String out = runMenu(
                "1", "abc", "401", "Test User", "engineering", "2024-13-45", "2024-01-15", "50000", "2", "10", "1000",
                "2", "402", "Bad Pay", "Sales", "2024-01-15", "-100", "0",
                "1", "401",
                "3", "403", "", "Operations", "2024-01-15", "15000", "0",
                "5", "401",
                "5", "999",
                "9", "401",
                "9", "999",
                "12",
                "7", "3",
                "10",
                "14");
        check("Malformed ID 'abc' is re-asked", out.contains("Invalid whole number 'abc'"));
        check("Malformed date is re-asked", out.contains("Invalid date '2024-13-45'"));
        check("Valid permanent employee is added", out.contains("Permanent employee 401 added successfully"));
        check("Negative contract pay is rejected", out.contains("Base salary must be a positive number"));
        check("Duplicate ID 401 is rejected", out.contains("Duplicate employee ID: 401"));
        check("Blank intern name is rejected", out.contains("Employee name must not be blank"));
        check("Search finds employee 401", out.contains("Search result for ID 401"));
        check("Search for 999 says not found", out.contains("No employee found with ID 999."));
        check("Individual payroll: 50000 + 10% bonus (5000) + 1000 = 56000 gross, net 44,800.00",
                out.contains("Gross salary: 56,000.00") && out.contains("Net salary  : 44,800.00"));
        check("Payroll for unknown ID shows an error and the menu continues", out.contains("Error: No employee found with ID 999"));
        check("Validation report lists rejected 402, 401 (duplicate) and 403",
                out.contains("Employee ID 402") && out.contains("Employee ID 403") && out.contains("Duplicate employee ID: 401"));
        check("Invalid sort order is reported", out.contains("Invalid sort order 3"));
        check("Batch payroll ran", out.contains("Batch Payroll Result") && out.contains("Processed OK       : 1"));
    }

    private static void testEmptyCollections() throws Exception {
        section("T13 Empty employee collections");
        EmployeeRepository<Employee> repo = new EmployeeRepository<>();
        PayrollService payroll = new PayrollService(repo, PAYROLL_DATE);
        ValidationService validation = new ValidationService(repo, payroll);
        ReportService report = new ReportService(repo, payroll, validation);

        check("Repository is empty", repo.isEmpty() && repo.getAll().isEmpty());
        check("All-employees report says no employees", capture(report::printAllEmployees).contains("No employees found."));
        check("Department report says no employees", capture(report::printDepartmentReport).contains("No employees found."));
        check("Sorted report says no payroll records", capture(() -> report.printSorted(true)).contains("No payroll records available."));
        check("Ranking says no payroll records", capture(report::printRanking).contains("No payroll records available."));
        check("Highest/lowest says no payroll records", capture(report::printHighestAndLowest).contains("No payroll records available."));
        String company = capture(report::printCompanyPayroll);
        check("Company payroll shows zero totals", company.contains("Employees processed : 0") && company.contains("Total net payroll   : 0.00"));
        PayrollBatchResult result = payroll.processBatch();
        check("Empty batch: 0 batches, 0 attempted", result.getBatchesRun() == 0 && result.getAttempted() == 0);
        check("Validation finds no problems", validation.validateAll().isEmpty());
        check("Validation report says all records are valid", capture(report::printInvalidRecords).contains("All records are valid"));
    }

    private static void testPayrollTotals() throws Exception {
        section("T14 Accurate payroll totals");
        EmployeeRepository<Employee> repo = sampleRepo();
        PayrollService payroll = new PayrollService(repo, PAYROLL_DATE);
        PayrollBatchResult result = payroll.processBatch();
        check("Total gross = 60000 + 50000 + 21000 = 131000", near(result.getTotalGross(), 131000));
        check("Total deductions = 12000 + 5000 + 1050 = 18050", near(result.getTotalDeductions(), 18050));
        check("Total net = 48000 + 45000 + 19950 = 112950", near(result.getTotalNet(), 112950));
        check("Gross - deductions equals net total", near(result.getTotalGross() - result.getTotalDeductions(), result.getTotalNet()));
        PayrollBatchResult.DepartmentTotal eng = result.getDepartmentTotals().get("Engineering");
        check("Engineering department: 1 employee, net 48000", eng.getEmployeeCount() == 1 && near(eng.getNet(), 48000));
        check("Department keys are sorted -> " + result.getDepartmentTotals().keySet(),
                new ArrayList<>(result.getDepartmentTotals().keySet()).equals(List.of("Engineering", "Finance", "Sales")));
        List<PayrollRecord> asc = payroll.sortByNetSalary(true);
        List<PayrollRecord> desc = payroll.sortByNetSalary(false);
        check("Ascending sort starts with the intern (19950)", near(asc.get(0).getNetSalary(), 19950));
        check("Descending sort starts with the permanent employee (48000)", near(desc.get(0).getNetSalary(), 48000));
    }

    private static void testRepositoryConsistency() throws Exception {
        section("T15 Map and list stay consistent");
        EmployeeRepository<Employee> repo = sampleRepo();
        check("remove(102) returns true", repo.remove(102));
        check("After removal: not found by ID, list size 2",
                !repo.exists(102) && !repo.findById(102).isPresent() && repo.getAll().size() == 2);
        check("remove(999) returns false", !repo.remove(999));
        Employee any = repo.getAll().get(0);
        check("getAll() is read-only (cannot bypass the repository)",
                throwsType(UnsupportedOperationException.class, () -> repo.getAll().add(any)));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static EmployeeRepository<Employee> sampleRepo() throws InvalidSalaryException {
        EmployeeRepository<Employee> repo = new EmployeeRepository<>();
        repo.add(new PermanentEmployee(101, "Asha Kumar", "engineering", LocalDate.of(2022, 1, 10), 50000, 5000, 5000));
        repo.add(new ContractEmployee(102, "Ravi Shah", "Sales", LocalDate.of(2023, 3, 1), 45000, 5000));
        repo.add(new Intern(103, "Meena Iyer", "FINANCE", LocalDate.of(2024, 6, 1), 20000, 1000));
        return repo;
    }

    private static boolean titleCaseCheck() {
        try {
            return "Human Resources".equals(new Intern(1, "X", "human   RESOURCES", LocalDate.of(2024, 1, 1), 1000).getDepartment());
        } catch (InvalidSalaryException e) {
            return false;
        }
    }

    /** Runs the real menu with the given lines as keyboard input and returns everything it printed. */
    private static String runMenu(String... lines) throws Exception {
        String input = String.join("\n", lines) + "\n";
        return capture(() -> new Main(new Scanner(input)).run());
    }

    private static String capture(ThrowingRunnable action) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString();
    }

    private static boolean throwsType(Class<? extends Throwable> type, ThrowingRunnable action) {
        try {
            action.run();
            return false;
        } catch (Throwable t) {
            return type.isInstance(t);
        }
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 0.005;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println(title);
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + description);
        } else {
            failed++;
            System.out.println("  [FAIL] " + description);
        }
    }
}
