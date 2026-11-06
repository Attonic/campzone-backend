package br.com.codeacle.campzone.organizacao.service;

import br.com.codeacle.campzone.organizacao.domain.Organizacao;

/**
 * Operações sobre a organização (o tenant). Por enquanto só a criação, usada pelo cadastro (módulo auth). Como é
 * contrato entre services internos e a organização ainda não tem endpoint próprio, devolve a entidade, que nunca
 * chega à API.
 */
public interface OrganizacaoService {

    /** Cria uma organização no plano {@code GRATIS}. Deve rodar dentro da transação de quem a chama. */
    Organizacao criarNoPlanoGratis(String nome);
}
