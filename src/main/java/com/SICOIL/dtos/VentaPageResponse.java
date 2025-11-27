package com.SICOIL.dtos;

import org.springframework.data.domain.Page;
import java.math.BigDecimal;

public class VentaPageResponse {
    private Page<VentaResponse> ventas;
    private BigDecimal totalGeneral;

    public VentaPageResponse(Page<VentaResponse> ventas, BigDecimal totalGeneral) {
        this.ventas = ventas;
        this.totalGeneral = totalGeneral;
    }

    public Page<VentaResponse> getVentas() {
        return ventas;
    }

    public void setVentas(Page<VentaResponse> ventas) {
        this.ventas = ventas;
    }

    public BigDecimal getTotalGeneral() {
        return totalGeneral;
    }

    public void setTotalGeneral(BigDecimal totalGeneral) {
        this.totalGeneral = totalGeneral;
    }
}
