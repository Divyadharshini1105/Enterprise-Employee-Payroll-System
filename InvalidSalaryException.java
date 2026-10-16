package exception;

/**
 * A CHECKED exception (it extends Exception, not RuntimeException).
 * Java forces the caller to either catch it or declare "throws" for it.
 * We use it when a salary-related value (base salary, bonus, allowance,
 * contract pay, stipend) is invalid, so nobody can ignore the problem by accident.
 */
public class InvalidSalaryException extends Exception {

    private static final long serialVersionUID = 1L; // version id used when an exception is serialized

    public InvalidSalaryException(String message) {
        super(message);
    }
}
