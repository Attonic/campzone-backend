package br.com.codeacle.campzone;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("dev")
class CampzoneApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void controllersDeTesteForaDoScanNaoEstaoNoContexto(ApplicationContext contexto) throws Exception {
		Class<?> controllerDeTeste = Class.forName("br.com.codeacle.testes.ApiTesteController");

		assertThat(contexto.getBeanNamesForType(controllerDeTeste)).isEmpty();
	}

}
