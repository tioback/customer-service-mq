package br.ufsc.grad.renatoback.tcc.customer.service.mq;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

	private Log logger = LogFactory.getLog(CustomerService.class);

	private final RabbitTemplate rabbitTemplate;

	private final AmqpAdmin amqpAdmin;
	private final FanoutExchange exchange;

	private AtomicInteger counter = new AtomicInteger();

	public CustomerService(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin, FanoutExchange exchange) {
		this.rabbitTemplate = rabbitTemplate;
		this.amqpAdmin = amqpAdmin;
		this.exchange = exchange;

		setUpConfirmation();
	}

	public void createCustomer(CorrelationData correlationData) {
		_doProcessing();

		signalUserCreation(counter.incrementAndGet(), correlationData);
	}

	private void _doProcessing() {
		// TODO Adicionar algo que consuma tempo de processamento
	}

	private void signalUserCreation(int userId, CorrelationData correlationData) {
		rabbitTemplate.correlationConvertAndSend(String.valueOf(userId), correlationData);
	}

	AtomicInteger recCounter = new AtomicInteger();
	AtomicInteger refCounter = new AtomicInteger();
	AtomicLong average = new AtomicLong();

	private CorrelationData currentCorrelationData;
	private long currentStart;

	public void createCustomer(int repetitions, int interval_seg, int threads, int sleep) {
		// BEGIN CONFIG
		final long interval_nano = TimeUnit.SECONDS.toNanos(interval_seg);
		// END CONFIG

		ExecutorService executor;
		for (int i = 0; i < repetitions; i++) {
			amqpAdmin.declareExchange(exchange);
			int activeThreadsCount = Thread.activeCount();
			executor = Executors.newFixedThreadPool(threads);

			currentStart = System.nanoTime();
			currentCorrelationData = new CorrelationData(String.format("%d-%d-%d", i, threads, sleep));

			for (int j = 0; j < threads; j++) {
				executor.execute(new Task(this, currentStart, interval_nano, sleep,
						new CorrelationData(String.format("%d-%d-%d", i, threads, sleep))));
			}
			int sobra = stopExecutor(interval_seg, executor);

			logger.info(String.format("FIM [Iteração][Ativas][Interrompidas] - [%d][%d][%d]", i, activeThreadsCount,
					sobra));

			printStatistics(threads, sleep, recCounter.getAndSet(0), refCounter.getAndSet(0), average.getAndSet(0));

			amqpAdmin.deleteExchange(exchange.getName());
		}
	}

	private void setUpConfirmation() {
		rabbitTemplate.setConfirmCallback((receivedCorrelationData, ack, cause) -> {
			if (currentCorrelationData.getId().equals(receivedCorrelationData.getId())) {
				average.accumulateAndGet(System.nanoTime() - currentStart,
						(n, m) -> (n + m) / (n == 0 || m == 0 ? 1 : 2));
				recCounter.incrementAndGet();
			} else {
				refCounter.incrementAndGet();
			}
		});
	}

	private int stopExecutor(int interval_seg, ExecutorService executor) {
		executor.shutdown();
		int sobra = 0;
		try {
			if (!executor.awaitTermination(interval_seg, TimeUnit.SECONDS)) {
				sobra = executor.shutdownNow().size();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return sobra;
	}

	public void printStatistics(int threads, int sleep, int recCounter, int refCounter, long average) {
		logger.info(String.format("[STAT]-[Thread][Pausa][Recebidos][Rejeitados][Latência]:\t%d\t%d\t%d\t%d\t%d",
				threads, sleep, recCounter, refCounter, average));
	}

	public void createCustomer(int repetitions, int interval, int threads, int start, int increment, int end) {
		for (int i = start; i <= end; i = i + increment) {
			createCustomer(repetitions, interval, threads, i);
		}
	}

}
