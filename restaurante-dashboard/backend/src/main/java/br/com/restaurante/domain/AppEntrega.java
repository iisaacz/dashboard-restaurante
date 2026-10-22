package br.com.restaurante.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "app_entrega")
public class AppEntrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private Canal canal;

    /** Loja aberta (recebendo pedidos) neste app. */
    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal comissaoPercentual = BigDecimal.ZERO;

    public AppEntrega() {}

    public AppEntrega(Canal canal, BigDecimal comissaoPercentual) {
        this.canal = canal;
        this.comissaoPercentual = comissaoPercentual;
    }

    public Long getId() { return id; }
    public Canal getCanal() { return canal; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public BigDecimal getComissaoPercentual() { return comissaoPercentual; }
    public void setComissaoPercentual(BigDecimal c) { this.comissaoPercentual = c; }
}
