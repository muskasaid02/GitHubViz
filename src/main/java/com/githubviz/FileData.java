package com.githubviz;

import java.awt.Color;

/**
 * Holds metrics for a Java source file:
 * name, line count, complexity score, and alpha transparency.
 *
 * @author Muska Said Hasan Mustafa
 * @author Nick Gottwald
 */
public class FileData {

    public final String name;
    public final int lines;
    public final int complexity;
    public final Color baseColor;
    public float alpha;

    /**
     * Creates a model representing a single Java file's metrics.
     *
     * @param name file name
     * @param lines non-empty line count
     * @param complexity number of control statements
     * @param util Complexity helper to get base color
     */
    public FileData(String name, int lines, int complexity, Complexity util) {
        this.name = name;
        this.lines = lines;
        this.complexity = complexity;
        this.baseColor = util.colorForComplexity(complexity);
    }

    /**
     * Sets transparency relative to largest file.
     *
     * @param maxLines largest line count among all files
     */
    public void setAlphaFromMax(int maxLines) {
        if (maxLines <= 0) alpha = 0f;
        else alpha = Math.min(1f, (float) lines / maxLines);
    }
}

