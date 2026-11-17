-- =============================================================
-- V2__datos_iniciales.sql (Flyway)
-- Datos ficticios para comprobar el funcionamiento.
-- Copia de base_datos/03_datos_iniciales.sql
-- El stock mostrado es el disponible DESPUÉS de las ventas de ejemplo.
-- =============================================================

INSERT INTO productos (nombre, descripcion, precio, stock, estado) VALUES
('Laptop Lenovo IdeaPad 3',         'Laptop 15.6", Ryzen 5, 8 GB RAM, 512 GB SSD', 11499.00,  8, 'ACTIVO'),
('Mouse inalámbrico Logitech M170', 'Mouse óptico inalámbrico 2.4 GHz',               249.00, 45, 'ACTIVO'),
('Teclado mecánico Redragon K552',  'Teclado mecánico TKL, switches rojos',           699.00, 20, 'ACTIVO'),
('Monitor Samsung 24"',             'Monitor LED Full HD 75 Hz',                     2899.00, 12, 'ACTIVO'),
('Memoria USB Kingston 64 GB',      'USB 3.2 Gen 1',                                  139.00, 97, 'ACTIVO'),
('Disco SSD Kingston 480 GB',       'SSD SATA III 2.5"',                              749.00, 30, 'ACTIVO'),
('Audífonos JBL Tune 510BT',        'Audífonos inalámbricos Bluetooth 5.0',           899.00, 15, 'ACTIVO'),
('Webcam Logitech C270',            'Cámara web HD 720p',                             549.00,  0, 'ACTIVO'),
('Router TP-Link Archer C6',        'Router inalámbrico doble banda AC1200',          799.00, 10, 'ACTIVO'),
('Cable HDMI 2 m',                  'Cable HDMI 2.0 4K (descontinuado)',               99.00, 60, 'INACTIVO');

INSERT INTO ventas (fecha, total, estado) VALUES
('2026-11-02 10:15:00', 11997.00, 'REGISTRADA'),
('2026-11-05 16:40:00', 4015.00, 'REGISTRADA'),
('2026-11-10 12:05:00', 1148.00, 'REGISTRADA');

-- subtotal = cantidad * precio_unitario ; total = suma de subtotales
INSERT INTO detalle_ventas (venta_id, producto_id, cantidad, precio_unitario, subtotal) VALUES
(1, 1, 1, 11499.00, 11499.00),
(1, 2, 2, 249.00, 498.00),
(2, 4, 1, 2899.00, 2899.00),
(2, 5, 3, 139.00, 417.00),
(2, 3, 1, 699.00, 699.00),
(3, 7, 1, 899.00, 899.00),
(3, 2, 1, 249.00, 249.00);
