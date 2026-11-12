package com.suds.mcp;

import com.suds.mcp.github.service.GitHubMcpService;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class SudsMcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(SudsMcpApplication.class, args);
	}

	@Bean
	public List<ToolCallback> githubTools(GitHubMcpService service) {
		return List.of(ToolCallbacks.from(service));
	}

}
