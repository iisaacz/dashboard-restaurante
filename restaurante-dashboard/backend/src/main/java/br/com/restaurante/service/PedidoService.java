package br.com.restaurante.service;

import br.com.restaurante.domain.*;
import br.com.restaurante.repo.AppEntregaRepository;
import br.com.restaurante.repo.PedidoRepository;
import br.com.restaurante.web.Dtos.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PedidoService {
    private static final List<StatusPedido> ABERTOS =
            Arrays.stream(StatusPedido.values()).filter(StatusPedido::isAberto).toList();

    private final PedidoRepository pedidos;
    private final AppEntregaRepository apps;

    public PedidoService(PedidoRepository pedidos, AppEntregaRepository apps) {
        this.pedidos = pedidos;
        this.apps = apps;
    }

    @Transactional(readOnly = true)
    public List<PedidoDto> listar(boolean apenasAbertos, Canal canal, Boolean delivery, LocalDate data) {
        List<Pedido> base = apenasAbertos
                ? pedidos.findByStatusInOrderByCriadoEmDesc(ABERTOS)
                : pedidos.findByCriadoEmGreaterThanEqualAndCriadoEmLessThanOrderByCriadoEmDesc(
                        data.atStartOfDay(), data.plusDays(1).atStartOfDay());
        return base.stream()
                .filter(p -> canal == null || p.getCanal() == canal)
                .filter(p -> delivery == null || p.getCanal().isDelivery() == delivery)
                .map(PedidoDto::de)
                .toList();
    }

    public PedidoDto criar(PedidoRequest req) {
        if (!req.canal().isDisponivel()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, req.canal().getRotulo() + ": integração indisponível.");
        }
        if (req.canal() == Canal.MESA && req.numeroMesa() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o número da mesa.");
        }
        if (req.canal().isDelivery()) {
            apps.findByCanal(req.canal()).filter(a -> !a.isAtivo()).ifPresent(a -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, req.canal().getRotulo() + " está pausado.");
            });
        }
        Pedido p = new Pedido();
        p.setCanal(req.canal());
        p.setNumeroMesa(req.canal() == Canal.MESA ? req.numeroMesa() : null);
        p.setCliente(req.cliente());
        p.setCodigoExterno(req.codigoExterno());
        p.setPagamento(req.pagamento() != null ? req.pagamento()
                : (req.canal().isDelivery() && req.canal() != Canal.PROPRIO ? FormaPagamento.ONLINE_APP : null));
        req.itens().forEach(i -> p.adicionarItem(new ItemPedido(i.nome(), i.quantidade(), i.precoUnitario())));
        return PedidoDto.de(pedidos.save(p));
    }

    public PedidoDto atualizarStatus(Long id, StatusPedido status) {
        Pedido p = buscar(id);
        if (!p.getStatus().isAberto()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido já finalizado.");
        }
        if (status == StatusPedido.CONCLUIDO && p.getPagamento() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Defina a forma de pagamento antes de concluir.");
        }
        p.setStatus(status);
        return PedidoDto.de(p);
    }

    @Transactional(readOnly = true)
    public List<MesaDto> mesasAbertas() {
        Map<Integer, List<Pedido>> porMesa = pedidos.findByStatusInOrderByCriadoEmDesc(ABERTOS).stream()
                .filter(p -> p.getCanal() == Canal.MESA)
                .collect(Collectors.groupingBy(Pedido::getNumeroMesa, TreeMap::new, Collectors.toList()));
        return porMesa.entrySet().stream()
                .map(e -> new MesaDto(e.getKey(),
                        e.getValue().stream().map(Pedido::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                        e.getValue().stream().map(PedidoDto::de).toList()))
                .toList();
    }

    public List<PedidoDto> fecharMesa(int numero, FormaPagamento pagamento) {
        List<Pedido> abertos = pedidos.findByCanalAndNumeroMesaAndStatusIn(Canal.MESA, numero, ABERTOS);
        if (abertos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mesa sem pedidos abertos.");
        }
        abertos.forEach(p -> {
            p.setPagamento(pagamento);
            p.setStatus(StatusPedido.CONCLUIDO);
        });
        return abertos.stream().map(PedidoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<AppDto> listarApps() {
        return apps.findAll().stream()
                .filter(a -> a.getCanal().isDelivery() && a.getCanal().isDisponivel())
                .sorted(Comparator.comparing(a -> a.getCanal().ordinal()))
                .map(a -> new AppDto(a.getId(), a.getCanal(), a.getCanal().getRotulo(), a.isAtivo(),
                        a.getComissaoPercentual(), pedidos.countByCanalAndStatusIn(a.getCanal(), ABERTOS)))
                .toList();
    }

    public AppDto atualizarApp(Long id, AppUpdateRequest req) {
        AppEntrega a = apps.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App não encontrado."));
        if (req.ativo() != null) a.setAtivo(req.ativo());
        if (req.comissaoPercentual() != null) a.setComissaoPercentual(req.comissaoPercentual());
        return new AppDto(a.getId(), a.getCanal(), a.getCanal().getRotulo(), a.isAtivo(),
                a.getComissaoPercentual(), pedidos.countByCanalAndStatusIn(a.getCanal(), ABERTOS));
    }

    private Pedido buscar(Long id) {
        return pedidos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
    }
}
