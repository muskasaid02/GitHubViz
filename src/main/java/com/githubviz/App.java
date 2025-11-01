package com.githubviz;

import javax.swing.*;

/**
 * Assignment 02 - GitHubViz
 * Entry point to launch the GitHub visualization UI.
 *
 * @author Muska Said Hasan Mustafa
 * @author Nick Gottwald
 * @version 2.0
 * @since 2025-10-31
 */
public class App {

    /**
     * Starts the application and sets the system UI theme.
     *
     * @param args unused CLI arguments
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            new GitHubViz().setVisible(true);
        });
    }
}
