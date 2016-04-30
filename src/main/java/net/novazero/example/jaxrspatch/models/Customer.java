package net.novazero.example.jaxrspatch.models;

import java.util.List;

public class Customer {

	private Long id;
	
	private String name;
	
	private List<String> phoneNumbers;
	
	public Long getId() {
		return id;
	}
	
	public Customer setId(Long id) {
		this.id = id;
		return this;
	}
	
	public String getName() {
		return name;
	}
	
	public Customer setName(String name) {
		this.name = name;
		return this;
	}
	
	public List<String> getPhoneNumbers() {
		return phoneNumbers;
	}
	
	public Customer setPhoneNumbers(List<String> phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
		return this;
	}
}
