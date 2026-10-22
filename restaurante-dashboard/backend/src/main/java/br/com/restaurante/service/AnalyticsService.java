package br.com.restaurante.service;

import br.com.restaurante.domain.*;
import br.com.restaurante.repo.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    public enum Periodo { DIARIO, MENSAL }

    public record Ponto(String rotulo, BigDecimal valor, long pedidos) {}
    public record Contagem(String chave, String rotulo, long pedidos, BigDecimal valor) {}
    public record ItemRank(String nome, long quantidade, BigDecimal valor) {}
    public record Resumo(Periodo periodo, LocalDateTime de, LocalDateTime ate, BigDecimal faturamento,
                         long totalPedidos, BigDecimal ticketMedio, long cancelados, List<Ponto> serie,
                         List<Contagem> pagamentos, List<Contagem> plataformas, List<ItemRank> topItens) {}

    private final PedidoRepository repo;

    public AnalyticsService(PedidoRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Resumo resumo(Periodo periodo, LocalDate data) {
        LocalDateTime de;
        LocalDateTime ate;
        if (periodo == Periodo.DIARIO) {
            de = data.atStartOfDay();
            ate = de.plusDays(1);
        } else {
            de = data.withDayOfMonth(1).atStartOfDay();
            ate = de.plusMonths(1);
        }
        List<Pedido> todos = repo.findByCriadoEmGreaterThanEqualAndCriadoEmLessThanOrderByCriadoEmDesc(de, ate);
        long cancelados = todos.stream().filter(p -> p.getStatus() == StatusPedido.CANCELADO).count();
        List<Pedido> validos = todos.stream().filter(p -> p.getStatus() != StatusPedido.CANCELADO).toList();

        BigDecimal faturamento = soma(validos);
        BigDecimal ticket = validos.isEmpty() ? BigDecimal.ZERO
                : faturamento.divide(BigDecimal.valueOf(validos.size()), 2, RoundingMode.HALF_UP);

        return new Resumo(periodo, de, ate, faturamento, validos.size(), ticket, cancelados,
                serie(periodo, de, validos),
                agrupar(validos, p -> p.getPagamento() == null ? "SEM_PAGAMENTO" : p.getPagamento().name(),
                        p -> p.getPagamento() == null ? "Em aberto" : p.getPagamento().getRotulo()),
                agrupar(validos, p -> p.getCanal().name(), p -> p.getCanal().getRotulo()),
                topItens(validos));
    }

    private List<Ponto> serie(Periodo periodo, LocalDateTime de, List<Pedido> validos) {
        int buckets = periodo == Periodo.DIARIO ? 24 : de.toLocalDate().lengthOfMonth();
        Function<Pedido, Integer> idx = p -> periodo == Periodo.DIARIO
                ? p.getCriadoEm().getHour() : p.getCriadoEm().getDayOfMonth() - 1;
        BigDecimal[] valores = new BigDecimal[buckets];
        long[] qtd = new long[buckets];
        Arrays.fill(valores, BigDecimal.ZERO);
        for (Pedido p : validos) {
            int i = idx.apply(p);
            valores[i] = valores[i].add(p.getTotal());
            qtd[i]++;
        }
        List<Ponto> out = new ArrayList<>();
        for (int i = 0; i < buckets; i++) {
            String rotulo = periodo == Periodo.DIARIO ? String.format("%02dh", i) : String.valueOf(i + 1);
            out.add(new Ponto(rotulo, valores[i], qtd[i]));
        }
        return out;
    }

    private List<Contagem> agrupar(List<Pedido> pedidos, Function<Pedido, String> chave, Function<Pedido, String> rotulo) {
        Map<String, List<Pedido>> grupos = new LinkedHashMap<>();
        pedidos.forEach(p -> grupos.computeIfAbsent(chave.apply(p), k -> new ArrayList<>()).add(p));
        return grupos.entrySet().stream()
                .map(e -> new Contagem(e.getKey(), rotulo.apply(e.getValue().get(0)), e.getValue().size(), soma(e.getValue())))
                .sorted(Comparator.comparingLong(Contagem::pedidos).reversed())
                .toList();
    }

    private List<ItemRank> topItens(List<Pedido> pedidos) {
        Map<String, long[]> qtd = new HashMap<>();
        Map<String, BigDecimal> valor = new HashMap<>();
        for (Pedido p : pedidos) {
            for (ItemPedido i : p.getItens()) {
                qtd.computeIfAbsent(i.getNome(), k -> new long[1])[0] += i.getQuantidade();
                valor.merge(i.getNome(), i.subtotal(), BigDecimal::add);
            }
        }
        return qtd.entrySet().stream()
                .map(e -> new ItemRank(e.getKey(), e.getValue()[0], valor.get(e.getKey())))
                .sorted(Comparator.comparingLong(ItemRank::quantidade).reversed())
                .limit(10)
                .toList();
    }

    private static BigDecimal soma(List<Pedido> pedidos) {
        return pedidos.stream().map(Pedido::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
