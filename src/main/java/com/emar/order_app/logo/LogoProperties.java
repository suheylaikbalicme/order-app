package com.emar.order_app.logo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logo")
public class LogoProperties {

    private boolean enabled = false;

    private String apiGateway;
    private String instance;
    private String tenantId;
    private String firm;

    private final Idm idm = new Idm();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getApiGateway() { return apiGateway; }
    public void setApiGateway(String apiGateway) { this.apiGateway = apiGateway; }

    public String getInstance() { return instance; }
    public void setInstance(String instance) { this.instance = instance; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getFirm() { return firm; }
    public void setFirm(String firm) { this.firm = firm; }

    public Idm getIdm() { return idm; }

    public static class Idm {
        private String tokenUrl;
        private String username;
        private String password;
        private String clientId;
        private String clientSecret;

        public String getTokenUrl() { return tokenUrl; }
        public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    }
}
