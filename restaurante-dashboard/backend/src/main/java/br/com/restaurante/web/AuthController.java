package br.com.restaurante.web;

import br.com.restaurante.domain.Usuario;
import br.com.restaurante.repo.UsuarioRepository;
import br.com.restaurante.security.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record LoginRequest(@NotBlank String usuario, @NotBlank String senha) {}
    public record LoginResponse(String token, Instant expiraEm, String usuario) {}
    public record TrocarSenhaRequest(@NotBlank String atual, @NotBlank @Size(min = 8, max = 72) String nova) {}

    private static final int MAX_FALHAS = 5;
    private static final long BLOQUEIO_MS = 60_000;

    private record Falhas(int total, long bloqueadoAte) {}

    private final Map<String, Falhas> falhas = new ConcurrentHashMap<>();
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final String hashFalso;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder, TokenService tokens) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.tokens = tokens;
        this.hashFalso = encoder.encode("nao-existe");
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        String nome = req.usuario().trim().toLowerCase();
        Falhas f = falhas.get(nome);
        if (f != null && f.bloqueadoAte() > System.currentTimeMillis()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Aguarde 1 minuto.");
        }
        Usuario u = usuarios.findByUsername(nome).orElse(null);
        boolean ok = encoder.matches(req.senha(), u != null ? u.getSenhaHash() : hashFalso) && u != null;
        if (!ok) {
            falhas.merge(nome, new Falhas(1, 0), (a, b) -> {
                int n = (a.bloqueadoAte() > 0 ? 0 : a.total()) + 1;
                return new Falhas(n, n >= MAX_FALHAS ? System.currentTimeMillis() + BLOQUEIO_MS : 0);
            });
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos.");
        }
        falhas.remove(nome);
        Instant expira = tokens.expiracao();
        return new LoginResponse(tokens.gerar(u.getUsername(), expira), expira, u.getUsername());
    }

    @PostMapping("/senha")
    @Transactional
    public Map<String, String> trocarSenha(Authentication auth, @Valid @RequestBody TrocarSenhaRequest req) {
        Usuario u = usuarios.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado."));
        if (!encoder.matches(req.atual(), u.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta.");
        }
        u.setSenhaHash(encoder.encode(req.nova()));
        return Map.of("ok", "Senha alterada.");
    }
}
