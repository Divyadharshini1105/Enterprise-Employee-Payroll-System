package service;

import model.Employee;
import repository.EmployeeRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Collects problems with employee records.
 *
 * Data is checked at the door (constructors and repository.add), so bad records
 * are normally refused and then logged here. validateAll() also re-checks the
 * stored records for problems that can appear later (duplicates, not-yet-eligible
 * employees, invalid calculated amounts).
 */
public class ValidationService {

    private final EmployeeRepository<Employee> repository;
    private final PayrollService payrollService;
    private final List<ValidationIssue> rejectedSubmissions = new ArrayList<>();

    public ValidationService(EmployeeRepository<Employee> repository, PayrollService payrollService) {
        this.repository = repository;
        this.payrollService = payrollService;
    }

    /** Remember an employee that was refused when someone tried to add it. */
    public void logRejected(Integer employeeId, String reason) {
        rejectedSubmissions.add(new ValidationIssue(employeeId, "Rejected at entry", reason));
    }

    /** Every problem found so far: refused submissions + problems in stored records. */
    public List<ValidationIssue> validateAll() {
        List<ValidationIssue> issues = new ArrayList<>(rejectedSubmissions);

        Set<Integer> seenIds = new HashSet<>();
        for (Employee employee : repository.getAll()) {
            if (!seenIds.add(employee.getId())) { // add() returns false if the ID was already there
                issues.add(new ValidationIssue(employee.getId(), "Stored record check",
                        "Duplicate employee ID found in stored records."));
            }
        }

        issues.addAll(payrollService.processBatch().getRejected());
        return issues;
    }
}
