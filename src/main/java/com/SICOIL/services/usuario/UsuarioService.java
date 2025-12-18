package com.SICOIL.services.usuario;

import com.SICOIL.dtos.usuario.UsuarioRequest;
import com.SICOIL.dtos.usuario.UsuarioResponse;
import com.SICOIL.models.Usuario;
import com.SICOIL.repositories.UsuarioRepository;
import com.SICOIL.services.security.UsuarioDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse crearUsuario(UsuarioRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de usuario es obligatoria.");
        }

        String usuario = request.getUsuario();
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        usuario = usuario.trim();

        String contrasena = request.getContrasena();
        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }

        if (usuarioRepository.existsByUsuarioIgnoreCase(usuario)) {
            throw new IllegalArgumentException("Ya existe un usuario con el nombre: " + usuario);
        }

        Usuario usuarioNuevo = Usuario.builder()
                .usuario(usuario)
                .contrasena(contrasena)
                .build();

        Usuario guardado = crear(usuarioNuevo);
        return UsuarioResponse.builder()
                .id(guardado.getId())
                .usuario(guardado.getUsuario())
                .build();
    }

    public Usuario crear(Usuario usuario) {
        String passwordEncriptado = passwordEncoder.encode(usuario.getContrasena());
        usuario.setContrasena(passwordEncriptado);
        return usuarioRepository.save(usuario);
    }

    public Usuario obtenerUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioDetails usuarioDetails)) {
            throw new EntityNotFoundException("No hay un usuario autenticado en el contexto actual.");
        }
        return usuarioDetails.getUsuario();
    }
}
