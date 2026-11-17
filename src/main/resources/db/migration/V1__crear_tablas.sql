-- =============================================================
-- V1__crear_tablas.sql (Flyway)
-- Modelo relacional de db_microservicios.
-- Copia de base_datos/02_creacion_tablas.sql
-- =============================================================

CREATE TABLE productos (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    nombre              VARCHAR(120)  NOT NULL,
    descripcion         VARCHAR(500)  NULL,
    precio              DECIMAL(10,2) NOT NULL,
    stock               INT           NOT NULL DEFAULT 0,
    estado              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVO',
    version             BIGINT        NOT NULL DEFAULT 0,
    fecha_creacion      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    fecha_actualizacion DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                      ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_productos PRIMARY KEY (id),
    CONSTRAINT uk_productos_nombre UNIQUE (nombre),
    CONSTRAINT ck_productos_precio CHECK (precio >= 0),
    CONSTRAINT ck_productos_stock  CHECK (stock >= 0),
    CONSTRAINT ck_productos_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_productos_estado ON productos (estado);

CREATE TABLE ventas (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    fecha             DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    total             DECIMAL(12,2) NOT NULL DEFAULT 0,
    estado            VARCHAR(20)   NOT NULL DEFAULT 'REGISTRADA',
    fecha_cancelacion DATETIME(6)   NULL,
    CONSTRAINT pk_ventas PRIMARY KEY (id),
    CONSTRAINT ck_ventas_total  CHECK (total >= 0),
    CONSTRAINT ck_ventas_estado CHECK (estado IN ('REGISTRADA', 'CANCELADA'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_ventas_fecha  ON ventas (fecha);
CREATE INDEX idx_ventas_estado ON ventas (estado);

-- Tabla asociativa que resuelve la relación N:M entre ventas y productos
CREATE TABLE detalle_ventas (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    venta_id        BIGINT        NOT NULL,
    producto_id     BIGINT        NOT NULL,
    cantidad        INT           NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_detalle_ventas PRIMARY KEY (id),
    CONSTRAINT fk_detalle_venta    FOREIGN KEY (venta_id)    REFERENCES ventas (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES productos (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uk_detalle_venta_producto UNIQUE (venta_id, producto_id),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_precio   CHECK (precio_unitario >= 0),
    CONSTRAINT ck_detalle_subtotal CHECK (subtotal >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_detalle_producto ON detalle_ventas (producto_id);
