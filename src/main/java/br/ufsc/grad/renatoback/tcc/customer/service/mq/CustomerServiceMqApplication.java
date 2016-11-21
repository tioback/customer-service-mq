package br.ufsc.grad.renatoback.tcc.customer.service.mq;

import static java.lang.System.getenv;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
public class CustomerServiceMqApplication {

	private Log logger = LogFactory.getLog(CustomerServiceMqApplication.class);

	public static final String EXCHANGE_NAME = "customer-creation.exchange";

	public static void main(String[] args) {
		SpringApplication.run(CustomerServiceMqApplication.class, args);
	}

	@Profile("heroku")
	@Bean(name = "connectionFactory")
	public ConnectionFactory herokuConnectionFactory() {
		final URI ampqUrl;
		try {
			// ampqUrl = new URI(getEnvOrThrow("CLOUDAMQP_URL"));
			ampqUrl = new URI("amqp://flifteha:jRG6EbAeuvSbGLl2aHPk79tYc_fbpxab@buck.rmq.cloudamqp.com/flifteha");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		logger.error("User AMQP config: " + ampqUrl);

		final CachingConnectionFactory factory = new CachingConnectionFactory();
		factory.setUsername(ampqUrl.getUserInfo().split(":")[0]);
		factory.setPassword(ampqUrl.getUserInfo().split(":")[1]);
		factory.setHost(ampqUrl.getHost());
		factory.setPort(ampqUrl.getPort());
		factory.setVirtualHost(ampqUrl.getPath().substring(1));

		return factory;
	}

	private static String getEnvOrThrow(String name) {
		final String env = getenv(name);
		if (env == null) {
			throw new IllegalStateException("Environment variable [" + name + "] is not set.");
		}
		return env;
	}

}
