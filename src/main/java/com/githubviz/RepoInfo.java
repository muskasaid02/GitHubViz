package com.githubviz;

/**
 * Simple data class that stores GitHub repository parts:
 * owner, repo name, and path inside repo.
 *
 * Example:
 * https://github.com/user/repo/tree/main/src
 * owner = user, repo = repo, path = src
 *
 * @author Muska Said Hasan Mustafa
 * @author Nick Gottwald
 */
public class RepoInfo {
    public final String owner;
    public final String repo;
    public final String path;

    public RepoInfo(String owner, String repo, String path) {
        this.owner = owner;
        this.repo = repo;
        this.path = path;
    }
}
