-- Datos iniciales para SICOIL
-- Ejecutar este script después de recrear la base para contar con
-- un usuario administrador, algunos clientes y productos base.

INSERT INTO usuarios (usuario, contrasena)
VALUES ('admin', '$2a$10$7EqJtq98hPqEX7fNZaFWoO5ul7x2W.c7xch2c6.pX9vY3uC2raGDa') -- password
ON DUPLICATE KEY UPDATE contrasena = VALUES(contrasena);

INSERT INTO clientes (nombre, telefono, direccion, fecha_registro) VALUES
('Cliente Genérico', '3000000000', 'Calle 1 # 2-3', NOW()),
('Ferretería El Tornillo', '3111111111', 'Cra 10 # 20-30', NOW()),
('Papelería Alfa', '3222222222', 'Av. Principal 45-10', NOW())
ON DUPLICATE KEY UPDATE telefono = VALUES(telefono),
                            direccion = VALUES(direccion);

INSERT INTO productos (nombre, precio_compra, cantidad_por_cajas, stock) VALUES
('Cuaderno argollado grande', 6500, 12, 50),
('Juego de marcadores', 12000, 6, 30),
('Resma papel carta', 9000, 5, 40),
('Pegante blanco 250ml', 3500, 24, 60),
('Cartuchera básica', 7500, 10, 25)
ON DUPLICATE KEY UPDATE precio_compra = VALUES(precio_compra),
                            cantidad_por_cajas = VALUES(cantidad_por_cajas),
                            stock = VALUES(stock);
