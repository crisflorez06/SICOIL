package com.SICOIL.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovimientoUpdateRequest {

    @NotNull
    private Integer cantidad;

    @Size(max = 500)
    private String observacion;
}
