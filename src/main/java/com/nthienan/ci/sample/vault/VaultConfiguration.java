package com.nthienan.ci.sample.vault;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@Configuration
public class VaultConfiguration extends AbstractVaultConfiguration {

    @Value("${vault.token-file}")
    private String tokenFilePath;

    private URI vaultUri;
    private String vaultToken;

    @PostConstruct
    protected void init() throws IOException, URISyntaxException {
        File tokenFile = new File(tokenFilePath);
        Map<String, String> vaultInfo = new ObjectMapper().readValue(tokenFile, new TypeReference<Map<String, String>>() {});
        vaultUri = new URI(vaultInfo.get("vaultAddr"));
        vaultToken = vaultInfo.get("clientToken");
    }

    @Override
    public VaultEndpoint vaultEndpoint() {
        return VaultEndpoint.from(vaultUri);
    }

    @Override
    public ClientAuthentication clientAuthentication() {
        return new TokenAuthentication(vaultToken);
    }
}
