package com.githubviz;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Main GUI window for the GitHubViz application.
 * Lets user enter a GitHub folder URL, fetch files, and visualize
 * complexity and size of Java files using colored squares.
 *
 * Handles Swing UI, buttons, grid display, hover tooltips, and selection.
 *
 * @author Muska Said Hasan Mustafa
 * @author Nick Gottwald
 */
public class GitHubViz extends JFrame {

    private static final String APP_TITLE = "Assignment 02";
    private static final int GRID_COLS = 8;

    private JTextField urlField, selectedFileField;
    private JButton okButton;
    private JPanel gridPanel;
    private JLabel statusBar;

    private final List<FileData> files = new ArrayList<>();
    private int maxLines = 0;

    private final GitHubService gh = new GitHubService();
    private final Complexity analyzer = new Complexity(5, 10);

    /**
     * Builds the UI frame and menus.
     */
    public GitHubViz() {
        setTitle(APP_TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        createMenuBar();
        createUI();
    }

    /**
     * Menu bar with File/Action/Help buttons.
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open from URL...");
        openItem.addActionListener(e -> analyzeRepository());
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        JMenu actionMenu = new JMenu("Action");
        JMenuItem reloadItem = new JMenuItem("Reload");
        reloadItem.addActionListener(e -> analyzeRepository());
        actionMenu.add(reloadItem);
        JMenuItem clearItem = new JMenuItem("Clear");
        clearItem.addActionListener(e -> clearGrid());
        actionMenu.add(clearItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(actionMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    /**
     * Builds the top input bar, grid panel, and status bar.
     */
    private void createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));

        urlField = new JTextField();
        okButton = new JButton("OK");
        okButton.setEnabled(false);

        // Enable button only if URL field not empty
        urlField.getDocument().addDocumentListener(new DocumentListener() {
            private void toggle() {
                okButton.setEnabled(!urlField.getText().trim().isEmpty());
            }
            public void insertUpdate(DocumentEvent e) { toggle(); }
            public void removeUpdate(DocumentEvent e) { toggle(); }
            public void changedUpdate(DocumentEvent e) { toggle(); }
        });

        okButton.addActionListener(e -> analyzeRepository());
        inputPanel.add(new JLabel("GitHub Folder URL:"), BorderLayout.WEST);
        inputPanel.add(urlField, BorderLayout.CENTER);
        inputPanel.add(okButton, BorderLayout.EAST);

        gridPanel = new JPanel(new GridLayout(0, GRID_COLS, 2, 2));
        JScrollPane scrollPane = new JScrollPane(gridPanel);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        selectedFileField = new JTextField();
        selectedFileField.setEditable(false);
        bottomPanel.add(new JLabel("Selected File Name:"), BorderLayout.WEST);
        bottomPanel.add(selectedFileField, BorderLayout.CENTER);

        statusBar = new JLabel(" ");

        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * Starts GitHub fetch + analysis in background thread.
     * Updates UI as it progresses.
     */
    private void analyzeRepository() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) return;

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                try {
                    publish("Fetching...");
                    RepoInfo info = gh.parseGitHubUrl(url);
                    if (info == null) { publish("Invalid URL"); return null; }

                    List<String> javaPaths = gh.listJavaFiles(info);
                    files.clear();
                    maxLines = 0;

                    int i = 0;
                    for (String p : javaPaths) {
                        String content = gh.getFileContent(info, p);
                        int lines = Complexity.countNonEmptyLines(content);
                        int complexity = analyzer.countComplexity(content);

                        files.add(new FileData(p, lines, complexity, analyzer));
                        maxLines = Math.max(maxLines, lines);

                        publish("Analyzed " + (++i) + " of " + javaPaths.size());
                    }

                    for (FileData f : files) f.setAlphaFromMax(maxLines);

                } catch (Exception ex) {
                    publish("Error: " + ex.getMessage());
                }
                return null;
            }

            @Override protected void process(List<String> chunks) {
                statusBar.setText(chunks.get(chunks.size() - 1));
            }

            @Override protected void done() {
                updateGrid();
                statusBar.setText(files.size() + " files analyzed");
            }
        }.execute();
    }

    /**
     * Rebuilds the grid UI after analysis.
     */
    private void updateGrid() {
        gridPanel.removeAll();
        for (FileData f : files) gridPanel.add(createSquare(f));
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    /**
     * Builds a square JPanel for one Java file.
     *
     * @param f file metrics model
     * @return a colored panel for the grid
     */
    private JPanel createSquare(FileData f) {
        JPanel sq = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int a = (int) (f.alpha * 255);
                Color draw = new Color(f.baseColor.getRed(), f.baseColor.getGreen(), f.baseColor.getBlue(), a);
                g.setColor(draw);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        sq.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        sq.setToolTipText("<html><b>"+f.name+"</b><br>Lines: "+f.lines+"<br>Complexity: "+f.complexity+"</html>");
        sq.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        sq.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                selectedFileField.setText(f.name);
                highlightSquare(sq);
            }
        });

        return sq;
    }

    /**
     * Highlights clicked square and un-highlights others.
     *
     * @param sq clicked panel
     */
    private void highlightSquare(JPanel sq) {
        for (Component c : gridPanel.getComponents())
            if (c instanceof JPanel) ((JPanel)c).setBorder(BorderFactory.createLineBorder(Color.BLACK));
        sq.setBorder(BorderFactory.createLineBorder(Color.BLUE, 3));
    }

    /**
     * Clears UI + stored file list.
     */
    private void clearGrid() {
        files.clear();
        gridPanel.removeAll();
        selectedFileField.setText("");
        statusBar.setText(" ");
        gridPanel.repaint();
    }

    /**
     * Shows About popup.
     */
    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "GitHubViz 2.0\nColor = complexity\nTransparency = size",
                "About", JOptionPane.INFORMATION_MESSAGE);
    }
}
