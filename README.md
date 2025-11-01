# GitHubViz 2.0

A Java Swing application that visualizes code complexity and size metrics for Java files in a GitHub repository. Each file is represented as a colored square in a grid, with color indicating complexity and transparency indicating size.

## Authors
Muska Said Hasan Mustafa and Nick Gottwald

## Features

- **GitHub Repository Analysis**: Fetches and analyzes all `.java` files from a public GitHub folder
- **Visual Grid Display**: Shows each Java file as a square in an 8-column grid
- **Complexity Color Coding**:
  - 🔴 Red: More than 10 conditional/loop statements
  - 🟡 Yellow: More than 5 conditional/loop statements  
  - 🟢 Green: 5 or fewer conditional/loop statements
- **Size Transparency**: Alpha value represents file size (lines of code)
  - Fully transparent: 0 lines
  - Fully opaque: Maximum lines among all files
  - Scaled proportionally in between
- **Interactive Features**:
  - Hover over squares to see filename, line count, and complexity
  - Click squares to select and display filename at bottom
  - Menu options for reloading and clearing data
- **Status Bar**: Shows progress during file fetching and analysis

## Requirements

- Java 11 or higher
- Maven 3.6 or higher
- Internet connection (for fetching GitHub files)

## Installation

1. Clone the repository:
```bash
git clone https://github.com/muskasaid02/GitHubViz.git
cd GitHubViz
```

2. Ensure Maven is installed:
```bash
mvn --version
```

## How to Run

### Using Maven (Recommended)

From the project root directory:

```bash
mvn clean compile
mvn exec:java
```

### Using Command Line

Alternatively, compile and run directly:

```bash
cd src/main/java
javac GitHubViz.java
java GitHubViz
```

## Usage

1. **Launch the application** using one of the methods above
2. **Enter a GitHub folder URL** in the text field at the top
   - Example: `https://github.com/username/repository/tree/main/src`
   - Must be a public repository
3. **Click "OK"** or use **File → Open from URL...** to start analysis
4. **Wait for analysis** - Status bar shows progress
5. **Interact with the grid**:
   - Hover over squares to see file details
   - Click squares to select them
   - Selected filename appears at the bottom

### Menu Options

- **File**
  - Open from URL...: Analyze a new repository
  - Exit: Close the application
- **Action**
  - Reload: Re-analyze the current URL
  - Clear: Clear the grid and reset
- **Help**
  - About: Show application information

## Complexity Calculation

Complexity is measured by counting the following control flow statements:
- `if` statements
- `switch` statements
- `while` loops
- `for` loops

Each occurrence counts as +1 complexity.

## Size Calculation

Size is measured by counting non-empty lines in each Java file.

## Project Structure

```
GitHubViz/
├── src/
│   └── main/
│       └── java/
│           └── GitHubViz.java    # Main application file
├── target/                        # Maven build output
├── pom.xml                        # Maven configuration
├── README.md                      # This file
└── Diagram.png                    # Class diagram
```

## Technologies Used

- **Java Swing**: GUI framework
- **Java AWT**: Graphics and event handling
- **TULIP Library**: Fetches files directly from GitHub API
- **SwingWorker**: Background processing (prevents UI freezing)
- **Maven**: Build and dependency management
- **Regular Expressions**: Parsing GitHub API JSON responses

## Known Limitations

1. **Public Repositories Only**: Cannot access private GitHub repositories (requires authentication)
2. **Rate Limiting**: GitHub API has rate limits for unauthenticated requests (60 requests/hour)
3. **Java Files Only**: Only analyzes `.java` files, ignores other file types
4. **Network Required**: Requires internet connection to fetch files
5. **Simple Complexity Metric**: Only counts basic control flow statements (if, switch, while, for)
   - Does not account for: nested complexity, ternary operators, lambda expressions, or stream operations
6. **Large Repositories**: May be slow for repositories with many files (100+ Java files)
7. **Error Handling**: Limited error messages for invalid URLs or network issues
8. **No Caching**: Re-fetches all files on each analysis (no local caching)

## Future Enhancements

- Add authentication for private repositories
- Cache fetched files locally
- Support for other programming languages
- More sophisticated complexity metrics (cyclomatic complexity)
- Export visualization as image
- Filter files by complexity/size thresholds
- Sort and search functionality

## Troubleshooting

**Issue**: "Cannot find symbol" compilation error
- **Solution**: Ensure you're using Java 11 or higher

**Issue**: Application window doesn't appear
- **Solution**: Check if you're running with proper display settings (macOS may require `java -XstartOnFirstThread`)

**Issue**: "Error fetching files" message
- **Solution**: Verify the GitHub URL is correct and the repository is public

**Issue**: No squares appear in grid
- **Solution**: Ensure the folder contains `.java` files. Check status bar for error messages.

**Issue**: Maven command not found
- **Solution**: Install Maven using `brew install maven` (macOS) or download from https://maven.apache.org

## License

This project is created for educational purposes as part of Assignment 02.

## Contact

For questions or issues, please contact Muska or Nick or open an issue on GitHub.

---

**Assignment 02 - GitHubViz 2.0**  
