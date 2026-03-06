package org.example.ecommerce.users.controller;

import org.example.ecommerce.users.exception.handler.RestExceptionHandler;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RestExceptionHandler.class)
public class TestApplication {
}
