package com.SICOIL.services.usuario;

import com.SICOIL.models.Usuario;
import com.SICOIL.repositories.UsuarioRepository;
import com.SICOIL.services.security.UsuarioDetails;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

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
