package br.com.restaurante.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Token assinado (HMAC-SHA256): base64url(usuario|expiraEpoch) . base64url(assinatura). */
@Service
public class TokenService {
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

    private final byte[] chave;
    private final long validadeSegundos;

    public TokenService(@Value("${app.jwt.secret:}") String segredo, @Value("${app.token-horas:12}") long horas) {
        if (segredo.isBlank()) {
            chave = new byte[32];
            new SecureRandom().nextBytes(chave);
            log.warn("JWT_SECRET não definido: chave aleatória; sessões caem a cada reinício.");
        } else {
            if (segredo.length() < 32) {
                throw new IllegalStateException("JWT_SECRET deve ter ao menos 32 caracteres.");
            }
            chave = segredo.getBytes(StandardCharsets.UTF_8);
        }
        validadeSegundos = horas * 3600;
    }

    public Instant expiracao() {
        return Instant.now().plusSeconds(validadeSegundos);
    }

    public String gerar(String usuario, Instant expira) {
        String corpo = B64.encodeToString((usuario + "|" + expira.getEpochSecond()).getBytes(StandardCharsets.UTF_8));
        return corpo + "." + B64.encodeToString(assinar(corpo));
    }

    /** Retorna o usuário se o token for válido e não expirado. */
    public Optional<String> validar(String token) {
        try {
            int ponto = token.indexOf('.');
            if (ponto < 1) return Optional.empty();
            String corpo = token.substring(0, ponto);
            byte[] sig = Base64.getUrlDecoder().decode(token.substring(ponto + 1));
            if (!MessageDigest.isEqual(sig, assinar(corpo))) return Optional.empty();
            String[] partes = new String(Base64.getUrlDecoder().decode(corpo), StandardCharsets.UTF_8).split("\\|");
            if (partes.length != 2 || Instant.now().getEpochSecond() > Long.parseLong(partes[1])) return Optional.empty();
            return Optional.of(partes[0]);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private byte[] assinar(String corpo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chave, "HmacSHA256"));
            return mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
