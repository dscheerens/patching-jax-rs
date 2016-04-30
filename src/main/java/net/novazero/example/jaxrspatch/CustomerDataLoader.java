package net.novazero.example.jaxrspatch;

import java.util.Arrays;

import javax.inject.Inject;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import net.novazero.example.jaxrspatch.dao.CustomerDao;
import net.novazero.example.jaxrspatch.models.Customer;

public class CustomerDataLoader implements Feature {
	
	@Inject
	private CustomerDao customerDao;

	@Override
	public boolean configure(FeatureContext context) {
		customerDao.insert(new Customer()
			.setName("First customer")
		);
		
		customerDao.insert(new Customer()
			.setName("Second customer")
			.setPhoneNumbers(Arrays.asList("01234", "56789"))
		);
		
		return true;
	}

}
