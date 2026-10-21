package br.com.codeacle.campzone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

class TestCampzoneApplicationTest {

	@Test
	void semPerfilInformadoAssumeDev() {
		assumeTrue(System.getenv("SPRING_PROFILES_ACTIVE") == null && System.getProperty("spring.profiles.active") == null);

		assertThat(TestCampzoneApplication.comPerfilDev(new String[] { "--server.port=1" }))
				.containsExactly("--server.port=1", "--spring.profiles.active=dev");
	}

	@Test
	void perfilInformadoPorArgumentoNaoEhAlterado() {
		String[] args = { "--spring.profiles.active=prod" };

		assertThat(TestCampzoneApplication.comPerfilDev(args)).containsExactly("--spring.profiles.active=prod");
	}
}
