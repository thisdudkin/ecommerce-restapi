# Distributed application of the E-Commerce Application

[![Build Status](https://github.com/thisdudkin/ecommerce-restapi/actions/workflows/ci.yml/badge.svg)](https://github.com/thisdudkin/ecommerce-restapi/actions/workflows/ci.yml/badge.svg)

## Starting services locally without Docker

Every microservice is a Spring Boot application and can be started locally using IDE or `../mvnw spring-boot:run` command.
If everything goes well, you can access the following services at given location:
- API Gateway  - http://localhost:8080
- User Service - http://localhost:8081
- Authentication Service - http://localhost:8082
- Order Service - http://localhost:8083

## Starting services locally with docker-compose

In order to start entire infrastructure using Docker, you have to build images by executing
``bash
./mvnw clean install -P buildDocker
``
This requires `Docker` or `Docker desktop` to be installed and running.

By default, the Docker OCI image is build for an `linux/amd64` platform.
For other architectures, you could change it by using the `-Dcontainer.platform` maven command line argument.
For instance, if you target container images for an Apple M2, you could use the command line with the `linux/arm64` architecture:
```bash
./mvnw clean install -P buildDocker -Dcontainer.platform="linux/arm64"
```

### Required `.env` file in the project root

Before running `docker compose up -d`, create a `.env` file in the root of the project with the following values:
```env
POSTGRES_DB=postgres
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

USERS_DB_PASSWORD=user_service_password
IDENTITY_DB_PASSWORD=authentication_service_password
ORDERS_DB_PASSWORD=order_service_password

CURSOR_SECRET=local-dev-cursor-secret

JWT_PUBLIC_KEY=<put-your-dev-public-key-here>
JWT_PRIVATE_KEY=<put-your-dev-private-key-here>

AUTH_VALIDATE_PATH=/api/v1/auth/validate
AUTH_CACHE_TTL=30s
AUTH_CACHE_KEY_PREFIX=auth:validate:
```

Once images are ready and the `.env` file is created, start everything with:
```bash
docker compose up -d
```

Containers startup order is coordinated with the `service_healthy` condition of the Docker Compose [depends-on](https://github.com/compose-spec/compose-spec/blob/main/spec.md#depends_on) expression
and the [healthcheck](https://github.com/compose-spec/compose-spec/blob/main/spec.md#healthcheck) of the service containers.

The `main` branch uses an OpenJDK with Java 21 as Docker base image.

*NOTE: Under MacOSX or Windows, make sure that the Docker VM has enough memory to run the microservices.*

## Database initialization

This project uses PostgreSQL as the main database.

Before starting the application, create a clean PostgreSQL container and initialize the database manually using the provided SQL script.
Before running the script, open `docker/script/init.sql` and replace the placeholder password value:
```
PASSWORD '?'
```

Replace the placeholders with the same values as:
- `USERS_DB_PASSWORD`
- `IDENTITY_DB_PASSWORD`
- `ORDERS_DB_PASSWORD`
