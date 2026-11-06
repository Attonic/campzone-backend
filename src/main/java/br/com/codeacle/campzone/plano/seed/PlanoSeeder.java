package br.com.codeacle.campzone.plano.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import br.com.codeacle.campzone.plano.domain.Plano;
import br.com.codeacle.campzone.plano.repository.PlanoRepository;

/**
 * Garante que o plano {@code GRATIS} existe. Idempotente: só cria se não existir e nunca sobrescreve valores
 * alterados no banco. Roda em todos os perfis. Os limites do plano grátis aparecem somente aqui.
 */
@Component
public class PlanoSeeder implements ApplicationRunner {

    public static final String CODIGO_GRATIS = "GRATIS";

    private static final Logger log = LoggerFactory.getLogger(PlanoSeeder.class);

    private final PlanoRepository planoRepository;

    public PlanoSeeder(PlanoRepository planoRepository) {
        this.planoRepository = planoRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (planoRepository.findByCodigo(CODIGO_GRATIS).isPresent()) {
            return;
        }
        try {
            planoRepository.saveAndFlush(new Plano(CODIGO_GRATIS, "Grátis", 1, 8));
            log.info("Plano {} criado.", CODIGO_GRATIS);
        } catch (DataIntegrityViolationException e) {
            // Outra instância criou o plano ao mesmo tempo (unique em codigo): o resultado desejado já existe.
            log.debug("Plano {} já criado por outra instância.", CODIGO_GRATIS);
        }
    }
}
