package br.com.codeacle.campzone;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;

/**
 * Sobe a aplicação com um PostgreSQL descartável (Testcontainers). Como o banco é descartável e não há risco de
 * tocar em dados reais, assume o perfil {@code dev} quando nenhum perfil foi informado (a aplicação principal não
 * tem perfil padrão, ADR 0004).
 */
public class TestCampzoneApplication {

	public static void main(String[] args) {
		SpringApplication.from(CampzoneApplication::main).with(TestcontainersConfiguration.class)
				.run(comPerfilDev(args));
	}

	static String[] comPerfilDev(String[] args) {
		boolean perfilInformado = System.getProperty("spring.profiles.active") != null
				|| System.getenv("SPRING_PROFILES_ACTIVE") != null
				|| Arrays.stream(args).anyMatch(arg -> arg.startsWith("--spring.profiles.active"));
		if (perfilInformado) {
			return args;
		}
		String[] comPerfil = Arrays.copyOf(args, args.length + 1);
		comPerfil[args.length] = "--spring.profiles.active=dev";
		return comPerfil;
	}

}
