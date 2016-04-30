package net.novazero.example.jaxrspatch;

import javax.inject.Singleton;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import net.novazero.example.jaxrspatch.dao.CustomerDao;
import net.novazero.example.jaxrspatch.dao.InMemoryCustomerDao;

public class ServiceBinder extends AbstractBinder {

	@Override
	protected void configure() {
		bind(InMemoryCustomerDao.class)
			.to(CustomerDao.class)
			.in(Singleton.class);
	}

}
