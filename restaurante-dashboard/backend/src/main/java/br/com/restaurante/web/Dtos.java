package br.com.restaurante.web;

import br.com.restaurante.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record ItemRequest(@NotBlank String nome, @Min(1) int quantidade, @NotNull @DecimalMin("0.01") BigDecimal precoUnitario) {}

    public record PedidoRequest(@NotNull Canal canal, @Min(1) Integer numeroMesa, String cliente,
                                String codigoExterno, FormaPagamento pagamento,
                                @NotEmpty @Valid List<ItemRequest> itens) {}

    public record StatusRequest(@NotNull StatusPedido status) {}

    public record FecharMesaRequest(@NotNull FormaPagamento pagamento) {}

    public record AppUpdateRequest(Boolean ativo, @DecimalMin("0") @DecimalMax("100") BigDecimal comissaoPercentual) {}

    public record ItemDto(String nome, int quantidade, BigDecimal precoUnitario) {}

    public record PedidoDto(Long id, Canal canal, String canalRotulo, Integer numeroMesa, String cliente,
                            String codigoExterno, StatusPedido status, FormaPagamento pagamento,
                            String pagamentoRotulo, BigDecimal total, LocalDateTime criadoEm, List<ItemDto> itens) {
        public static PedidoDto de(Pedido p) {
            return new PedidoDto(p.getId(), p.getCanal(), p.getCanal().getRotulo(), p.getNumeroMesa(),
                    p.getCliente(), p.getCodigoExterno(), p.getStatus(), p.getPagamento(),
                    p.getPagamento() == null ? null : p.getPagamento().getRotulo(), p.getTotal(),
                    p.getCriadoEm(),
                    p.getItens().stream().map(i -> new ItemDto(i.getNome(), i.getQuantidade(), i.getPrecoUnitario())).toList());
        }
    }

    public record MesaDto(int numero, BigDecimal total, List<PedidoDto> pedidos) {}

    public record AppDto(Long id, Canal canal, String rotulo, boolean ativo, BigDecimal comissaoPercentual,
                         long pedidosAbertos) {}
}
