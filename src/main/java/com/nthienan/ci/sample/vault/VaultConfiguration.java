package com.nthienan.ci.sample.vault;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.AppRoleAuthentication;
import org.springframework.vault.authentication.AppRoleAuthenticationOptions;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.support.VaultToken;

import java.net.URI;

@Configuration
public class VaultConfiguration extends AbstractVaultConfiguration {

    @Value("${vault.uri}")
    private URI vaultUri;

    @Value("${vault.app-role.name}")
    private String roleName;

    @Value("${vault.token}")
    private String vaultToken;

    @Override
    public VaultEndpoint vaultEndpoint() {
        return VaultEndpoint.from(vaultUri);
    }

    @Override
    public ClientAuthentication clientAuthentication() {
        VaultToken token = VaultToken.of(vaultToken);
        AppRoleAuthenticationOptions options = AppRoleAuthenticationOptions.builder()
            .appRole(roleName)
            .roleId(AppRoleAuthenticationOptions.RoleId.pull(token))
            .secretId(AppRoleAuthenticationOptions.SecretId.pull(token))
            .build();

        return new AppRoleAuthentication(options, restOperations());
    }
}
