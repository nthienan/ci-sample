# Sample project for Vault's demo
Follow these steps in order to configure Vault before running the application

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

- Create a policy named `read-mongo-secret`:
  ```bash
  $ echo 'path "kv/data/app/ci-sample/mongo" {
    capabilities = ["read", "list"]
  }' | vault policy write read-mongo-secret -
  Success! Uploaded policy: read-mongo-secret
  ```
  In this case, tokens assigned to the `read-mongo-secret` policy would have permission to read a secret on the `kv/data/app/ci-sample/mongo/*` path.
  
- Create an app role named `read-mongo-secret`:
  ```bash
  $ vault write auth/approle/role/read-mongo-secret \
  	secret_id_ttl=20m \
  	token_ttl=15m \
  	token_max_ttl=120m \
  	policies="read-mongo-secret"
  Success! Data written to: auth/approle/role/read-mongo-secret
  ```
  This step, we have created a role that will generate tokens associated with policy `read-mongo-secret`.
  Tokens generated through this role have a time-to-live of 15 minutes. That means that after 15 minutes, that tokens are expired and can’t be used anymore.

- Create policy for the application to pull role-id and secret-id:
  Now the application will need permissions to retrieve role-id and secret-id for our newly created role.
  ```bash
  $ echo 'path "auth/approle/role/read-mongo-secret/role-id" {
    capabilities = ["read"]
  }
  path "auth/approle/role/read-mongo-secret/secret-id" {
    capabilities = ["read","create","update"]
  }' | vault policy write read-mongo-role -
  Success! Uploaded policy: read-mongo-role
  ```
  
- Write mongo credential that the application will consume:
  ```bash
  $ vault kv put kv/app/ci-sample/mongo username=mongo_root password=123456
  Key              Value
  ---              -----
  created_time     2019-04-12T07:14:45.151004401Z
  deletion_time    n/a
  destroyed        false
  version          1
  ```
  
- Generate a token for the application to login into Vault. This token should have a relatively large TTL
  ```bash
  $ vault token create -policy=read-mongo-role -ttl=8760h 
  WARNING! The following warnings were returned from Vault:
  
    * TTL of "8760h0m0s" exceeded the effective max_ttl of "768h0m0s"; TTL value
    is capped accordingly
  
  Key                  Value
  ---                  -----
  token                s.Ca8jBqeaLgdYCUKhC4wKd7ko
  token_accessor       exFSntG06zBTRVgz09OAWpIx
  token_duration       768h
  token_renewable      true
  token_policies       ["default" "read-mongo-role"]
  identity_policies    []
  policies             ["default" "read-mongo-role"]
  ```

- Copy newly created token and store it into [src/main/resources/application.properties](src/main/resources/application.properties)
  ```properties
  ...
  vault.uri=http://vault.nthienan.com
  vault.app-role.name=read-mongo-secret
  vault.token=s.Ca8jBqeaLgdYCUKhC4wKd7ko # paste token here
  ```
