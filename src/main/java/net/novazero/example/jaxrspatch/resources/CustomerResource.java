package net.novazero.example.jaxrspatch.resources;

import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.novazero.example.jaxrspatch.dao.CustomerDao;
import net.novazero.example.jaxrspatch.models.Customer;
import net.novazero.example.jaxrspatch.patchsupport.MediaTypes;
import net.novazero.example.jaxrspatch.patchsupport.ObjectPatch;
import net.novazero.example.jaxrspatch.patchsupport.PATCH;

@Path("/customers")
public class CustomerResource {
	
	private final CustomerDao customerDao;
	
	@Inject
	public CustomerResource(CustomerDao customerDao) {
		this.customerDao = customerDao;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<Customer> getCustomers() {
		return (customerDao.getAll());
	}
	
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCustomer(@PathParam("id") long id) {
		Customer customer = customerDao.get(id);
		
		if (customer == null) {
			throw new NotFoundException();
		}
		
		return Response.status(Response.Status.CREATED).entity(customer).build();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Customer addCustomer(Customer customer) {
		return customerDao.insert(customer);
	}
	
	@PUT
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Customer updateCustomer(@PathParam("id") long id, Customer customer) {
		if (customerDao.get(id) == null) {
			throw new NotFoundException();
		}
		
		customer.setId(id);
		
		customerDao.update(customer);
		
		return customer;
	}
	
	@PATCH
	@Path("/{id}")
	@Consumes({MediaType.APPLICATION_JSON, MediaTypes.APPLICATION_JSON_PATCH})
	@Produces(MediaType.APPLICATION_JSON)
	public Customer patchCustomer(@PathParam("id") long id, ObjectPatch patch) {
		Customer customer = customerDao.get(id);
		
		if (customer == null) {
			throw new NotFoundException();
		}
		
		customer = patch.apply(customer);
		
		customer.setId(id);
		
		customerDao.update(customer);
		
		return customer;
	}
	
	@DELETE
	@Path("/{id}")
	public void deleteCustomer(@PathParam("id") long id) {
		Customer customer = customerDao.get(id);
		
		if (customer == null) {
			throw new NotFoundException();
		}
		
		customerDao.delete(customer);
	}

}
