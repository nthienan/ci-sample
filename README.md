# CI Sample Application

Scenario 4: New applications that use dynamic credentials

![New applications that use dynamic credentials](docs/scenario-4-vault-dynamic-credential.png)

### Configure Vault policies
Follow these steps to configure Vault in order to run the application

- Login to vault with a user has permissions to create roles and policies.   
  ```bash
  $ vault login -method=userpass username=nthienan
  Password (will be hidden): <enter password here>
  Key                    Value
  ---                    -----
  token                  s.TiaDOUCNHQlfmf97fzx5t0tR
  token_accessor         6gbw8tq7xPDF509AWLn9tNzk
  token_duration         768h
  token_renewable        true
  token_policies         ["admin" "default"]
  identity_policies      []
  policies               ["admin" "default"]
  token_meta_username    nthienan
  ``` 

- Enable the database secrets engine if it is not already enabled:
  ```bash
  $ vault secrets enable database
  Success! Enabled the database secrets engine at: database/
  ```
  
- Configure Vault with the proper plugin and connection information:
  ```bash
  $ vault write database/config/mongodb-server-1 \
      plugin_name=mongodb-database-plugin \
      allowed_roles="ci-sample" \
      connection_url="mongodb://{{username}}:{{password}}@mongo.nthienan.com:27017/admin?ssl=false" \
      username="mongo_root" \
      password="123456"
  ```

- Configure a database role that maps a name in Vault to a MongoDB command that executes and creates the database credential:
  ```bash
  $ vault write database/roles/ci-sample \
      db_name=mongodb-server-1 \
      creation_statements='{ "db": "admin", "roles": [{ "role": "readWrite" }, {"role": "read", "db": "blog"}] }' \
      default_ttl="1h" \
      max_ttl="8760h"
  Success! Data written to: database/roles/ci-sample
  ```

- Create a policy named `read-mongo-credential`:
  ```bash
  $ echo 'path "database/creds/ci-sample" {
    capabilities = ["read"]
  }
  path "sys/leases/renew/ci-sample/*" {
  	capabilities = ["update"]
  }' | vault policy write read-mongo-credential -
  Success! Uploaded policy: read-mongo-credential
  ```
  In this case, tokens assigned to the `read-mongo-credential` policy would have permission to generate a new credential by reading from the `/creds` endpoint with the name of the role.
  
- Create an app role named `ci-sample-dynamic-credentail`:
  ```bash
  $ vault write auth/approle/role/ci-sample-dynamic-credential \
  	secret_id_ttl=5m \
  	period=15m \
  	policies="read-mongo-credential"
  Success! Data written to: auth/approle/role/ci-sample-dynamic-credential
  ```
  This step, we have created a AppRole that will generate periodic tokens associated with policy `read-mongo-credential`.
  It probably makes sense to create AppRole periodic tokens since we are talking about long-running apps that need to be able to renew their token indefinitely.   
  Periodic tokens have a TTL, but no max TTL; therefore, they may live for an infinite duration of time so long as they are renewed within their TTL. This is useful for long-running services that cannot handle regenerating a token

- Create policy for the application to pull role-id and secret-id:
  Now the application will need permissions to retrieve role-id and secret-id for our newly created role.
  ```bash
  $ echo 'path "auth/approle/role/ci-sample-dynamic-credential/role-id" {
    capabilities = ["read"]
  }
  path "auth/approle/role/ci-sample-dynamic-credential/secret-id" {
    capabilities = ["read","create","update"]
  }' | vault policy write ci-sample-dynamic-credential -
  Success! Uploaded policy: ci-sample-dynamic-credential
  ```
  
- Generate a token for the application to login into Vault. This token should have a relatively large TTL
  ```bash
  $ vault token create -policy=ci-sample-dynamic-credential -period="2h"  
  Key                  Value
  ---                  -----
  token                s.RqVbX1wsra8weeX9cYLtSd39
  token_accessor       heWIYcWuqA55dieU7FlGWEKz
  token_duration       2h
  token_renewable      true
  token_policies       ["ci-sample-dynamic-credential" "default"]
  identity_policies    []
  policies             ["ci-sample-dynamic-credential" "default"]
  ```

- Copy newly created token and store it into [src/main/resources/application.properties](src/main/resources/application.properties)
  ```properties
  ...
  vault.uri=http://vault.nthienan.com
  vault.app-role.name=ci-sample-dynamic-credential
  vault.token=s.RqVbX1wsra8weeX9cYLtSd39 # token here
  ```
