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

	private AtomicInteger recCounter = new AtomicInteger();
	private AtomicInteger refCounter = new AtomicInteger();
	private AtomicLong average = new AtomicLong();

	private String currentCorrelationDataKey;
	private long currentStart;

	public CustomerService(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin, FanoutExchange exchange) {
		this.rabbitTemplate = rabbitTemplate;
		this.amqpAdmin = amqpAdmin;
		this.exchange = exchange;

		setUpConfirmation();
	}

	public void createCustomer() {
		resetExchange();
		_createCustomer(new CorrelationData("1-0-0-0"));
	}

	public void _createCustomer(CorrelationData correlationData) {
		_doProcessing();

		signalUserCreation(counter.incrementAndGet(), correlationData);
	}

	private void _doProcessing() {
		// TODO Adicionar algo que consuma tempo de processamento
	}

	private void signalUserCreation(int userId, CorrelationData correlationData) {
		rabbitTemplate.correlationConvertAndSend(String.valueOf(userId), correlationData);
	}

	public void createCustomer(int repetitions, int interval_seg, int threads, int sleep) {
		// BEGIN CONFIG
		final long interval_nano = TimeUnit.SECONDS.toNanos(interval_seg);
		// END CONFIG

		ExecutorService executor;
		for (int i = 0; i < repetitions; i++) {

			resetExchange();

			int activeThreadsCount = Thread.activeCount();
			executor = Executors.newFixedThreadPool(threads);

			currentStart = System.nanoTime();
			currentCorrelationDataKey = String.format("%d-%d-%d", i, threads, sleep);

			for (int j = 0; j < threads; j++) {
				executor.execute(new Task(this, currentStart, interval_nano, sleep, i, threads));
			}
			int sobra = stopExecutor(interval_seg, executor);

			logger.info(String.format("FIM [Iteração][Ativas][Interrompidas] - [%d][%d][%d]", i, activeThreadsCount,
					sobra));

			printStatistics(threads, sleep, recCounter.getAndSet(0), refCounter.getAndSet(0), average.getAndSet(0));
		}
	}

	private void resetExchange() {
		amqpAdmin.deleteExchange(exchange.getName());
		amqpAdmin.declareExchange(exchange);
	}

	private void setUpConfirmation() {
		rabbitTemplate.setConfirmCallback((receivedCorrelationData, ack, cause) -> {
			if (currentCorrelationDataKey == null) {
				return;
			}

			String[] parts = receivedCorrelationData.getId().split("-");

			if (currentCorrelationDataKey.equals(String.format("%s-%s-%s", parts[0], parts[1], parts[2]))) {
				average.accumulateAndGet(System.nanoTime() - Long.parseLong(parts[3]),
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

	public void createCustomer(int repetitions, int interval, int threadStart, int threadIncrement, int threadEnd,
			int sleepStart, int sleepIncrement, int sleepEnd) {
		for (int i = threadStart; i <= threadEnd; i = i + threadIncrement) {
			createCustomer(repetitions, interval, i, sleepStart, sleepIncrement, sleepEnd);
		}
	}

}
