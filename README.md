## Mini TOTP auth server

Intended to work with Nginx `auth_request`

Uses Micronaut framework

---

### Warning

Do not expose the server port, TOTP secret deleting and adding is not authenticated.

Secrets cannot be viewed via http and must be inspected in secrets.json.

---
### Environment variables

- To change the location of the secrets save file, change `SECRETFOLDER` variable to the parent folder of the json.

    Example: `SECRETFOLDER=/etc/newfolder`


-  To change the default port from 8082, set the `MICRONAUT_SERVER_PORT` variable. 

    Example: `MICRONAUT_SERVER_PORT=9090`


- To change the default auth token TTL from 300s, set `TOKENTTL` in seconds.

    Example: `TOKENTTL=100`

---

Cookie format is `authtoken-someId`

Each secret ID has its own cookie.

TOTP secrets are saved under unique IDs, which are also used as usernames.
TOTP secrets are saved in a json file. 

All secrets are loaded in memory at startup. Any changes to secrets.json must be loaded with 
`/totp/reload/{logoutAll}`

If secrets.json becomes corrupted or inaccessible, certain functions such as saving or deleting secrets becomes disabled. Any issues have to be fixed manually. When any issues are fixed, the reload endpoint can be called.

Default locations:
- Windows: `C:\ProgramData\totpauthserver\secrets.json` - %ALLUSERSPROFILE%
- Linux: `/etc/totpauthserver/secrets.json`

---


`/logs/{amount}`
Shows selected number of last logs


`/auth/loginpage`
Serves login page


`/auth/login`
Login POST endpoint - POST params `id` and `totp`


`/totp/list`
List available IDs


`/totp/save/{id}/{ttl}/{secret}`
Save TOTP secret under an ID, with seconds TTL


`/totp/delete/{id}`
Delete a TOTP secret via ID


`/totp/new`
Generate TOTP secret


`/auth/logout/{id}`
Logs client out of specific ID


`/totp/reload/{logout}`
Reloads secrets from the file, with logout all users option (boolean)


Below endpoints return <ins>200</ins> "ok" or <ins>401</ins> "unauthorized" (or 400 if input is invalid)


`/auth/verify/{id}`
Authenticate a client token (via cookie)


`/auth/verify/{id}/{token}`
Authenticate a client token


`/totp/verify/{id}/{code}`
Verify a TOTP code


---

### Nginx config example

Server accessible on port 8082

Change the internal `/authrequest` endpoint to contain the appropriate totp ID for your authenticated service, so that only the correct TOTP can be used to log in

``` 
location /auth/login {
    auth_request off;
    proxy_pass http://127.0.0.1:8082/auth/login;
}

location /totp/list {
    auth_request off;
    proxy_pass http://127.0.0.1:8082/totp/list;
}

location = /authrequest {
    internal;
    proxy_pass http://127.0.0.1:8082/auth/verify/exampleId;  # App-specific ID
    proxy_set_header Content-Length "";
    proxy_pass_request_body off;
    proxy_set_header Cookie $http_cookie;
    proxy_set_header X-Original-URI $request_uri;
}

location /unauthorized {
    internal;
    proxy_pass http://127.0.0.1:8082/auth/loginpage;
}

location /myLogoutPath {
    auth_request /authrequest;
    error_page 401 403 =200 /unauthorized;
    proxy_pass http://127.0.0.1:8082/auth/logout/exampleId;  # App-specific ID
}

location / {
    auth_request /authrequest;
    error_page 401 403 =200 /unauthorized;
    proxy_pass http://127.0.0.1:5800/;
}
```

---

### Other information

Homepage uses **compiled** Tailwind CSS. 