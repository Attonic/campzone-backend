package br.com.codeacle.campzone.organizacao.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.codeacle.campzone.organizacao.domain.Organizacao;
import br.com.codeacle.campzone.organizacao.repository.OrganizacaoRepository;
import br.com.codeacle.campzone.plano.domain.Plano;
import br.com.codeacle.campzone.plano.repository.PlanoRepository;

class OrganizacaoServiceImplTest {

    private final OrganizacaoRepository organizacaoRepository = mock(OrganizacaoRepository.class);
    private final PlanoRepository planoRepository = mock(PlanoRepository.class);
    private final OrganizacaoServiceImpl service = new OrganizacaoServiceImpl(organizacaoRepository, planoRepository);

    @Test
    void criaAOrganizacaoNoPlanoGratis() {
        Plano gratis = new Plano("GRATIS", "Grátis", 1, 8);
        when(planoRepository.findByCodigo("GRATIS")).thenReturn(Optional.of(gratis));
        when(organizacaoRepository.save(any(Organizacao.class))).thenAnswer(i -> i.getArgument(0));

        Organizacao criada = service.criarNoPlanoGratis("Minha Org");

        assertThat(criada.getNome()).isEqualTo("Minha Org");
        assertThat(criada.getPlano()).isSameAs(gratis);
        verify(organizacaoRepository).save(criada);
    }

    @Test
    void semOPlanoGratisFalhaComMensagemClara() {
        when(planoRepository.findByCodigo("GRATIS")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criarNoPlanoGratis("Org"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GRATIS");
        verifyNoInteractions(organizacaoRepository);
    }
}
