package br.com.restaurante.config;

import br.com.restaurante.domain.*;
import br.com.restaurante.repo.AppEntregaRepository;
import br.com.restaurante.repo.PedidoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSeeder implements CommandLineRunner {
    private record Prato(String nome, String preco) {}

    private static final List<Prato> CARDAPIO = List.of(
            new Prato("Feijoada completa", "42.90"), new Prato("Parmegiana de frango", "38.50"),
            new Prato("Picanha na chapa", "69.90"), new Prato("Risoto de camarão", "58.00"),
            new Prato("X-Burger artesanal", "29.90"), new Prato("Salada Caesar", "26.00"),
            new Prato("Lasanha à bolonhesa", "36.00"), new Prato("Refrigerante lata", "7.00"),
            new Prato("Suco natural", "11.00"), new Prato("Pudim", "14.00"));

    private final PedidoRepository pedidos;
    private final AppEntregaRepository apps;
    private final boolean seed;

    public DataSeeder(PedidoRepository pedidos, AppEntregaRepository apps, @Value("${app.seed}") boolean seed) {
        this.pedidos = pedidos;
        this.apps = apps;
        this.seed = seed;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (apps.count() == 0) {
            apps.save(new AppEntrega(Canal.RAPPI, new BigDecimal("15.00")));
            apps.save(new AppEntrega(Canal.UBER_EATS, new BigDecimal("13.00")));
            apps.save(new AppEntrega(Canal.PROPRIO, BigDecimal.ZERO));
        }
        if (!seed || pedidos.count() > 0) return;

        Random r = new Random(42);
        Canal[] canais = {Canal.RAPPI, Canal.RAPPI, Canal.UBER_EATS, Canal.UBER_EATS, Canal.PROPRIO,
                Canal.MESA, Canal.MESA, Canal.MESA, Canal.MESA, Canal.MESA};
        FormaPagamento[] presenciais = {FormaPagamento.PIX, FormaPagamento.PIX, FormaPagamento.CREDITO,
                FormaPagamento.CREDITO, FormaPagamento.DEBITO, FormaPagamento.DINHEIRO, FormaPagamento.VALE_REFEICAO};
        LocalDateTime agora = LocalDateTime.now();

        for (int dia = 30; dia >= 0; dia--) {
            int qtd = 25 + r.nextInt(25);
            for (int n = 0; n < qtd; n++) {
                Pedido p = novoPedido(r, canais[r.nextInt(canais.length)], presenciais);
                int hora = r.nextBoolean() ? 11 + r.nextInt(4) : 18 + r.nextInt(5);
                LocalDateTime quando = agora.toLocalDate().minusDays(dia).atTime(hora, r.nextInt(60));
                if (quando.isAfter(agora)) continue;
                p.setCriadoEm(quando);
                p.setStatus(r.nextInt(25) == 0 ? StatusPedido.CANCELADO : StatusPedido.CONCLUIDO);
                pedidos.save(p);
            }
        }
        // pedidos em andamento para demonstrar a operação
        for (int n = 0; n < 6; n++) {
            Pedido p = novoPedido(r, canais[r.nextInt(canais.length)], presenciais);
            p.setStatus(StatusPedido.values()[r.nextInt(3)]);
            if (p.getCanal() == Canal.MESA) p.setPagamento(null);
            p.setCriadoEm(agora.minusMinutes(5 + r.nextInt(40)));
            pedidos.save(p);
        }
    }

    private Pedido novoPedido(Random r, Canal canal, FormaPagamento[] presenciais) {
        Pedido p = new Pedido();
        p.setCanal(canal);
        if (canal == Canal.MESA) {
            p.setNumeroMesa(1 + r.nextInt(12));
            p.setPagamento(presenciais[r.nextInt(presenciais.length)]);
        } else {
            p.setCliente("Cliente " + (100 + r.nextInt(900)));
            p.setCodigoExterno(canal.name().substring(0, 2) + (10000 + r.nextInt(90000)));
            p.setPagamento(canal == Canal.PROPRIO ? presenciais[r.nextInt(presenciais.length)] : FormaPagamento.ONLINE_APP);
        }
        int itens = 1 + r.nextInt(3);
        for (int i = 0; i < itens; i++) {
            Prato prato = CARDAPIO.get((int) Math.min(CARDAPIO.size() - 1, Math.abs(r.nextGaussian()) * 3.5));
            p.adicionarItem(new ItemPedido(prato.nome(), 1 + r.nextInt(2), new BigDecimal(prato.preco())));
        }
        return p;
    }
}
