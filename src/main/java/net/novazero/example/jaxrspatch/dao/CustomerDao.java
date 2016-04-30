package net.novazero.example.jaxrspatch.dao;

import java.util.Collection;

import net.novazero.example.jaxrspatch.models.Customer;

public interface CustomerDao {

	Customer get(long id);
	
	Collection<Customer> getAll();
	
	Customer insert(Customer customer);
	
	Customer update(Customer customer);
	
	Customer delete(Customer customer);
}
