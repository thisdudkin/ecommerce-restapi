# Distributed application of the E-Commerce Application

[![Build Status](https://github.com/thisdudkin/ecommerce-restapi/actions/workflows/ci.yml/badge.svg)](https://github.com/thisdudkin/ecommerce-restapi/actions/workflows/ci.yml/badge.svg)

## Starting services locally without Docker

Every microservice is a Spring Boot application and can be started locally using IDE or `../mvnw spring-boot:run` command.
If everything goes well, you can access the following services at given location:
* User Server - http://localhost:8081

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

Once images are ready, you can start them with a single command
`docker compose up -d`.

Containers startup order is coordinated with the `service_healthy` condition of the Docker Compose [depends-on](https://github.com/compose-spec/compose-spec/blob/main/spec.md#depends_on) expression
and the [healthcheck](https://github.com/compose-spec/compose-spec/blob/main/spec.md#healthcheck) of the service containers.

The `main` branch uses an OpenJDK with Java 21 as Docker base image.

*NOTE: Under MacOSX or Windows, make sure that the Docker VM has enough memory to run the microservices.*

## Database initialization

This project uses PostgreSQL as the main database.

Before starting the application, create a clean PostgreSQL container and initialize the database manually using the provided SQL script.
Before running the script, open `docker/script/init.sql` and replace the placeholder password value:

```sql
PASSWORD '?'
