# Bouncr

Bouncr is an reverse proxy with authentication and authorization for backend applications.

![bouncer](http://2.bp.blogspot.com/-kVVeXhsM8yU/VIhOpmLlnDI/AAAAAAAApfY/O5N9L72Byo4/s450/job_sp.png)

Bouncer has following features:

- Authenticate
    - Various types of credentials
        - Password
        - LDAP
        - OpenID Connect
        - Client certificate
    - Two factor authentication (using by Google authenticator)
- Authorization (based on Group - Role - Permission)
- Sign in
- Sign out
- Audit
    - Show active sessions
    - Show security activities
- IdP
    - OpenID Connect provider
- Administration pages
    - Manage users
    - Manage groups
    - Manage applications and realms
    - Manage roles
    - Manage OpenID Connect applications

![bouncr architecture](https://i.imgur.com/BXWLGPG.png)

## Build

To build for production,

```
mvn -P \!dev package
```

## Developing

To begin developing, start with a REPL.

```sh
mvn compile exec:java
```

Run `start` to prep and initiate the system.

```
enkan> /start
```

By default this creates a web server at <http://localhost:3000>.


When you make changes to your source files, use `reset` to reload any
modified files and reset the server.

```
enkan> /reset
```

## Docker

You can build the docker image of the api server and the proxy server using by jib.

```
% cd bouncr-api-server
% mvn -P\!dev,postgresql,hazelcast compile jib:dockerBuild
```

```
% cd bouncr-proxy
% mvn -P\!dev,postgresql,hazelcast compile jib:dockerBuild
```

`docker-compose.yml` can run the api server and the proxy server with Hazelcast and Postgresql database.

## License

Copyright © 2017-2018 kawasima

Distributed under the Eclipse Public License, the same as Clojure.
