package net.novazero.example.jaxrspatch.dao;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.novazero.example.jaxrspatch.models.Customer;

public class InMemoryCustomerDao implements CustomerDao {
	
	private final Map<Long, Customer> customers = new HashMap<>();
	
	private volatile long nextId = 1;

	public synchronized Customer get(long id) {
		return customers.get(id);
	}
	
	public synchronized Collection<Customer> getAll() {
		return customers.values();
	}

	public synchronized Customer insert(Customer customer) {
		long customerId = nextId++;
		customer.setId(customerId);
		customers.put(customerId, customer);
		return customer;
	}

	public synchronized Customer update(Customer customer) {
		customers.put(customer.getId(), customer);
		return customer;
	}

	public synchronized Customer delete(Customer customer) {
		customers.remove(customer.getId());
		return customer;
	}
	
}
