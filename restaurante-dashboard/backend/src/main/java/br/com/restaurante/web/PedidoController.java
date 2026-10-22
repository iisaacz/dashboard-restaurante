package br.com.restaurante.web;

import br.com.restaurante.domain.Canal;
import br.com.restaurante.service.PedidoService;
import br.com.restaurante.web.Dtos.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PedidoController {
    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    /** abertos=true ignora a data e traz tudo que ainda está em andamento. */
    @GetMapping("/pedidos")
    public List<PedidoDto> listar(@RequestParam(defaultValue = "false") boolean abertos,
                                  @RequestParam(required = false) Canal canal,
                                  @RequestParam(required = false) Boolean delivery,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.listar(abertos, canal, delivery, data == null ? LocalDate.now() : data);
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoDto criar(@Valid @RequestBody PedidoRequest req) {
        return service.criar(req);
    }

    @PatchMapping("/pedidos/{id}/status")
    public PedidoDto status(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        return service.atualizarStatus(id, req.status());
    }

    @GetMapping("/mesas")
    public List<MesaDto> mesas() {
        return service.mesasAbertas();
    }

    @PostMapping("/mesas/{numero}/fechar")
    public List<PedidoDto> fechar(@PathVariable int numero, @Valid @RequestBody FecharMesaRequest req) {
        return service.fecharMesa(numero, req.pagamento());
    }

    @GetMapping("/apps")
    public List<AppDto> apps() {
        return service.listarApps();
    }

    @PatchMapping("/apps/{id}")
    public AppDto atualizarApp(@PathVariable Long id, @Valid @RequestBody AppUpdateRequest req) {
        return service.atualizarApp(id, req);
    }
}
