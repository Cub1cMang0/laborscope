package com.laborscope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class LaborScopeController {
	@Autowired
	private LaborScopeApplication laborscopeservice;
	public static void main(String[] args) {
		Dotenv.configure().directory("..").ignoreIfMissing().systemProperties().load();
		SpringApplication.run(LaborScopeController.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onStartup() {
        laborscopeservice.startCrawl("https://en.wikipedia.org");
	}
}
