package main;

import exception.InvalidSalaryException;
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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

/**
 * Menu-driven console application.
 *
 * Input strategy: EVERY value is read with scanner.nextLine() and converted by us
 * (Integer.parseInt, Double.parseDouble ...). We never use nextInt()/nextDouble(),
 * so the leftover "Enter" key can never confuse a later nextLine().
 */
public class Main {

    private final Scanner scanner;
    private final EmployeeRepository<Employee> repository = new EmployeeRepository<>();
    private final PayrollService payrollService = new PayrollService(repository);
    private final ValidationService validationService = new ValidationService(repository, payrollService);
    private final ReportService reportService =
            new ReportService(repository, payrollService, validationService);

    public Main(Scanner scanner) {
        this.scanner = scanner;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            new Main(scanner).run();
        } finally {
            scanner.close(); // always release the input stream, even if something went wrong
        }
    }

    /** The repeating menu loop. */
    public void run() {
        System.out.println("==============================================");
        System.out.println(" Enterprise Employee and Payroll Management System");
        System.out.println("==============================================");

        boolean running = true;
        while (running) {
            printMenu();

            // ---- read and check the menu choice ----
            String line;
            try {
                line = readLine("Enter your choice (1-14): ");
            } catch (NoSuchElementException ex) {
                System.out.println();
                System.out.println("No more input available. Exiting.");
                break;
            }
            int choice;
            try {
                choice = Integer.parseInt(line);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid input '" + line + "'. Please type a number from 1 to 14.");
                continue; // show the menu again
            }

            // ---- run the chosen action ----
            try {
                switch (choice) {
                    case 1:
                        addPermanentEmployee();
                        break;
                    case 2:
                        addContractEmployee();
                        break;
                    case 3:
                        addIntern();
                        break;
                    case 4:
                        reportService.printAllEmployees();
                        break;
                    case 5:
                        searchById();
                        break;
                    case 6:
                        filterByDepartment();
                        break;
                    case 7:
                        sortBySalary();
                        break;
                    case 8:
                        reportService.printDepartmentReport();
                        break;
                    case 9:
                        calculateIndividualPayroll();
                        break;
                    case 10:
                        PayrollBatchResult result = payrollService.processBatch();
                        reportService.printBatchResult(result);
                        break;
                    case 11:
                        reportService.printRanking();
                        reportService.printHighestAndLowest();
                        break;
                    case 12:
                        reportService.printInvalidRecords();
                        break;
                    case 13:
                        reportService.printCompanyPayroll();
                        break;
                    case 14:
                        running = false;
                        System.out.println("Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid choice " + choice + ". Please choose a number from 1 to 14.");
                        continue;
                }
            } catch (NoSuchElementException ex) {
                System.out.println();
                System.out.println("Input ended in the middle of a task. Exiting.");
                break;
            } catch (IllegalArgumentException | IllegalStateException ex) {
                // Application boundary: show a friendly message and keep the menu running.
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("-------------------- MENU --------------------");
        System.out.println(" 1. Add permanent employee");
        System.out.println(" 2. Add contract employee");
        System.out.println(" 3. Add intern");
        System.out.println(" 4. View all employees");
        System.out.println(" 5. Search by employee ID");
        System.out.println(" 6. Filter by department");
        System.out.println(" 7. Sort employees by salary");
        System.out.println(" 8. Generate department-wise reports");
        System.out.println(" 9. Calculate individual payroll");
        System.out.println("10. Process batch payroll");
        System.out.println("11. Rank employees by net salary");
        System.out.println("12. Validate employee records");
        System.out.println("13. Display total payroll summary");
        System.out.println("14. Exit");
        System.out.println("----------------------------------------------");
    }

    // ------------------------------------------------------------------
    // Add employees
    // ------------------------------------------------------------------

    private void addPermanentEmployee() {
        System.out.println("--- Add Permanent Employee ---");
        int id = readInt("Employee ID: ");
        if (rejectIfDuplicate(id)) {
            return;
        }
        try {
            String name = readLine("Name: ");
            String department = readLine("Department (e.g. Engineering, Sales, Finance, Operations): ");
            LocalDate joiningDate = readDate("Joining date (yyyy-MM-dd): ");
            double baseSalary = readDouble("Base salary: ");
            double bonus = readBonus(baseSalary);
            double allowances = readDouble("Allowances: ");

            Employee employee = new PermanentEmployee(id, name, department, joiningDate,
                    baseSalary, bonus, allowances);
            repository.add(employee);
            System.out.println("Permanent employee " + id + " added successfully.");
        } catch (InvalidSalaryException | IllegalArgumentException ex) {
            reject(id, ex.getMessage());
        }
    }

    private void addContractEmployee() {
        System.out.println("--- Add Contract Employee ---");
        int id = readInt("Employee ID: ");
        if (rejectIfDuplicate(id)) {
            return;
        }
        try {
            String name = readLine("Name: ");
            String department = readLine("Department (e.g. Engineering, Sales, Finance, Operations): ");
            LocalDate joiningDate = readDate("Joining date (yyyy-MM-dd): ");
            double contractPay = readDouble("Contract pay: ");
            double bonus = Employee.calculateBonus(readDouble("Bonus (fixed amount, 0 for none): "));

            Employee employee = new ContractEmployee(id, name, department, joiningDate, contractPay, bonus);
            repository.add(employee);
            System.out.println("Contract employee " + id + " added successfully.");
        } catch (InvalidSalaryException | IllegalArgumentException ex) {
            reject(id, ex.getMessage());
        }
    }

    private void addIntern() {
        System.out.println("--- Add Intern ---");
        int id = readInt("Employee ID: ");
        if (rejectIfDuplicate(id)) {
            return;
        }
        try {
            String name = readLine("Name: ");
            String department = readLine("Department (e.g. Engineering, Sales, Finance, Operations): ");
            LocalDate joiningDate = readDate("Joining date (yyyy-MM-dd): ");
            double stipend = readDouble("Monthly stipend: ");
            double bonus = Employee.calculateBonus(readDouble("Eligible bonus (fixed amount, 0 for none): "));

            Employee employee = new Intern(id, name, department, joiningDate, stipend, bonus);
            repository.add(employee);
            System.out.println("Intern " + id + " added successfully.");
        } catch (InvalidSalaryException | IllegalArgumentException ex) {
            reject(id, ex.getMessage());
        }
    }

    /** Asks how the bonus should be calculated and uses the matching overloaded method. */
    private double readBonus(double baseSalary) {
        System.out.println("Bonus type: 1) Fixed amount  2) Percentage of base salary  "
                + "3) Percentage of base salary + extra amount");
        int type = readInt("Choose bonus type (1-3): ");
        switch (type) {
            case 1:
                return Employee.calculateBonus(readDouble("Fixed bonus amount: "));
            case 2:
                return Employee.calculateBonus(readDouble("Bonus percentage (0-100): "), baseSalary);
            case 3:
                double percentage = readDouble("Bonus percentage (0-100): ");
                double extra = readDouble("Extra fixed amount: ");
                return Employee.calculateBonus(percentage, baseSalary, extra);
            default:
                throw new IllegalArgumentException("Invalid bonus type " + type + ". Choose 1, 2 or 3.");
        }
    }

    private boolean rejectIfDuplicate(int id) {
        if (repository.exists(id)) {
            reject(id, "Duplicate employee ID: " + id);
            return true;
        }
        return false;
    }

    private void reject(int id, String reason) {
        System.out.println("Employee NOT added. Reason: " + reason);
        validationService.logRejected(id, reason);
    }

    // ------------------------------------------------------------------
    // Search, filter, sort, payroll
    // ------------------------------------------------------------------

    private void searchById() {
        int id = readInt("Enter employee ID to search: ");
        Optional<Employee> found = repository.findById(id);
        if (found.isPresent()) {
            reportService.printEmployeeTable("Search result for ID " + id, List.of(found.get()));
            System.out.println("Details: " + found.get());
        } else {
            System.out.println("No employee found with ID " + id + ".");
        }
    }

    private void filterByDepartment() {
        String department = readLine("Enter department name: ");
        List<Employee> matches = repository.findByDepartment(department);
        reportService.printEmployeeTable("Employees in department '" + department + "'", matches);
    }

    private void sortBySalary() {
        int order = readInt("Sort order: 1) Ascending  2) Descending: ");
        if (order != 1 && order != 2) {
            System.out.println("Invalid sort order " + order + ". Choose 1 or 2.");
            return;
        }
        reportService.printSorted(order == 1);
    }

    private void calculateIndividualPayroll() {
        int id = readInt("Enter employee ID: ");
        PayrollRecord record = payrollService.calculatePayroll(id);
        reportService.printIndividualPayroll(record);
    }

    // ------------------------------------------------------------------
    // Input helpers: every one of them uses nextLine() only
    // ------------------------------------------------------------------

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int readInt(String prompt) {
        while (true) {
            String text = readLine(prompt);
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                System.out.println("  Invalid whole number '" + text + "'. Please try again.");
            }
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            String text = readLine(prompt);
            // Only plain numbers like 5000 or -12.50 are accepted ("NaN", "1e5", "5d" are not).
            if (text.matches("-?\\d+(\\.\\d+)?")) {
                return Double.parseDouble(text);
            }
            System.out.println("  Invalid number '" + text + "'. Please try again (example: 45000.50).");
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            String text = readLine(prompt);
            try {
                return LocalDate.parse(text); // expects yyyy-MM-dd
            } catch (DateTimeParseException ex) {
                System.out.println("  Invalid date '" + text + "'. Use the format yyyy-MM-dd, e.g. 2024-01-15.");
            }
        }
    }
}
