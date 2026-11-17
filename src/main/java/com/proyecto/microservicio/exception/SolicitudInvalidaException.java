package com.proyecto.microservicio.exception;

/** Datos de entrada incoherentes que no cubren las anotaciones de validación: HTTP 400. */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
