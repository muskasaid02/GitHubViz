/**
 * Assignment 02 - GitHubViz
 * This program connects to a GitHub folder, finds all Java files,
 * and shows one square for each file. The color shows how complex
 * the file is based on control statements, and transparency shows
 * how big the file is (line count). This uses the TULIP library to
 * read files directly from GitHub.
 *
 * Authors: Muska Said Hasan Mustafa and Nick Gottwald
 * Date: October 2025
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
 import javiergs.tulip.GitHubHandler;
 
 public class GitHubViz extends JFrame {
 
     private static final String APP_TITLE = "Assignment 02";
     private static final int GRID_COLS = 8;
     private static final int COMPLEXITY_YELLOW = 5;
     private static final int COMPLEXITY_RED = 10;
     private static final int SQUARE_SIZE = 80;
 
     private JTextField urlField;
     private JButton okButton;
     private JPanel gridPanel;
     private JTextField selectedFileField;
     private JLabel statusBar;
 
     private final List<FileData> files = new ArrayList<>();
     private int maxLines = 0;
 
     /**
      * Stores data for one file such as name, line count,
      * complexity count, base color, and transparency value.
      */
     private static class FileData {
         final String name;
         final int lines;
         final int complexity;
         final Color baseColor;
         float alpha;
 
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
 
         /**
          * Sets alpha value based on biggest file in the repo.
          * If the file is small, it will look lighter.
          *
          * @param maxLines largest file size in the repo
          */
         void setAlphaFromMax(int maxLines) {
             if (maxLines <= 0) {
                 this.alpha = 0f;
             } else {
                 this.alpha = Math.max(0f, Math.min(1f, (float) lines / maxLines));
             }
         }
     }
 
     /**
      * Sets up the window and UI components.
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
      * Creates the menu bar with File, Action, and Help menus.
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
      * Builds the layout, including the input bar,
      * grid panel, and selected file area.
      */
     private void createUI() {
         JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
         mainPanel.setBackground(new Color(173, 216, 204));
         mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
 
         JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
         inputPanel.setBackground(Color.LIGHT_GRAY);
         inputPanel.setBorder(BorderFactory.createCompoundBorder(
                 BorderFactory.createLineBorder(Color.BLACK),
                 new EmptyBorder(10, 10, 10, 10)
         ));
 
         JLabel urlLabel = new JLabel("GitHub Folder URL:");
         urlField = new JTextField();
         urlField.setFont(new Font("Monospaced", Font.PLAIN, 12));
 
         okButton = new JButton("OK");
         okButton.addActionListener(e -> analyzeRepository());
         okButton.setEnabled(false);
 
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
 
         JPanel centerContainer = new JPanel(new BorderLayout());
         centerContainer.setBackground(Color.LIGHT_GRAY);
         centerContainer.setBorder(BorderFactory.createCompoundBorder(
                 BorderFactory.createLineBorder(Color.BLACK),
                 new EmptyBorder(10, 10, 10, 10)
         ));
 
         gridPanel = new JPanel(new GridLayout(0, GRID_COLS, 2, 2));
         JScrollPane scrollPane = new JScrollPane(gridPanel);
         centerContainer.add(scrollPane, BorderLayout.CENTER);
 
         JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
         bottomPanel.setBackground(Color.LIGHT_GRAY);
         bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                 BorderFactory.createLineBorder(Color.BLACK),
                 new EmptyBorder(10, 10, 10, 10)
         ));
 
         JLabel selectedLabel = new JLabel("Selected File Name:");
         selectedFileField = new JTextField();
         selectedFileField.setEditable(false);
 
         bottomPanel.add(selectedLabel, BorderLayout.WEST);
         bottomPanel.add(selectedFileField, BorderLayout.CENTER);
 
         statusBar = new JLabel(" ");
         statusBar.setBorder(BorderFactory.createCompoundBorder(
                 BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
                 new EmptyBorder(5, 10, 5, 10)
         ));
 
         mainPanel.add(inputPanel, BorderLayout.NORTH);
         mainPanel.add(centerContainer, BorderLayout.CENTER);
         mainPanel.add(bottomPanel, BorderLayout.SOUTH);
 
         add(mainPanel, BorderLayout.CENTER);
         add(statusBar, BorderLayout.SOUTH);
     }
 
     /**
      * Connects to GitHub, finds .java files, counts lines and
      * complexity, then updates the grid. This runs in background
      * so the UI does not freeze.
      */
     private void analyzeRepository() {
         final String url = urlField.getText().trim();
         if (url.isEmpty()) return;
 
         SwingWorker<Void, String> worker = new SwingWorker<>() {
             @Override
             protected Void doInBackground() {
                 try {
                     publish("Fetching...");
                     RepoInfo info = parseGitHubUrl(url);
                     if (info == null) {
                         publish("Invalid GitHub URL");
                         return null;
                     }
 
                     GitHubHandler gh = new GitHubHandler(info.owner, info.repo);
                     List<String> paths = gh.listFiles(info.path);
                     List<String> javaPaths = new ArrayList<>();
                     for (String p : paths) if (p.endsWith(".java")) javaPaths.add(p);
 
                     files.clear();
                     maxLines = 0;
                     int i = 0;
                     for (String filePath : javaPaths) {
                         String name = filePath.substring(filePath.lastIndexOf('/') + 1);
                         String content = gh.getFileContent(filePath);
                         int lines = countNonEmptyLines(content);
                         int comp = countComplexity(content);
                         files.add(new FileData(name, lines, comp));
                         maxLines = Math.max(maxLines, lines);
                         publish("Analyzed " + (++i) + " of " + javaPaths.size());
                     }
                     for (FileData f : files) f.setAlphaFromMax(maxLines);
 
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
                 statusBar.setText(files.size() + " files analyzed");
             }
         };
 
         worker.execute();
     }
 
     /**
      * Simple helper class to store repo info parts.
      */
     private static class RepoInfo {
         final String owner, repo, path;
         RepoInfo(String o, String r, String p) { owner=o; repo=r; path=p; }
     }
 
     /**
      * Breaks down the GitHub URL into owner, repo name, and folder path.
      * Works for normal repo URLs like:
      * https://github.com/user/repo/tree/main/src
      *
      * @param url user-entered GitHub link
      * @return RepoInfo or null if invalid
      */
     private RepoInfo parseGitHubUrl(String url) {
         try {
             URI u = new URI(url);
             String[] parts = u.getPath().split("/");
             String owner = parts[1];
             String repo = parts[2];
             if (parts.length >= 5 && "tree".equals(parts[3])) {
                 StringBuilder p = new StringBuilder();
                 for (int i = 5; i < parts.length; i++) {
                     p.append(parts[i]);
                     if (i < parts.length - 1) p.append("/");
                 }
                 return new RepoInfo(owner, repo, p.toString());
             }
             return new RepoInfo(owner, repo, "");
         } catch (Exception e) {
             return null;
         }
     }
 
     /**
      * Counts only non-empty lines in the file.
      *
      * @param content full source file text
      * @return number of actual code lines
      */
     private int countNonEmptyLines(String content) {
         int c = 0;
         for (String ln : content.split("\n"))
             if (!ln.trim().isEmpty()) c++;
         return c;
     }
 
     /**
      * Counts control statements like if/switch/while/for.
      * Removes comments and strings first so we don't falsely count them.
      */
     private int countComplexity(String content) {
         String code = stripCommentsAndStrings(content);
         int c = 0;
         String[] keys = {"if", "switch", "while", "for"};
         for (String k : keys) {
             Matcher m = Pattern.compile("\\b" + k + "\\b").matcher(code);
             while (m.find()) c++;
         }
         return c;
     }
 
     /**
      * Removes comments and string literal text so
      * complexity check only sees real code.
      */
     private String stripCommentsAndStrings(String src) {
         src = src.replaceAll("(?s)/\\*.*?\\*/", "");
         src = src.replaceAll("(?m)//.*$", "");
         src = src.replaceAll("\"([^\"\\\\]|\\\\.)*\"", "\"\"");
         src = src.replaceAll("'([^'\\\\]|\\\\.)*'", "''");
         return src;
     }
 
     /**
      * Refreshes the grid display with colored squares.
      */
     private void updateGrid() {
         gridPanel.removeAll();
         for (FileData f : files) gridPanel.add(createSquare(f));
         gridPanel.revalidate();
         gridPanel.repaint();
     }
 
     /**
      * Makes a square panel for one file, with tooltip and click behavior.
      */
     private JPanel createSquare(FileData f) {
         JPanel sq = new JPanel() {
             protected void paintComponent(Graphics g) {
                 super.paintComponent(g);
                 int a = (int)(f.alpha * 255);
                 Color draw = new Color(f.baseColor.getRed(), f.baseColor.getGreen(), f.baseColor.getBlue(), a);
                 g.setColor(draw);
                 g.fillRect(0, 0, getWidth(), getHeight());
             }
         };
         sq.setBorder(BorderFactory.createLineBorder(Color.BLACK));
         sq.setPreferredSize(new Dimension(SQUARE_SIZE, SQUARE_SIZE));
         sq.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
         sq.setToolTipText("<html><b>"+f.name+"</b><br>Lines: "+f.lines+"<br>Complexity: "+f.complexity+"</html>");
         sq.addMouseListener(new MouseAdapter() {
             public void mouseClicked(MouseEvent e) {
                 selectedFileField.setText(f.name);
                 highlightSquare(sq);
             }
         });
         return sq;
     }
 
     /**
      * Highlights only the clicked square and resets all others.
      */
     private void highlightSquare(JPanel sq) {
         for (Component c : gridPanel.getComponents())
             if (c instanceof JPanel) ((JPanel)c).setBorder(BorderFactory.createLineBorder(Color.BLACK));
         sq.setBorder(BorderFactory.createLineBorder(Color.BLUE, 3));
     }
 
     /**
      * Clears the grid and status text.
      */
     private void clearGrid() {
         files.clear();
         gridPanel.removeAll();
         selectedFileField.setText("");
         statusBar.setText(" ");
         gridPanel.revalidate();
         gridPanel.repaint();
     }
 
     /**
      * Shows a short message describing the visual encoding.
      */
     private void showAbout() {
         JOptionPane.showMessageDialog(this,
                 "Assignment 02\nColor = complexity\nTransparency = file size",
                 "About", JOptionPane.INFORMATION_MESSAGE);
     }
 
     /**
      * Launches the program.
      */
     public static void main(String[] args) {
         SwingUtilities.invokeLater(() -> {
             try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
             catch (Exception ignored) {}
             new GitHubViz().setVisible(true);
         });
     }
 }
 