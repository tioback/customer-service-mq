package br.ufsc.grad.renatoback.tcc.customer.service.mq;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CustomerServiceMqApplication {

	public static final String EXCHANGE_NAME = "customer-creation.exchange";

	public static void main(String[] args) {
		SpringApplication.run(CustomerServiceMqApplication.class, args);
	}

	@Autowired
	AmqpAdmin admin;

	@Bean
	FanoutExchange exchange() {
		FanoutExchange exchange = new FanoutExchange(EXCHANGE_NAME, false, false);
		admin.declareExchange(exchange);
		return exchange;
	}

}
