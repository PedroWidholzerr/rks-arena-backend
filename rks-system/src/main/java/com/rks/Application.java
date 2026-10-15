package com.rks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

// O @EntityScan restringe a busca de entidades: com os package-info dos pacotes de arquitetura,
// a varredura padrão em com.rks quebrava a criação do EntityManagerFactory.
@SpringBootApplication
@EntityScan("com.rks.infra.persistencia.entidade")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
