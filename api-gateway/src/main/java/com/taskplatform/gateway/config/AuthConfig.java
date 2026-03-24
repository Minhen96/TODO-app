package com.taskplatform.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "auth.service")
@Getter
@Setter
public class AuthConfig {

    private String url;
    private String validateEndpoint = "/api/auth/validate";

    public String getValidateUrl() {
        return url + validateEndpoint;
    }
}
