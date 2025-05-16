package walknroll.zoodini.utils;

public class DebugPrinter {
    // Wrapper for DebugPrinter.println()
    public static void println(String message) {
        if (Constants.DEBUG) {
            DebugPrinter.println(message);
        }
    }

    // Wrapper for System.out.print()
    public static void print(String message) {
        if (Constants.DEBUG) {
            System.out.print(message);
        }
    }
}
