package br.com.restaurante.domain;

public enum Canal {
    IFOOD("iFood", true, false),
    NOVE_NOVE("99Food", true, false),
    RAPPI("Rappi", true, true),
    UBER_EATS("Uber Eats", true, true),
    PROPRIO("Delivery próprio", true, true),
    MESA("Mesa", false, true);

    private final String rotulo;
    private final boolean delivery;
    /** false = integração ainda não liberada (iFood e 99Food aguardam acesso). */
    private final boolean disponivel;

    Canal(String rotulo, boolean delivery, boolean disponivel) {
        this.rotulo = rotulo;
        this.delivery = delivery;
        this.disponivel = disponivel;
    }

    public boolean isDisponivel() { return disponivel; }

    public String getRotulo() { return rotulo; }
    public boolean isDelivery() { return delivery; }
}
