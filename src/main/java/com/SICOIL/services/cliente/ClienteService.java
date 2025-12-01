package com.SICOIL.services.cliente;

import com.SICOIL.dtos.cliente.ClienteRequest;
import com.SICOIL.dtos.cliente.ClienteResponse;
import com.SICOIL.mappers.cliente.ClienteMapper;
import com.SICOIL.models.Cliente;
import com.SICOIL.repositories.ClienteRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con id: " + id));
    }

    public List<ClienteResponse> traerTodos(String nombreFiltro) {
        log.debug("Listando clientes con filtro='{}'", nombreFiltro);
        Specification<Cliente> spec = Specification.where(ClienteSpecification.nombreContains(nombreFiltro));

        return clienteRepository.findAll(spec).stream()
                .map(clienteMapper::entityToResponse)
                .toList();
    }

    @Transactional
    public ClienteResponse crearCliente(ClienteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de cliente es obligatoria.");
        }

        String nombre = request.getNombre();

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio.");
        }

        log.info("Creando cliente '{}'", nombre);

        if (clienteRepository.existsByNombreIgnoreCase(nombre)) {
            log.warn("Intento de crear cliente duplicado '{}'", nombre);
            throw new IllegalArgumentException("Ya existe un cliente con el nombre: " + nombre);
        }

        Cliente cliente = clienteMapper.requestToEntity(request);
        Cliente guardado = clienteRepository.save(cliente);
        return clienteMapper.entityToResponse(guardado);
    }
}
