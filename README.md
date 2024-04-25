## Mini TOTP auth server

Intended to work with Nginx `auth_request`

Uses Micronaut framework

---
### Environment variables

To change the location of the secrets save file, change `SECRETFOLDER` variable to the parent folder of the json.

Example: `SECRETFOLDER=/etc/newfolder`

To change the default port from 8082, set the `MICRONAUT_SERVER_PORT` variable.

Example: `MICRONAUT_SERVER_PORT=9090`

---

TOTP secrets are saved under unique IDs, which are also used as usernames.
TOTP secrets are saved in a json file. 

Default locations:
- Windows: `C:\ProgramData\totpauthserver\secrets.json` - %ALLUSERSPROFILE%
- Linux: `/etc/totpauthserver/secrets.json`

---

Authenticate a client token (via cookie):
`/auth/verify/{id}`
200 "ok" or 401 "unauthorized"


Verify a TOTP code:
`/totp/verify/{id}/{token}`
200 "ok" or 401 "unauthorized"


Generate TOTP secret:
`/totp/new`


Save TOTP secret under an ID:
`/totp/save/{id}/{secret}`


Delete a TOTP secret via ID:
`/totp/delete/{id}`


List available IDs:
`/totp/list`


Serves login page:
`/auth/loginpage`


Login POST endpoint - POST params `id` and `totp`:
`/auth/login`

Logs client out of specific ID:
`/auth/logout/{id}`


---

### Nginx config example

Server accessible on port 8082

Change the internal `/authrequest` endpoint to contain the appropriate totp ID for your authenticated service, so that only the correct TOTP can be used to log in

``` 
location /static/ {
    proxy_pass http://127.0.0.1:8082/static/;
}

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
    proxy_pass http://127.0.0.1:8082/auth/verify/exampleId;
    proxy_set_header Content-Length "";
    proxy_pass_request_body off;
    proxy_set_header Cookie $http_cookie;
    proxy_set_header X-Original-URI $request_uri;
}

location /unauthorized {
    internal;
    proxy_pass http://127.0.0.1:8082/auth/loginpage;
}

location / {
    auth_request /authrequest;
    error_page 401 403 =200 /unauthorized;
    proxy_pass http://127.0.0.1:5800/;
}
```
