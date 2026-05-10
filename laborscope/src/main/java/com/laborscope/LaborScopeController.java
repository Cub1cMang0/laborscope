package com.laborscope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;

import com.laborscope.kafka.CrawlJobProducer;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class LaborScopeController {
	@Autowired
	private LaborScopeApplication laborscopeservice;
	@Autowired
	private CrawlJobProducer crawlJobProducer;
	public static void main(String[] args) {
		Dotenv.configure().directory("..").ignoreIfMissing().systemProperties().load();
		SpringApplication.run(LaborScopeController.class, args);
	}

	// Inside LaborScopeController
	@GetMapping("/export")
	public String triggerExport() {
		try
		{
			laborscopeservice.exportDataToCsv("wikidata.csv");
			return "Success!";
		}
		catch (Exception e)
		{
			return "Export failed: " + e.getMessage();
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onStartup() {
		crawlJobProducer.publish("https://en.wikipedia.org", 1);	}
}
