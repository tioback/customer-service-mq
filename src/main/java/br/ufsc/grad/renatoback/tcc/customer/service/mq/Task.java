package br.ufsc.grad.renatoback.tcc.customer.service.mq;

import org.springframework.amqp.rabbit.support.CorrelationData;

public class Task implements Runnable {

	private final CustomerService service;
	private final Long start;
	private final Long interval;
	private final Integer sleep;
	private final Integer iteration;
	private final Integer threads;

	public Task(CustomerService service, Long start, Long interval, Integer sleep, Integer iteration, Integer threads) {
		this.service = service;
		this.start = start;
		this.interval = interval;
		this.sleep = sleep;
		this.iteration = iteration;
		this.threads = threads;
	}

	@Override
	public void run() {
		long now;
		while ((now = System.nanoTime()) - start < interval) {
			service._createCustomer(new CorrelationData(String.format("%d-%d-%d-%d", iteration, threads, sleep, now)));

			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

}
