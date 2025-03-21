package edu.cornell.cis3152.lighting.utils;

public class DebugPrinter {
    // Flag to control whether debug output is enabled.
    private static boolean debugEnabled = true;

    // Setter to control debug mode.
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    // Wrapper for System.out.println()
    public static void println(String message) {
        if (debugEnabled) {
            System.out.println(message);
        }
    }

    // Wrapper for System.out.print()
    public static void print(String message) {
        if (debugEnabled) {
            System.out.print(message);
        }
    }
}
