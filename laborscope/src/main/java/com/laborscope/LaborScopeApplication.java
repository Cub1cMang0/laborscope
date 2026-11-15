package com.laborscope;
 
import java.io.File;
 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
 
import io.github.cdimascio.dotenv.Dotenv;


@SpringBootApplication
public class LaborScopeApplication {
	@Value("${server.port:8080}")
	private int port;
	
	public static void main(String[] args) {
		String envDirectory = new File(".env").exists() ? "." : "..";
		Dotenv.configure().directory(envDirectory).ignoreIfMissing().systemProperties().load();
		SpringApplication.run(LaborScopeApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		System.out.println();
		System.out.println("WikiCrawl is ready. Begin a crawl session with:");
		System.out.println("  curl -X POST \"localhost:" + port + "/crawl?url=https://en.wikipedia.org/wiki/Apache_Kafka\"");
		System.out.println("(omit ?url=... to use the default seed). Press Ctrl+C to stop.");
		System.out.println();
	}

}
