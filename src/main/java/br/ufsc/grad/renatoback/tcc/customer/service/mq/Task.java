package br.ufsc.grad.renatoback.tcc.customer.service.mq;

import org.springframework.amqp.rabbit.support.CorrelationData;

public class Task implements Runnable {

	private final CustomerService service;
	private final Long start;
	private final Long interval;
	private final Integer sleep;
	private final CorrelationData correlationData;

	public Task(CustomerService service, Long start, Long interval, Integer sleep, CorrelationData correlationData) {
		this.service = service;
		this.start = start;
		this.interval = interval;
		this.sleep = sleep;
		this.correlationData = correlationData;
	}

	@Override
	public void run() {
		while (System.nanoTime() - start < interval) {
			service.createCustomer(correlationData);

			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

}
