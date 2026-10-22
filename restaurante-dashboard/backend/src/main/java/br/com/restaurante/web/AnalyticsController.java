package br.com.restaurante.web;

import br.com.restaurante.service.AnalyticsService;
import br.com.restaurante.service.AnalyticsService.Periodo;
import br.com.restaurante.service.AnalyticsService.Resumo;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analises")
public class AnalyticsController {
    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping
    public Resumo resumo(@RequestParam(defaultValue = "DIARIO") Periodo periodo,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.resumo(periodo, data == null ? LocalDate.now() : data);
    }
}
