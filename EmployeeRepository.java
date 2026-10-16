package repository;

import model.Employee;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generic storage for employees.
 *
 * "T extends Employee" is a BOUNDED type parameter: T can be Employee or any
 * subclass of it (PermanentEmployee, Intern, ...), but never String or Integer.
 * Benefits:
 *   - Code reuse: one class works for every employee type.
 *   - Type safety: EmployeeRepository<Intern> refuses to store a ContractEmployee
 *     at compile time, and findById() returns T so no casting is needed.
 *   - Because T is known to be an Employee, we can safely call employee.getId() inside.
 *
 * The HashMap (fast lookup by ID) and the ArrayList (ordered iteration) are BOTH
 * updated only inside add() and remove(), so they can never get out of sync.
 */
public class EmployeeRepository<T extends Employee> {

    private final Map<Integer, T> employeeMap = new HashMap<>();
    private final List<T> employeeList = new ArrayList<>();

    /**
     * Adds an employee. Average time O(1).
     *
     * @throws IllegalArgumentException if the employee is null or the ID already exists
     */
    public void add(T employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee must not be null.");
        }
        if (employeeMap.containsKey(employee.getId())) {
            throw new IllegalArgumentException("Duplicate employee ID: " + employee.getId());
        }
        employeeMap.put(employee.getId(), employee);
        employeeList.add(employee);
    }

    /** Looks up by ID in O(1) average time. Optional means "maybe nothing found". */
    public Optional<T> findById(int id) {
        return Optional.ofNullable(employeeMap.get(id));
    }

    /** True if an employee with this ID is already stored. O(1) average. */
    public boolean exists(int id) {
        return employeeMap.containsKey(id);
    }

    /**
     * Returns a read-only VIEW of the stored employees (no copy is made, so this is
     * cheap). Callers cannot add or remove through it, which protects the map/list pair.
     */
    public List<T> getAll() {
        return Collections.unmodifiableList(employeeList);
    }

    /** Employees of one department (case-insensitive). O(n). */
    public List<T> findByDepartment(String department) {
        List<T> result = new ArrayList<>();
        if (department == null) {
            return result;
        }
        String wanted = department.trim();
        for (T employee : employeeList) {
            if (employee.getDepartment().equalsIgnoreCase(wanted)) {
                result.add(employee);
            }
        }
        return result;
    }

    /** Removes by ID from BOTH collections. O(n) because the list must shift elements. */
    public boolean remove(int id) {
        T removed = employeeMap.remove(id);
        if (removed == null) {
            return false;
        }
        employeeList.remove(removed);
        return true;
    }

    public int size() {
        return employeeList.size();
    }

    public boolean isEmpty() {
        return employeeList.isEmpty();
    }
}
