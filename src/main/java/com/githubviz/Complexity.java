package com.githubviz;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides simple Java complexity metrics and helpers:
 * - Counts non-empty lines
 * - Counts control flow keywords
 * - Determines color based on complexity
 * - Removes comments & strings before counting
 *
 * This is a lightweight, student-friendly complexity model (not full cyclomatic).
 *
 * @author Muska Said Hasan Mustafa
 * @author Nick Gottwald
 */
public class Complexity {
    private final int yellowThreshold;
    private final int redThreshold;

    public Complexity(int yellowThreshold, int redThreshold) {
        this.yellowThreshold = yellowThreshold;
        this.redThreshold = redThreshold;
    }

    /**
     * Counts occurrences of if/switch/while/for.
     *
     * @param content Java source text
     * @return count of control flow statements
     */
    public int countComplexity(String content) {
        String code = stripCommentsAndStrings(content);
        String[] keys = {"if", "switch", "while", "for"};

        int count = 0;
        for (String k : keys) {
            Matcher m = Pattern.compile("\\b" + k + "\\b").matcher(code);
            while (m.find()) count++;
        }
        return count;
    }

    /**
     * Picks color based on complexity thresholds.
     */
    public Color colorForComplexity(int complexity) {
        if (complexity > redThreshold) return new Color(255, 0, 0);
        if (complexity > yellowThreshold) return new Color(255, 255, 0);
        return new Color(0, 255, 0);
    }

    /**
     * Counts non-blank lines in a file.
     */
    public static int countNonEmptyLines(String content) {
        int c = 0;
        for (String ln : content.split("\n")) {
            if (!ln.trim().isEmpty()) c++;
        }
        return c;
    }

    /**
     * Removes comments and string literals so we don't accidentally count
     * "if" inside a comment or quoted text.
     */
    private String stripCommentsAndStrings(String src) {
        src = src.replaceAll("(?s)/\\*.*?\\*/", "");   // block comments
        src = src.replaceAll("(?m)//.*$", "");         // line comments
        src = src.replaceAll("\"([^\"\\\\]|\\\\.)*\"", "\"\""); // strings
        src = src.replaceAll("'([^'\\\\]|\\\\.)*'", "''");     // chars
        return src;
    }
}

