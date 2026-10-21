package br.com.codeacle.campzone.organizacao.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.organizacao.repository.OrganizacaoRepository;
import br.com.codeacle.campzone.organizacao.service.OrganizacaoService;
import br.com.codeacle.campzone.plano.repository.PlanoRepository;
import br.com.codeacle.campzone.plano.seed.PlanoSeeder;

@Service
public class OrganizacaoServiceImpl implements OrganizacaoService {

    private final OrganizacaoRepository organizacaoRepository;
    private final PlanoRepository planoRepository;

    public OrganizacaoServiceImpl(OrganizacaoRepository organizacaoRepository, PlanoRepository planoRepository) {
        this.organizacaoRepository = organizacaoRepository;
        this.planoRepository = planoRepository;
    }

    @Override
    @Transactional
    public Organizacao criarNoPlanoGratis(String nome) {
        var plano = planoRepository.findByCodigo(PlanoSeeder.CODIGO_GRATIS)
                .orElseThrow(() -> new IllegalStateException(
                        "Plano " + PlanoSeeder.CODIGO_GRATIS + " não encontrado: o PlanoSeeder não foi executado."));
        return organizacaoRepository.save(new Organizacao(nome, plano));
    }
}
