package com.biblioteca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.biblioteca.config.AppCookieProperties;
import com.biblioteca.config.AiServiceProperties;
import com.biblioteca.config.TelegramProperties;
import com.biblioteca.config.LibraryPolicyProperties;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableAsync
@EnableConfigurationProperties({ AppCookieProperties.class, AiServiceProperties.class, TelegramProperties.class,
		LibraryPolicyProperties.class })
public class BibliotecaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BibliotecaBackendApplication.class, args);
	}

}
