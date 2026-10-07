package model;

import java.math.BigDecimal;

public record Plano(Integer id, String nome, String descricao, BigDecimal valorMensal, int duracaoMeses) { }
