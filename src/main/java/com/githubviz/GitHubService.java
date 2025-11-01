package com.githubviz;

import javiergs.tulip.GitHubHandler;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles GitHub operations via TULIP library:
 * - Parse GitHub URLs
 * - List Java files in a repo folder
 * - Fetch file contents
 *
 * @author Muska Said Hasan Mustafa
    @author Nick Gottwald
 */
public class GitHubService {

    /**
     * Parses a GitHub tree URL into owner/repo/path pieces.
     *
     * @param url GitHub folder URL
     * @return RepoInfo or null if invalid
     */
    public RepoInfo parseGitHubUrl(String url) {
        try {
            URI u = new URI(url);
            String[] parts = u.getPath().split("/");
            String owner = parts[1];
            String repo = parts[2];

            if (parts.length >= 5 && parts[3].equals("tree")) {
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
     * Lists all .java files in the given repo path.
     *
     * @param info repo info
     * @return list of .java file paths
     * @throws Exception if GitHub/TULIP fails
     */
    public List<String> listJavaFiles(RepoInfo info) throws Exception {
        GitHubHandler gh = new GitHubHandler(info.owner, info.repo);
        List<String> paths = gh.listFiles(info.path);
        List<String> javaPaths = new ArrayList<>();

        for (String p : paths) {
            if (p.toLowerCase().endsWith(".java")) javaPaths.add(p);
        }
        return javaPaths;
    }

    /**
     * Downloads content for a given file path.
     *
     * @param info repo info
     * @param filePath file inside repo
     * @return file content
     * @throws Exception if fetch fails
     */
    public String getFileContent(RepoInfo info, String filePath) throws Exception {
        GitHubHandler gh = new GitHubHandler(info.owner, info.repo);
        return gh.getFileContent(filePath);
    }
}
