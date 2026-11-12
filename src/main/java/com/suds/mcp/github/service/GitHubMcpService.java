package com.suds.mcp.github.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GitHubMcpService {

    private final RestClient restClient;
    private final String token;

    public GitHubMcpService() {
        this.token = System.getenv("GITHUB_TOKEN");
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github+json");
        if (token != null && !token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token);
        }
        this.restClient = builder.build();
    }

    @Tool(name = "github_get_repository", description = "Fetches detailed information about a specific public or private GitHub repository.")
    public String getRepository(String owner, String repo) {
        return this.restClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .retrieve()
                .body(String.class);
    }

    @Tool(name = "github_create_issue", description = "Creates a new issue inside a specified repository.")
    public String createIssue(String owner, String repo, String title, String body) {
        var payload = java.util.Map.of("title", title, "body", body);
        return this.restClient.post()
                .uri("/repos/{owner}/{repo}/issues", owner, repo)
                .body(payload)
                .retrieve()
                .body(String.class);
    }

    @Tool(name = "github_list_repositories", description = "Lists all repositories for the authenticated user (when username is omitted or blank) or for a specified GitHub user. Returns a JSON array of repository objects, paginating through all available pages.")
    public String listRepositories(String username) {
        StringBuilder combined = new StringBuilder("[");
        boolean first = true;
        try {
            int page = 1;
            while (page <= 20) {
                String uri;
                if (username == null || username.isBlank()) {
                    uri = "/user/repos?per_page=100&page=" + page + "&type=all&sort=updated";
                } else {
                    uri = "/users/" + username.trim() + "/repos?per_page=100&page=" + page + "&type=all&sort=updated";
                }
                String body = this.restClient.get()
                        .uri(uri)
                        .retrieve()
                        .body(String.class);
                if (body == null) {
                    break;
                }
                String trimmed = body.trim();
                if (!trimmed.startsWith("[") || trimmed.equals("[]")) {
                    break;
                }
                // strip outer brackets
                String inner = trimmed.substring(1, trimmed.length() - 1).trim();
                if (inner.isEmpty()) {
                    break;
                }
                if (!first) {
                    combined.append(",");
                }
                combined.append(inner);
                first = false;
                // detect last page: fewer than 100 items (rough heuristic via count of top-level objects)
                // Count objects by counting "full_name" occurrences is fragile; instead check if page returned <100 by counting top-level commas at depth 0 is overkill.
                // Use a simpler heuristic: if the raw array length suggests less than 100 objects, stop.
                // We'll just continue; stop when a page returns fewer than 100 by counting '},' at depth 0 approximated by number of "\"id\":" occurrences.
                long count = countOccurrences(trimmed, "\"id\":");
                if (count < 100) {
                    break;
                }
                page++;
            }
            combined.append("]");
            return combined.toString();
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private long countOccurrences(String haystack, String needle) {
        long count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
