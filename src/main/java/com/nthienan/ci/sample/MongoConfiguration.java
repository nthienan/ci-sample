package com.nthienan.ci.sample;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.vault.core.RestOperationsCallback;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.lease.LeaseEndpoints;
import org.springframework.vault.core.lease.SecretLeaseContainer;
import org.springframework.vault.core.lease.event.SecretLeaseCreatedEvent;
import org.springframework.vault.support.VaultInitializationResponse;
import org.springframework.vault.support.VaultResponse;
import org.springframework.web.client.RestOperations;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableScheduling
@EnableConfigurationProperties
@ConfigurationProperties("mongo")
public class MongoConfiguration implements InitializingBean {

    @Autowired
    private VaultTemplate vaultTemplate;

    private String host;
    private String port;
    private String database;

    @Value("${vault.db-role}")
    private String dbRole;
    private String username;
    private String password;

    private String leaseId;

    /**
     * Get Mongo credential from Vault by using vault template
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        VaultResponse response = vaultTemplate.read("database/creds/" + dbRole);
        leaseId = response.getLeaseId();
        Map<String, Object> data = response.getData();
        username = (String) data.get("username");
        password = (String) data.get("password");
    }

    /**
     * Auto renewal lease after 15 minutes
     */
    @Scheduled(fixedDelay = 120000)
    protected void renewalVaultLease() {
        vaultTemplate.doWithSession(restOperations -> {
            Map<String, String> payload = new HashMap<>();
            payload.put("lease_id", leaseId);
            HttpEntity<Map> request = new HttpEntity(payload);
            ResponseEntity<VaultInitializationResponse> exchange = restOperations
                .exchange("/sys/leases", HttpMethod.PUT,
                    new HttpEntity<>(request),
                    VaultInitializationResponse.class);
            return exchange.getBody();
        });
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

    public void setDbRole(String dbRole) {
        this.dbRole = dbRole;
    }

    public String connectURI() {
        return "mongodb://" + username + ":" + password + "@" + host + ":" + port;
    }
}
