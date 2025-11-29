package com.SICOIL.config;

import com.SICOIL.models.Usuario;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            Usuario usuario = Usuario.builder()
                    .usuario("admin")
                    .contrasena(passwordEncoder.encode("admin"))
                    .build();
            usuarioRepository.save(usuario);
            LOGGER.info("Usuario por defecto 'admin' creado para acceso inicial.");
        }
    }
}
