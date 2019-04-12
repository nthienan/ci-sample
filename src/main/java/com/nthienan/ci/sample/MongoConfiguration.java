package com.nthienan.ci.sample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("mongo")
public class MongoConfiguration {

    @Autowired
    private VaultTemplate vaultTemplate;

    private String host;
    private String port;
    private String database;
    private String secretPath;
    private String username;
    private String password;

    @PostConstruct
    /**
     * Get Mongo credential from Vault by using vault template
     */
    protected void init() {
        VaultResponse response = vaultTemplate.read(secretPath);
        Map<String, String> data = (HashMap<String, String>) response.getData().get("data");
        username = data.get("username");
        password = data.get("password");
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDatabase() {
        return database;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void setSecretPath(String secretPath) {
        this.secretPath = secretPath;
    }

    public String connectURI() {
        return "mongodb://" + username + ":" + password + "@" + host + ":" + port;
    }
}
