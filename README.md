# Sample project for Vault's demo
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
      default_ttl="744h" \
      max_ttl="8760h"
  Success! Data written to: database/roles/ci-sample
  ```

- Create a policy named `read-mongo-credential`:
  ```bash
  $ echo 'path "database/creds/ci-sample" {
    capabilities = ["read"]
  }
  path "database/creds/ci-sample/*" {
      capabilities = ["create", "update"]
    }' | vault policy write read-mongo-credential -
  Success! Uploaded policy: read-mongo-credential
  ```
  In this case, tokens assigned to the `read-mongo-credential` policy would have permission to generate a new credential by reading from the `/creds` endpoint with the name of the role.
  
- Create an app role named `ci-sample-dynamic-credentail`:
  ```bash
  $ vault write auth/approle/role/ci-sample-dynamic-credential \
  	secret_id_ttl=20m \
  	token_ttl=15m \
  	token_max_ttl=120m \
  	policies="read-mongo-credential"
  Success! Data written to: auth/approle/role/ci-sample-dynamic-credential
  ```
  This step, we have created a AppRole that will generate tokens associated with policy `read-mongo-credential`.
  Tokens generated through this role have a time-to-live of 15 minutes. That means that after 15 minutes, that tokens are expired and can’t be used anymore.

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
  $ vault token create -policy=ci-sample-dynamic-credential -ttl=8760h 
  WARNING! The following warnings were returned from Vault:
  
    * TTL of "8760h0m0s" exceeded the effective max_ttl of "768h0m0s"; TTL value
    is capped accordingly
  
  Key                  Value
  ---                  -----
  token                s.KMSBsX4zM1YhpWB4FUcCwtFa
  token_accessor       gK6Qy1VgiRx2gp2gPRvm3KFu
  token_duration       768h
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
  vault.token=s.KMSBsX4zM1YhpWB4FUcCwtFa # token here
  ```
