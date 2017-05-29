package svenz;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.sleuth.Sampler;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanReporter;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@RestController
public class Main {

	private static Logger log = LoggerFactory.getLogger(Main.class);
	
	@Autowired
	private ApplicationContext context;
	
	@RequestMapping("/")
	public String home() {
		Span span = context.getBean(Tracer.class).getCurrentSpan();
		log.info("Handling home {}", span.tags());
		return "Hello World";
	}
	
	@Bean
	public LogSpanReporter getReporter() {
		return new LogSpanReporter();
	}

	@Bean
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}

	@Bean
	public Sampler getSampler() {
		return new AlwaysSampler();
	}
	
	public static void main(String[] args) throws URISyntaxException {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(Main.class, args);
		RestTemplate template = applicationContext.getBean(RestTemplate.class);
	
		HttpEntity<?> requestEntity = RequestEntity.get(new URI("http://localhost:8080/")).accept(MediaType.TEXT_PLAIN).header("User-Agent", "Test").build();
		ResponseEntity<String> entity = template.exchange("http://localhost:8080/", HttpMethod.GET, requestEntity, String.class);
		String rsp = entity.getBody();
		log.info("Response: {}", rsp);
	}
	
	private static class LogSpanReporter implements SpanReporter {

		@Override
		public void report(Span span) {
			ByteBuffer bb = ByteBuffer.allocate(16);
			bb.asLongBuffer().put(span.getTraceIdHigh()).put(span.getTraceId());
			UUID uuid = UUID.nameUUIDFromBytes(bb.array());
			log.info("Span: {} {} {}", uuid, span, span.tags());
		}
		
	}
}
