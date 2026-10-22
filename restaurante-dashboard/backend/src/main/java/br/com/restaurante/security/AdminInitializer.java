package br.com.restaurante.security;

import br.com.restaurante.domain.Usuario;
import br.com.restaurante.repo.UsuarioRepository;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Cria o primeiro usuário se a tabela estiver vazia. */
@Component
public class AdminInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);
    private static final String ALFABETO = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String user;
    private final String senha;
    private final boolean reset;

    public AdminInitializer(UsuarioRepository usuarios, PasswordEncoder encoder,
                            @Value("${app.admin.user}") String user, @Value("${app.admin.password:}") String senha,
                            @Value("${app.admin.reset:false}") boolean reset) {
        this.reset = reset;
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.user = user;
        this.senha = senha;
    }

    @Override
    public void run(String... args) {
        if (reset && !senha.isBlank()) {
            String nome = user.trim().toLowerCase();
            Usuario u = usuarios.findByUsername(nome).orElse(null);
            if (u == null) {
                usuarios.save(new Usuario(nome, encoder.encode(senha)));
            } else {
                u.setSenhaHash(encoder.encode(senha));
                usuarios.save(u);
            }
            log.warn("Senha do usuário '{}' redefinida por ADMIN_RESET. Remova essa variável.", nome);
            return;
        }
        if (usuarios.count() > 0) return;
        String s = senha;
        if (s.isBlank()) {
            SecureRandom r = new SecureRandom();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) sb.append(ALFABETO.charAt(r.nextInt(ALFABETO.length())));
            s = sb.toString();
            log.warn("ADMIN_PASSWORD não definido. Usuário '{}' criado com senha: {}  (troque depois do login)", user, s);
        }
        usuarios.save(new Usuario(user.toLowerCase(), encoder.encode(s)));
    }
}
