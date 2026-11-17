package com.proyecto.microservicio.exception;

/** Violación de una regla de negocio por el estado actual del recurso: HTTP 409 Conflict. */
public class ConflictoNegocioException extends RuntimeException {
    public ConflictoNegocioException(String mensaje) {
        super(mensaje);
    }
}
