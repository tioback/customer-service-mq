package br.ufsc.grad.renatoback.tcc.customer.service.mq;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

	private Log logger = LogFactory.getLog(CustomerService.class);

	private final RabbitTemplate rabbitTemplate;

	private AtomicInteger counter = new AtomicInteger();

	public CustomerService(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
		this.rabbitTemplate.setExchange(CustomerServiceMqApplication.EXCHANGE_NAME);
	}

	public void createCustomer() {
		_doProcessing();

		signalUserCreation(counter.incrementAndGet());
	}

	private void _doProcessing() {
		// TODO Adicionar algo que consuma tempo de processamento
	}

	private void signalUserCreation(int userId) {
		logger.info("PUB -- create-customer-exchange");
		rabbitTemplate.convertAndSend(String.valueOf(System.nanoTime()));
	}

	public void createForAMinute() {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		long start = System.nanoTime();
		while (System.nanoTime() - start < 60000000000l) {
			executor.execute(() -> {
				createCustomer();
			});
		}
		int sobra = executor.shutdownNow().size();
		logger.info(String.format("Fim do processamento com %d threads não processadas.", sobra));
	}

}
