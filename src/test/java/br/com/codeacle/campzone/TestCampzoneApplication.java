package br.com.codeacle.campzone;

import org.springframework.boot.SpringApplication;

public class TestCampzoneApplication {

	public static void main(String[] args) {
		SpringApplication.from(CampzoneApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
