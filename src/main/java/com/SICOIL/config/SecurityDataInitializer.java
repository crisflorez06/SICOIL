package com.SICOIL.config;

import com.SICOIL.models.Cliente;
import com.SICOIL.models.Producto;
import com.SICOIL.models.Usuario;
import com.SICOIL.repositories.ClienteRepository;
import com.SICOIL.repositories.ProductoRepository;
import com.SICOIL.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityDataInitializer implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityDataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsuarios();
        seedClientes();
        seedProductos();
    }

    private void seedUsuarios() {
        if (usuarioRepository.count() > 0) {
            return;
        }
        Usuario admin = Usuario.builder()
                .usuario("admin")
                .contrasena(passwordEncoder.encode("admin"))
                .build();
        usuarioRepository.save(admin);
        LOGGER.info("Usuario por defecto 'admin' creado para acceso inicial.");
    }

    private void seedClientes() {
        if (clienteRepository.count() > 0) {
            return;
        }
        Cliente generico = Cliente.builder()
                .nombre("Cliente Genérico")
                .telefono("3000000000")
                .direccion("Calle 1 # 2-3")
                .build();
        Cliente ferreteria = Cliente.builder()
                .nombre("Ferretería El Tornillo")
                .telefono("3111111111")
                .direccion("Cra 10 # 20-30")
                .build();
        Cliente papeleria = Cliente.builder()
                .nombre("Papelería Alfa")
                .telefono("3222222222")
                .direccion("Av. Principal 45-10")
                .build();
        clienteRepository.save(generico);
        clienteRepository.save(ferreteria);
        clienteRepository.save(papeleria);
        LOGGER.info("Clientes base insertados.");
    }

    private void seedProductos() {
        if (productoRepository.count() > 0) {
            return;
        }
        productoRepository.save(new Producto("Cuaderno argollado grande", 6500d, 12, 50));
        productoRepository.save(new Producto("Juego de marcadores", 12000d, 6, 30));
        productoRepository.save(new Producto("Resma papel carta", 9000d, 5, 40));
        productoRepository.save(new Producto("Pegante blanco 250ml", 3500d, 24, 60));
        productoRepository.save(new Producto("Cartuchera básica", 7500d, 10, 25));
        LOGGER.info("Productos base insertados.");
    }
}
