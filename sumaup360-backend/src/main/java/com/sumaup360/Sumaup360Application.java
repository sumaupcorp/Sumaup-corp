package com.sumaup360;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend monolito modular de SUMAUP360.
 *
 * El backend es la unica autoridad de roles, permisos, tenant y membresia.
 * Firebase solo prueba la identidad; aqui se decide que puede hacer cada usuario.
 */
@SpringBootApplication
public class Sumaup360Application {

    public static void main(String[] args) {
        SpringApplication.run(Sumaup360Application.class, args);
    }
}
