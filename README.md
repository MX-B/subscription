
Moneyex Payments - Subscriptions
=====================
A service to manage subscriptions and APIs for the Innovation Cloud

## docker-compose.yml

```
version: '2.1'

services:

  usage:
    hostname: usage
    image: 
    ulimits:
      memlock:
        soft: -1
        hard: -1
      nofile: 65536
    ports:
      - "8080:8080"
    environment:
      - LOG_LEVEL=INFO
      - PORT=8080
      - JDBC_CONNECTION_STRING=
      - DB_USER=
      - DB_PASSWORD=
      - AUTH_CLIENT_SECRET=
      - AUTH_CLIENT=
      - AUTH_REALM=
      - AUTH_SERVER_URL= 
```

## Environment Configuration

| ENV Name                                  | Description                                           | Required | Default Value                                                                                               |
|-------------------------------------------|-------------------------------------------------------|----------|-------------------------------------------------------------------------------------------------------------|
| `JDBC_CONNECTION_STRING`                  | Used to create connection with the database           | **Yes**  | `jdbc:mysql://localhost:3306/Moneyex_ic_usage?useLegacyDatetimeCode=false&useTimezone=true&serverTimezone=UTC` |
| `DB_USER`                                 | MySQL Database User                                   | No       | `<blank>`                                                                                                   |
| `DB_PASSWORD`                             | MySQL Database Password                               | No       | `<blank>`                                                                                                   |
| `PORT`                                    | TCP Port where the server will run                    | No       | `8080`                                                                                                      |
| `LOG_LEVEL`                               | Log level                                             | No       | `INFO`                                                                                                      |
| `AUTH_CLIENT_SECRET`                      | Secret to authenticate with Keycloak                  | **Yes**  |                                                                                                             |
| `AUTH_CLIENT`                             | ClientID to authenticate with Keycloak                | No       | `Moneyex-ic-usage`                                                                                             |
| `AUTH_REALM`                              | Realm to authenticate with Keycloak                   | No       | `innovation-cloud`                                                                                          |
| `AUTH_SERVER_URL`                         | Keycloak Auth URL                                     | No       | `https://is.dev.Moneyex.io/auth`                                                                               |
| `Moneyex_BRIDGE_URL`                         | Address to Bridge Service Endpoint                    | No       | `https://bridge.dev.Moneyex.io`                                                               |
| `Moneyex_WHITELABEL_URL`                     | Address to Whitelabel Service Endpoint                | No       | `http://localhost:8084`                                                                                     |
| `PLAN_CRON`                               | A cron string (Quartz) for the Plan Sync with Bridge  | No       | `0 0 * * * ?` (Every hours)                                                                                 |

## External services

1. Uses `MySQL` to store metrics data
2. Uses `Keycloak` to authenticate between services and also to authenticate endpoints
3. Uses `Google Cloud Storage` to store Tenant Logo
4. Uses `Bridge` to check API Endpoints
5. Uses `Whitelabel Manager` to manage whitelabels

## Keycloak

- Client ID: `Moneyex-payments-subscriptions`
- Service Account Enabled: **Yes**

### Roles

| Name                | Description                | Composite | Composite Roles |
|---------------------|----------------------------|-----------|-----------------|
| `user`              | Role to read only          | No        | -               |
| `admin`             | Admin role with all roles  | **Yes**   | `user`          |

### Service Account Roles

| Client ID                 | Required Roles | Description                                     |
|---------------------------|----------------|-------------------------------------------------|
| `Moneyex-whitelabel-manager` | `admin`        | Uses to create, list, read and update Tenant    |

## Development

### Install

This project doesn't require installation

### Run

Run as java application in Eclipse or IntelliJ or Use docker-compose as mentioned above

### Run Tests

First configure some env variables

| ENV Name      | Description                | Required | Default Value   |
|---------------|----------------------------|----------|-----------------|
| `DB_URL`      | Address to MySQL database  | No       | `localhost`     |
| `DB_PORT`     | Port to MySQL database     | No       | `3306`          |
| `DB_NAME`     | Database schema            | No       | `Moneyex_test`     |
| `DB_USER`     | MySQL database User        | No       | `root`          |
| `DB_PASSWORD` | MySQL database password    | No       | `passw0rd`      |

Then run:

```
mvn test
```