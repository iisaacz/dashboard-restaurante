package br.com.restaurante.domain;

public enum FormaPagamento {
    PIX("Pix"),
    CREDITO("Crédito"),
    DEBITO("Débito"),
    DINHEIRO("Dinheiro"),
    VALE_REFEICAO("Vale-refeição"),
    ONLINE_APP("Pago no app");

    private final String rotulo;

    FormaPagamento(String rotulo) { this.rotulo = rotulo; }

    public String getRotulo() { return rotulo; }
}
