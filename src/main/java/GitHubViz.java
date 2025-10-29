
/**
 * @author Muska Said Hasan Mustafa and Nick Gottwald
 * @version 2.0
 * @date October 29, 2025
 */

 import javax.swing.*;
 import javax.swing.border.EmptyBorder;
 import java.awt.*;
 import java.awt.event.*;
 import java.io.BufferedReader;
 import java.io.InputStreamReader;
 import java.net.URI;
 import java.net.URL;
 import java.net.HttpURLConnection;
 import java.util.*;
 import java.util.List;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 public class GitHubViz extends JFrame {
     private JTextField urlField;
     private JPanel gridPanel;
     private JTextField selectedFileField;
     private JLabel statusBar;
     private List<FileData> files;
     private int maxLines = 0;
     private static final int GRID_COLS = 8;
     
     static class FileData {
         String name;
         int lines;
         int complexity;
         Color color;
         float alpha;
         
         FileData(String name, int lines, int complexity) {
             this.name = name;
             this.lines = lines;
             this.complexity = complexity;
             
             // Determine color based on complexity
             if (complexity > 10) {
                 this.color = new Color(255, 0, 0);
             } else if (complexity > 5) {
                 this.color = new Color(255, 255, 0);
             } else {
                 this.color = new Color(0, 255, 0);
             }
         }
         
         void setAlpha(int maxLines) {
             if (maxLines == 0) {
                 this.alpha = 0f;
             } else {
                 this.alpha = (float) lines / maxLines;
             }
         }
     }
     
     public GitHubViz() {
         setTitle("GitHubViz 2.0");
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         setSize(1000, 700);
         setLocationRelativeTo(null);
         
         files = new ArrayList<>();
         
         createMenuBar();
         createUI();
     }
     
     private void createMenuBar() {
         JMenuBar menuBar = new JMenuBar();
         
         // File menu
         JMenu fileMenu = new JMenu("File");
         JMenuItem openItem = new JMenuItem("Open from URL...");
         openItem.addActionListener(e -> analyzeRepository());
         fileMenu.add(openItem);
         fileMenu.addSeparator();
         JMenuItem exitItem = new JMenuItem("Exit");
         exitItem.addActionListener(e -> System.exit(0));
         fileMenu.add(exitItem);
         
         // Action menu
         JMenu actionMenu = new JMenu("Action");
         JMenuItem reloadItem = new JMenuItem("Reload");
         reloadItem.addActionListener(e -> analyzeRepository());
         actionMenu.add(reloadItem);
         JMenuItem clearItem = new JMenuItem("Clear");
         clearItem.addActionListener(e -> clearGrid());
         actionMenu.add(clearItem);
         
         // Help menu
         JMenu helpMenu = new JMenu("Help");
         JMenuItem aboutItem = new JMenuItem("About");
         aboutItem.addActionListener(e -> showAbout());
         helpMenu.add(aboutItem);
         
         menuBar.add(fileMenu);
         menuBar.add(actionMenu);
         menuBar.add(helpMenu);
         
         setJMenuBar(menuBar);
     }
     
     private void createUI() {
         JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
         mainPanel.setBackground(new Color(173, 216, 204));
         mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
         
         // Top panel with title and input
         JPanel topPanel = new JPanel(new BorderLayout(10, 10));
         topPanel.setBackground(new Color(173, 216, 204));
         
         JLabel titleLabel = new JLabel("Title");
         titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
         titleLabel.setOpaque(true);
         titleLabel.setBackground(Color.LIGHT_GRAY);
         titleLabel.setBorder(BorderFactory.createCompoundBorder(
             BorderFactory.createLineBorder(Color.BLACK),
             new EmptyBorder(5, 5, 5, 5)
         ));
         
         JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
         inputPanel.setBackground(Color.LIGHT_GRAY);
         inputPanel.setBorder(BorderFactory.createCompoundBorder(
             BorderFactory.createLineBorder(Color.BLACK),
             new EmptyBorder(10, 10, 10, 10)
         ));
         
         urlField = new JTextField("GitHub Folder URL");
         urlField.setFont(new Font("Monospaced", Font.PLAIN, 12));
         
         JButton okButton = new JButton("OK");
         okButton.setPreferredSize(new Dimension(100, 30));
         okButton.addActionListener(e -> analyzeRepository());
         
         inputPanel.add(urlField, BorderLayout.CENTER);
         inputPanel.add(okButton, BorderLayout.EAST);
         
         topPanel.add(titleLabel, BorderLayout.NORTH);
         topPanel.add(inputPanel, BorderLayout.CENTER);
         
         // Center panel with grid
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
         
         // Bottom panel with selected file
         JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
         bottomPanel.setBackground(Color.LIGHT_GRAY);
         bottomPanel.setBorder(BorderFactory.createCompoundBorder(
             BorderFactory.createLineBorder(Color.BLACK),
             new EmptyBorder(10, 10, 10, 10)
         ));
         
         JLabel selectedLabel = new JLabel("Selected File Name:");
         selectedLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
         
         selectedFileField = new JTextField("text");
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
         
         mainPanel.add(topPanel, BorderLayout.NORTH);
         mainPanel.add(centerContainer, BorderLayout.CENTER);
         mainPanel.add(bottomPanel, BorderLayout.SOUTH);
         
         add(mainPanel, BorderLayout.CENTER);
         add(statusBar, BorderLayout.SOUTH);
     }
     
     private void analyzeRepository() {
         String url = urlField.getText().trim();
         if (url.isEmpty() || url.equals("GitHub Folder URL")) {
             JOptionPane.showMessageDialog(this, 
                 "Please enter a valid GitHub folder URL", 
                 "Error", JOptionPane.ERROR_MESSAGE);
             return;
         }
         
         SwingWorker<Void, String> worker = new SwingWorker<>() {
             @Override
             protected Void doInBackground() throws Exception {
                 publish("Fetching...");
                 
                 // Convert GitHub URL to raw content URL
                 String apiUrl = convertToApiUrl(url);
                 List<String> javaFiles = fetchJavaFiles(apiUrl);
                 
                 publish(javaFiles.size() + " files found. Analyzing...");
                 
                 files.clear();
                 maxLines = 0;
                 
                 for (String fileUrl : javaFiles) {
                     String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
                     String content = fetchFileContent(fileUrl);
                     
                     int lines = countNonEmptyLines(content);
                     int complexity = countComplexity(content);
                     
                     files.add(new FileData(fileName, lines, complexity));
                     maxLines = Math.max(maxLines, lines);
                     
                     publish("Analyzed " + files.size() + " of " + javaFiles.size() + " files");
                 }
                 
                 // Set alpha values
                 for (FileData file : files) {
                     file.setAlpha(maxLines);
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
     
     private String convertToApiUrl(String url) {
         // Convert GitHub web URL to API URL
         if (url.contains("github.com") && !url.contains("api.github.com")) {
             url = url.replace("github.com", "api.github.com/repos");
             url = url.replace("/tree/", "/contents/");
             if (url.contains("/blob/")) {
                 url = url.replace("/blob/", "/contents/");
             }
         }
         return url;
     }
     
     private List<String> fetchJavaFiles(String url) throws Exception {
         List<String> javaFiles = new ArrayList<>();
         
         URI uri = new URI(url);
         HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
         conn.setRequestMethod("GET");
         conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
         
         BufferedReader reader = new BufferedReader(
             new InputStreamReader(conn.getInputStream()));
         
         StringBuilder response = new StringBuilder();
         String line;
         while ((line = reader.readLine()) != null) {
             response.append(line);
         }
         reader.close();
         conn.disconnect();
         
         // Parse JSON response to find .java files
         String json = response.toString();
         Pattern pattern = Pattern.compile("\"download_url\":\\s*\"([^\"]+\\.java)\"");
         Matcher matcher = pattern.matcher(json);
         
         while (matcher.find()) {
             javaFiles.add(matcher.group(1));
         }
         
         return javaFiles;
     }
     
     private String fetchFileContent(String fileUrl) throws Exception {
         HttpURLConnection conn = (HttpURLConnection) new URI(fileUrl).toURL().openConnection();
         conn.setRequestMethod("GET");
         
         BufferedReader reader = new BufferedReader(
             new InputStreamReader(conn.getInputStream()));
         
         StringBuilder content = new StringBuilder();
         String line;
         while ((line = reader.readLine()) != null) {
             content.append(line).append("\n");
         }
         reader.close();
         conn.disconnect();
         
         return content.toString();
     }
     
     private int countNonEmptyLines(String content) {
         String[] lines = content.split("\n");
         int count = 0;
         for (String line : lines) {
             if (!line.trim().isEmpty()) {
                 count++;
             }
         }
         return count;
     }
     
     private int countComplexity(String content) {
         int complexity = 0;
         String[] keywords = {"if", "switch", "while", "for"};
         
         for (String keyword : keywords) {
             Pattern pattern = Pattern.compile("\\b" + keyword + "\\b");
             Matcher matcher = pattern.matcher(content);
             while (matcher.find()) {
                 complexity++;
             }
         }
         
         return complexity;
     }
     
     private void updateGrid() {
         gridPanel.removeAll();
         
         for (FileData file : files) {
             JPanel square = createSquare(file);
             gridPanel.add(square);
         }
         
         gridPanel.revalidate();
         gridPanel.repaint();
     }
     
     private JPanel createSquare(FileData file) {
         JPanel square = new JPanel() {
             @Override
             protected void paintComponent(Graphics g) {
                 super.paintComponent(g);
                 Graphics2D g2d = (Graphics2D) g;
                 g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                     RenderingHints.VALUE_ANTIALIAS_ON);
                 
                 Color drawColor = new Color(
                     file.color.getRed(),
                     file.color.getGreen(),
                     file.color.getBlue(),
                     (int)(file.alpha * 255)
                 );
                 
                 g2d.setColor(drawColor);
                 g2d.fillRect(0, 0, getWidth(), getHeight());
             }
         };
         
         square.setBorder(BorderFactory.createLineBorder(Color.BLACK));
         square.setPreferredSize(new Dimension(80, 80));
         square.setCursor(new Cursor(Cursor.HAND_CURSOR));
         
         // Tooltip
         square.setToolTipText(String.format(
             "<html><b>%s</b><br>Lines: %d<br>Complexity: %d</html>",
             file.name, file.lines, file.complexity
         ));
         
         // Click listener
         square.addMouseListener(new MouseAdapter() {
             @Override
             public void mouseClicked(MouseEvent e) {
                 selectedFileField.setText(file.name);
                 highlightSquare(square);
             }
         });
         
         return square;
     }
     
     private void highlightSquare(JPanel square) {
         // Remove highlight from all squares
         for (Component comp : gridPanel.getComponents()) {
             if (comp instanceof JPanel) {
                 ((JPanel)comp).setBorder(BorderFactory.createLineBorder(Color.BLACK));
             }
         }
         // Highlight selected square
         square.setBorder(BorderFactory.createLineBorder(Color.BLUE, 3));
     }
     
     private void clearGrid() {
         files.clear();
         gridPanel.removeAll();
         selectedFileField.setText("");
         statusBar.setText(" ");
         gridPanel.revalidate();
         gridPanel.repaint();
     }
     
     private void showAbout() {
         String message = "GitHubViz 2.0\n\n" +
             "Visualizes Java files from GitHub repositories.\n\n" +
             "Color represents complexity:\n" +
             "• Red: > 10 control statements\n" +
             "• Yellow: > 5 control statements\n" +
             "• Green: ≤ 5 control statements\n\n" +
             "Transparency represents size (lines of code)";
         
         JOptionPane.showMessageDialog(this, message, "About", 
             JOptionPane.INFORMATION_MESSAGE);
     }
     
     public static void main(String[] args) {
         SwingUtilities.invokeLater(() -> {
             try {
                 UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
             } catch (Exception e) {
                 e.printStackTrace();
             }
             
             GitHubViz app = new GitHubViz();
             app.setVisible(true);
         });
     }
 }