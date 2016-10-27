package br.ufsc.grad.renatoback.tcc.customer.service.mq;

import static br.ufsc.grad.renatoback.tcc.customer.service.mq.CustomerServiceMqApplication.EXCHANGE_NAME;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {

	@Autowired
	ConnectionFactory connectionFactory;

	@Bean
	public AmqpAdmin amqpAdmin() {
		return new RabbitAdmin(connectionFactory);
	}

	@Bean
	public RabbitTemplate rabbitTemplate() {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setExchange(EXCHANGE_NAME);
		return template;
	}

	@Bean
	FanoutExchange exchange(AmqpAdmin amqpAdmin) {
		FanoutExchange exchange = new FanoutExchange(EXCHANGE_NAME, false, false);
		amqpAdmin.declareExchange(exchange);
		return exchange;
	}

}
