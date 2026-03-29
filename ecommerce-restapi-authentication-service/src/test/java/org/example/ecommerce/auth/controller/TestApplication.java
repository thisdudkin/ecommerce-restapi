package org.example.ecommerce.auth.controller;

import org.example.ecommerce.auth.exception.handler.RestExceptionHandler;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RestExceptionHandler.class)
public class TestApplication {
}
