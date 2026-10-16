package main;

import exception.InvalidSalaryException;
import model.Employee;
import model.PermanentEmployee;

import java.lang.ref.WeakReference;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Small separate program that SHOWS stack, heap, references and garbage collection.
 * Run it with:  java -cp out main.MemoryDemo
 * (Exact numbers differ on every computer and every run.)
 */
public class MemoryDemo {

    public static void main(String[] args) throws InterruptedException, InvalidSalaryException {

        // ---- 1. Stack frames: every method call gets its own frame ----
        System.out.println("1) Stack frames: factorial(4) calls itself, each call has its own 'n'");
        System.out.println("   factorial(4) = " + factorial(4));

        // ---- 2. Heap objects and references ----
        System.out.println("\n2) Heap and references");
        int number = 10; // a primitive local variable
        Employee first = new PermanentEmployee(1, "Asha", "Engineering", LocalDate.of(2022, 1, 10), 50000);
        // 'first' is a local variable holding a REFERENCE. The object itself lives on the heap.
        Employee second = first; // copies the reference, NOT the object
        System.out.println("   first == second (same object)? " + (first == second));
        System.out.println("   number = " + number + " (the JVM may keep it in a stack frame or a CPU register;");
        System.out.println("   it is the JVM's choice, so do not assume every local always lives on the stack)");

        // ---- 3. Unreachable objects and garbage collection ----
        System.out.println("\n3) Garbage collection");
        WeakReference<Employee> watcher = new WeakReference<>(first);
        first = null;
        System.out.println("   After first = null, the object is STILL reachable through 'second': "
                + (watcher.get() != null));
        second = null;
        System.out.println("   After second = null the object is unreachable, so it is ELIGIBLE for collection.");
        System.gc(); // only a REQUEST (hint). The JVM may ignore it or run it later.
        Thread.sleep(200);
        System.out.println("   Collected already? " + (watcher.get() == null)
                + "  (may print true or false: System.gc() does not guarantee immediate collection)");

        // ---- 4. Memory used by many objects ----
        System.out.println("\n4) Memory used by 200,000 employee objects (approximate)");
        long before = usedMemoryMb();
        List<Employee> many = new ArrayList<>();
        for (int i = 1; i <= 200_000; i++) {
            many.add(new PermanentEmployee(i, "Employee" + i, "Sales", LocalDate.of(2023, 1, 1), 30000 + i % 100));
        }
        long during = usedMemoryMb();
        System.out.println("   used before: ~" + before + " MB, with list alive: ~" + during + " MB");
        many = null; // the list and all its employees become unreachable
        System.gc();
        Thread.sleep(300);
        System.out.println("   after many = null and System.gc(): ~" + usedMemoryMb() + " MB (not guaranteed to drop)");
    }

    private static int factorial(int n) {
        if (n <= 1) {
            return 1;
        }
        return n * factorial(n - 1); // new stack frame for every call
    }

    private static long usedMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }
}
