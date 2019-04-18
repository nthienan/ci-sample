# CI Sample Application

Scenario 3: New applications that use static credentials

![New applications that use static credentials](docs/scenario-3-vault-static-credential.png)

### Configure Vault policies
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

- Create a policy named `read-mongo-credential`:
  ```bash
  $ echo 'path "kv/data/app/ci-sample/mongo" {
    capabilities = ["read", "list"]
  }' | vault policy write read-mongo-credential -
  Success! Uploaded policy: read-mongo-credential
  ```
  In this case, tokens assigned to the `read-mongo-credential` policy would have permission to read a secret on the `kv/data/app/ci-sample/mongo/*` path.
  
- Create an app role named `ci-sample`:
  ```bash
  $ vault write auth/approle/role/ci-sample \
  	secret_id_ttl=20m \
  	token_ttl=15m \
  	token_max_ttl=120m \
  	policies="read-mongo-credential"
  Success! Data written to: auth/approle/role/ci-sample
  ```
  This step, we have created a role that will generate tokens associated with policy `read-mongo-credential`.
  Tokens generated through this role have a time-to-live of 15 minutes. That means that after 15 minutes, that tokens are expired and can’t be used anymore.

- Create policy for the application to pull role-id and secret-id:
  Now the application will need permissions to retrieve role-id and secret-id for our newly created role.
  ```bash
  $ echo 'path "auth/approle/role/ci-sample/role-id" {
    capabilities = ["read"]
  }
  path "auth/approle/role/ci-sample/secret-id" {
    capabilities = ["read","create","update"]
  }' | vault policy write ci-sample -
  Success! Uploaded policy: ci-sample
  ```
  
- Write mongo credential that the application will consume:
  ```bash
  $ vault kv put kv/app/ci-sample/mongo username=root password=root
  Key              Value
  ---              -----
  created_time     2019-04-12T07:14:45.151004401Z
  deletion_time    n/a
  destroyed        false
  version          1
  ```
  
- Generate a token for the application to login into Vault. This token should have a relatively large TTL
  ```bash
  $ vault token create -policy=ci-sample -ttl=8760h 
  Key                  Value
  ---                  -----
  token                s.eC4Foh4exoIfLyxqIKIniGC3
  token_accessor       nA0jZ5eT4kqZwW5Pcjmvn9el
  token_duration       8760h
  token_renewable      true
  token_policies       ["ci-sample" "default"]
  identity_policies    []
  policies             ["ci-sample" "default"]
  ```

- Copy newly created token and store it into [src/main/resources/application.properties](src/main/resources/application.properties)
  ```properties
  ...
  vault.uri=http://vault.nthienan.com
  vault.app-role.name=ci-sample
  vault.token=s.eC4Foh4exoIfLyxqIKIniGC3 # paste token here
  ```
