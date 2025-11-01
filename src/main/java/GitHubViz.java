/**
 * Assignment 02 — GitHub Java File Visualizer (with TULIP)
 *
 * Visualizes .java files from a GitHub *folder*: color encodes complexity,
 * opacity encodes non-empty line count. Includes a labeled URL field, menus,
 * a grid of squares, a selected-file display, and a status bar.
 *
 * Fetch layer uses TULIP's GitHubHandler:
 *   - listFiles(path) -> List<String> of file paths in the repo folder
 *   - getFileContent(path) -> String of file contents
 *
 * Expected URL formats:
 *   • https://github.com/<owner>/<repo>/tree/<branch>/<path-to-folder>
 *   • https://github.com/<owner>/<repo>  (lists repo root)
 * (Branch is parsed but GitHubHandler works on default branch.)
 *
 * @author  Muska Said
 * @version 3.0
 * @date    October 31, 2025
 */

 import javax.swing.*;
 import javax.swing.border.EmptyBorder;
 import javax.swing.event.DocumentEvent;
 import javax.swing.event.DocumentListener;
 import java.awt.*;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 import java.net.URI;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 // TULIP
 import javiergs.tulip.GitHubHandler;
 
 public class GitHubViz extends JFrame {
 
     // ---------- Constants ----------
     private static final String APP_TITLE = "Assignment 02"; // Spec requires this exact title
     private static final int GRID_COLS = 8;
     private static final int COMPLEXITY_YELLOW = 5;
     private static final int COMPLEXITY_RED = 10;
     private static final int SQUARE_SIZE = 80;
 
     // ---------- UI ----------
     private JTextField urlField;
     private JButton okButton;
     private JPanel gridPanel;
     private JTextField selectedFileField;
     private JLabel statusBar;
 
     // ---------- Data ----------
     private final List<FileData> files = new ArrayList<>();
     private int maxLines = 0;
 
     // ---------- Model ----------
     private static class FileData {
         final String name;
         final int lines;
         final int complexity;
         final Color baseColor; // color without alpha
         float alpha;           // 0..1 based on lines
 
         FileData(String name, int lines, int complexity) {
             this.name = name;
             this.lines = lines;
             this.complexity = complexity;
 
             if (complexity > COMPLEXITY_RED) {
                 this.baseColor = new Color(255, 0, 0);
             } else if (complexity > COMPLEXITY_YELLOW) {
                 this.baseColor = new Color(255, 255, 0);
             } else {
                 this.baseColor = new Color(0, 255, 0);
             }
         }
 
         void setAlphaFromMax(int maxLines) {
             if (maxLines <= 0) {
                 this.alpha = 0f;
             } else {
                 this.alpha = Math.max(0f, Math.min(1f, (float) lines / (float) maxLines));
             }
         }
     }
 
     // ---------- Constructor ----------
     public GitHubViz() {
         setTitle(APP_TITLE);
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         setSize(1000, 700);
         setLocationRelativeTo(null);
 
         createMenuBar();
         createUI();
     }
 
     // ---------- Menu Bar ----------
     private void createMenuBar() {
         JMenuBar menuBar = new JMenuBar();
 
         // File
         JMenu fileMenu = new JMenu("File");
         JMenuItem openItem = new JMenuItem("Open from URL...");
         openItem.addActionListener(e -> analyzeRepository());
         fileMenu.add(openItem);
         fileMenu.addSeparator();
         JMenuItem exitItem = new JMenuItem("Exit");
         exitItem.addActionListener(e -> System.exit(0));
         fileMenu.add(exitItem);
 
         // Action
         JMenu actionMenu = new JMenu("Action");
         JMenuItem reloadItem = new JMenuItem("Reload");
         reloadItem.addActionListener(e -> analyzeRepository());
         actionMenu.add(reloadItem);
         JMenuItem clearItem = new JMenuItem("Clear");
         clearItem.addActionListener(e -> clearGrid());
         actionMenu.add(clearItem);
 
         // Help
         JMenu helpMenu = new JMenu("Help");
         JMenuItem aboutItem = new JMenuItem("About");
         aboutItem.addActionListener(e -> showAbout());
         helpMenu.add(aboutItem);
 
         menuBar.add(fileMenu);
         menuBar.add(actionMenu);
         menuBar.add(helpMenu);
 
         setJMenuBar(menuBar);
     }
 
     // ---------- Main UI ----------
     private void createUI() {
         JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
         mainPanel.setBackground(new Color(173, 216, 204));
         mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
 
         // Top: labeled URL input + OK
         JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
         inputPanel.setBackground(Color.LIGHT_GRAY);
         inputPanel.setBorder(BorderFactory.createCompoundBorder(
                 BorderFactory.createLineBorder(Color.BLACK),
                 new EmptyBorder(10, 10, 10, 10)
         ));
 
         JLabel urlLabel = new JLabel("GitHub Folder URL:");
         urlLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
 
         urlField = new JTextField();
         urlField.setFont(new Font("Monospaced", Font.PLAIN, 12));
 
         okButton = new JButton("OK");
         okButton.setPreferredSize(new Dimension(100, 30));
         okButton.addActionListener(e -> analyzeRepository());
         okButton.setEnabled(false); // enable only when non-empty
 
         urlField.getDocument().addDocumentListener(new DocumentListener() {
             private void toggle() {
                 okButton.setEnabled(!urlField.getText().trim().isEmpty());
             }
             public void insertUpdate(DocumentEvent e) { toggle(); }
             public void removeUpdate(DocumentEvent e) { toggle(); }
             public void changedUpdate(DocumentEvent e) { toggle(); }
         });
 
         inputPanel.add(urlLabel, BorderLayout.WEST);
         inputPanel.add(urlField, BorderLayout.CENTER);
         inputPanel.add(okButton, BorderLayout.EAST);
 
         // Center: grid inside scroll pane
         JPanel centerContainer = new JPanel(new BorderLayout());
         centerContainer.setBackground(Color.LIGHT_GRAY);
         centerContainer.setBorder(BorderFactory.createCompoundBorder(
                 BorderFactory.createLineBorder(Color.BLACK),
                 new EmptyBorder(10, 10, 10, 10)
         ));
 
         gridPanel = new JPanel(new GridLayout(0, GRID_COLS, 2, 2));
         gridPanel.setBackground(Color.WHITE);
 
         JScrollPane scrollPane = new JScrollPane(gridPanel);
         scrollPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
         centerContainer.add(scrollPane, BorderLayout.CENTER);
 
         // Bottom: selected file
         JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
         bottomPanel.setBackground(Color.LIGHT_GRAY);
         bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                 BorderFactory.createLineBorder(Color.BLACK),
                 new EmptyBorder(10, 10, 10, 10)
         ));
 
         JLabel selectedLabel = new JLabel("Selected File Name:");
         selectedLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
 
         selectedFileField = new JTextField();
         selectedFileField.setEditable(false);
         selectedFileField.setFont(new Font("Monospaced", Font.PLAIN, 12));
 
         bottomPanel.add(selectedLabel, BorderLayout.WEST);
         bottomPanel.add(selectedFileField, BorderLayout.CENTER);
 
         // Status bar
         statusBar = new JLabel(" ");
         statusBar.setBorder(BorderFactory.createCompoundBorder(
                 BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
                 new EmptyBorder(5, 10, 5, 10)
         ));
         statusBar.setFont(new Font("SansSerif", Font.PLAIN, 11));
 
         // Layout
         mainPanel.add(inputPanel, BorderLayout.NORTH);
         mainPanel.add(centerContainer, BorderLayout.CENTER);
         mainPanel.add(bottomPanel, BorderLayout.SOUTH);
 
         add(mainPanel, BorderLayout.CENTER);
         add(statusBar, BorderLayout.SOUTH);
     }
 
     // ---------- Behavior ----------
 
     /** Fetches, analyzes, and visualizes Java files from a GitHub folder URL using TULIP. */
     private void analyzeRepository() {
         final String url = urlField.getText().trim();
         if (url.isEmpty()) {
             JOptionPane.showMessageDialog(this,
                     "Please enter a valid GitHub folder URL",
                     "Error", JOptionPane.ERROR_MESSAGE);
             return;
         }
 
         SwingWorker<Void, String> worker = new SwingWorker<>() {
             @Override
             protected Void doInBackground() {
                 publish("Fetching...");
                 try {
                     RepoInfo info = parseGitHubUrl(url);
                     if (info == null) {
                         publish("Unrecognized GitHub URL format.");
                         files.clear();
                         maxLines = 0;
                         return null;
                     }
 
                     // TULIP handler (note: operates on default branch)
                     GitHubHandler gh = new GitHubHandler(info.owner, info.repo);
 
                     // List files in the folder; filter for .java only
                     List<String> paths = gh.listFiles(info.path == null ? "" : info.path);
                     List<String> javaPaths = new ArrayList<>();
                     for (String p : paths) {
                         if (p != null && p.toLowerCase().endsWith(".java")) {
                             javaPaths.add(p);
                         }
                     }
 
                     if (javaPaths.isEmpty()) {
                         publish("No .java files found.");
                         files.clear();
                         maxLines = 0;
                         return null;
                     }
 
                     publish(javaPaths.size() + " files found. Analyzing...");
 
                     files.clear();
                     maxLines = 0;
                     int idx = 0;
 
                     for (String filePath : javaPaths) {
                         String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
 
                         String content = gh.getFileContent(filePath);
 
                         int lines = countNonEmptyLines(content);
                         int complexity = countComplexity(content);
 
                         files.add(new FileData(fileName, lines, complexity));
                         if (lines > maxLines) maxLines = lines;
 
                         idx++;
                         publish(String.format("Analyzed %d of %d files", idx, javaPaths.size()));
                     }
 
                     for (FileData f : files) {
                         f.setAlphaFromMax(maxLines);
                     }
 
                 } catch (Exception ex) {
                     publish("Error: " + ex.getMessage());
                 }
                 return null;
             }
 
             @Override
             protected void process(List<String> chunks) {
                 statusBar.setText(chunks.get(chunks.size() - 1));
             }
 
             @Override
             protected void done() {
                 updateGrid();
                 statusBar.setText(files.isEmpty() ? "No files to display" : (files.size() + " files analyzed"));
             }
         };
 
         worker.execute();
     }
 
     // ---------- GitHub URL parsing ----------
 
     /** Holder for parsed parts of a GitHub URL. */
     private static class RepoInfo {
         final String owner;
         final String repo;
         final String branch; // parsed but unused (GitHubHandler uses default branch)
         final String path;   // folder path inside repo
         RepoInfo(String owner, String repo, String branch, String path) {
             this.owner = owner; this.repo = repo; this.branch = branch; this.path = path;
         }
     }
 
     /**
      * Parses URLs like:
      *   https://github.com/<owner>/<repo>/tree/<branch>/<path...>
      *   https://github.com/<owner>/<repo>
      * Returns RepoInfo or null if not recognized.
      */
     private RepoInfo parseGitHubUrl(String url) {
         try {
             URI u = new URI(url);
             String host = u.getHost();
             if (host == null || !host.contains("github.com")) return null;
 
             String[] parts = u.getPath().split("/");
             // parts: ["", owner, repo, maybe "tree", maybe branch, path...]
             if (parts.length < 3) return null;
 
             String owner = parts[1];
             String repo  = parts[2];
 
             if (parts.length >= 5 && "tree".equals(parts[3])) {
                 String branch = parts[4];
                 StringBuilder path = new StringBuilder();
                 for (int i = 5; i < parts.length; i++) {
                     path.append(parts[i]);
                     if (i < parts.length - 1) path.append("/");
                 }
                 return new RepoInfo(owner, repo, branch, path.toString());
             } else {
                 // Root of repo; no explicit branch/path
                 return new RepoInfo(owner, repo, null, "");
             }
         } catch (Exception e) {
             return null;
         }
     }
 
     // ---------- Metrics ----------
 
     /** Counts non-empty lines in the given file content. */
     private int countNonEmptyLines(String content) {
         String[] lines = content.split("\n", -1);
         int count = 0;
         for (String ln : lines) {
             if (!ln.trim().isEmpty()) count++;
         }
         return count;
     }
 
     /**
      * Counts occurrences of control statements (if|switch|while|for) in *code*
      * (comments and string/char literals are stripped first).
      */
     private int countComplexity(String content) {
         String code = stripCommentsAndStrings(content);
         int complexity = 0;
         String[] keywords = {"if", "switch", "while", "for"};
         for (String kw : keywords) {
             Matcher m = Pattern.compile("\\b" + kw + "\\b").matcher(code);
             while (m.find()) complexity++;
         }
         return complexity;
     }
 
     /** Removes block/line comments and string/char literals for safer token scans. */
     private String stripCommentsAndStrings(String src) {
         // Remove block comments: /* ... */
         src = src.replaceAll("(?s)/\\*.*?\\*/", "");
         // Remove line comments: // ...
         src = src.replaceAll("(?m)//.*$", "");
         // Remove string literals (handles escapes simplistically)
         src = src.replaceAll("\"([^\"\\\\]|\\\\.)*\"", "\"\"");
         // Remove char literals
         src = src.replaceAll("'([^'\\\\]|\\\\.)*'", "''");
         return src;
     }
 
     // ---------- Rendering ----------
 
     /** Rebuilds the grid based on current file data. */
     private void updateGrid() {
         gridPanel.removeAll();
 
         for (FileData file : files) {
             JPanel square = createSquare(file);
             gridPanel.add(square);
         }
 
         gridPanel.revalidate();
         gridPanel.repaint();
     }
 
     /** Creates an individual colored/alpha square with tooltip & click selection. */
     private JPanel createSquare(FileData file) {
         JPanel square = new JPanel() {
             @Override
             protected void paintComponent(Graphics g) {
                 super.paintComponent(g);
                 Graphics2D g2d = (Graphics2D) g;
                 g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
 
                 // Apply alpha to color (0..255)
                 int a = (int) (file.alpha * 255f);
                 a = Math.max(0, Math.min(255, a));
                 Color draw = new Color(file.baseColor.getRed(), file.baseColor.getGreen(), file.baseColor.getBlue(), a);
 
                 g2d.setColor(draw);
                 g2d.fillRect(0, 0, getWidth(), getHeight());
             }
         };
 
         square.setBorder(BorderFactory.createLineBorder(Color.BLACK));
         square.setPreferredSize(new Dimension(SQUARE_SIZE, SQUARE_SIZE));
         square.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
 
         // Tooltip
         square.setToolTipText(String.format(
                 "<html><b>%s</b><br>Lines: %d<br>Complexity: %d</html>",
                 file.name, file.lines, file.complexity
         ));
 
         // Click to select
         square.addMouseListener(new MouseAdapter() {
             @Override
             public void mouseClicked(MouseEvent e) {
                 selectedFileField.setText(file.name);
                 highlightSquare(square);
             }
         });
 
         return square;
     }
 
     /** Highlights the selected square and removes highlight from others. */
     private void highlightSquare(JPanel square) {
         for (Component comp : gridPanel.getComponents()) {
             if (comp instanceof JPanel) {
                 ((JPanel) comp).setBorder(BorderFactory.createLineBorder(Color.BLACK));
             }
         }
         square.setBorder(BorderFactory.createLineBorder(Color.BLUE, 3));
     }
 
     /** Clears UI/state. */
     private void clearGrid() {
         files.clear();
         gridPanel.removeAll();
         selectedFileField.setText("");
         statusBar.setText(" ");
         gridPanel.revalidate();
         gridPanel.repaint();
     }
 
     /** Simple About dialog with spec-mapped legend. */
     private void showAbout() {
         String message = "Assignment 02 — GitHub Java Visualizer\n\n" +
                 "Color encodes complexity:\n" +
                 "• Red: > 10 control statements\n" +
                 "• Yellow: > 5 control statements\n" +
                 "• Green: ≤ 5 control statements\n\n" +
                 "Transparency encodes size (non-empty lines).\n" +
                 "Select a square to see its file name.";
         JOptionPane.showMessageDialog(this, message, "About",
                 JOptionPane.INFORMATION_MESSAGE);
     }
 
     // ---------- Main ----------
     public static void main(String[] args) {
         SwingUtilities.invokeLater(() -> {
             try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
             catch (Exception ignored) {}
 
             GitHubViz app = new GitHubViz();
             app.setVisible(true);
         });
     }
 }
 