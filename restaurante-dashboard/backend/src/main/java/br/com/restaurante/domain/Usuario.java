package br.com.restaurante.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(nullable = false, length = 100)
    private String senhaHash;

    public Usuario() {}

    public Usuario(String username, String senhaHash) {
        this.username = username;
        this.senhaHash = senhaHash;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
}
