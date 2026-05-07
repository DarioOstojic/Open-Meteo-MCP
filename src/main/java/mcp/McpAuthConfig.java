package mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mcp.auth")
public class McpAuthConfig {

    /***
     * Spring Boot's @ConfigurationProperties supports multiple property naming styles:
     * properties# All of these work and map to the same field "apiKey"
     * mcp.auth.apiKey=123
     * mcp.auth.api-key=123
     * mcp.auth.api_key=123
     * mcp.auth.APIKEY=123
     */
    private String apiKey;
    private boolean enabled = true;

    // Getters and setters
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}