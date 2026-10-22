package br.com.restaurante.domain;

public enum StatusPedido {
    NOVO, EM_PREPARO, PRONTO, CONCLUIDO, CANCELADO;

    public boolean isAberto() {
        return this != CONCLUIDO && this != CANCELADO;
    }
}
