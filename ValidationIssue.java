package service;

/**
 * One problem found with an employee record: which employee (if known),
 * where the problem was found, and why.
 */
public class ValidationIssue {

    private final Integer employeeId; // Integer (not int) so it can be null when the ID is unknown
    private final String source;
    private final String reason;

    public ValidationIssue(Integer employeeId, String source, String reason) {
        this.employeeId = employeeId;
        this.source = source;
        this.reason = reason;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public String getSource() {
        return source;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "Employee ID " + (employeeId == null ? "N/A" : employeeId.toString())
                + " [" + source + "]: " + reason;
    }
}
