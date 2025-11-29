package com.SICOIL.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nombre;

    @NotNull
    @PositiveOrZero
    @Column(name = "precio_compra", nullable = false)
    private Double precioCompra;

    @NotNull
    @PositiveOrZero
    @Column(name = "cantidad_por_cajas", nullable = false)
    private Integer cantidadPorCajas;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer stock;

    public Producto(String nombre,
                    Double precioCompra,
                    Integer cantidadPorCajas,
                    Integer stock) {
        this.nombre = nombre;
        this.precioCompra = precioCompra;
        this.cantidadPorCajas = cantidadPorCajas;
        this.stock = stock;
    }


}
