/**
 * 
 */
package com.tfl.billing;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.mockito.MockitoAnnotations;

import com.tfl.external.Customer;
import com.tfl.external.CustomerDatabase;
import com.tfl.external.PaymentsSystem;

import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

/**
 * @author Wai
 *
 */
public class BillingSteps {	
	
	private List<Customer> mockCustomerCollection;		
	private CustomerDatabase mockCustomerDatabase;		
	private PaymentsSystem mockPaymentsSystem;
	private List<JourneyEvent> mockEventLog;
    private Set<UUID> mockCurrentlyTravelling;    
	private TravelTracker tracker;
	private Map<String, Customer> customers;
	private UUID mockReaderId;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		this.mockCustomerCollection = new ArrayList<Customer>();
		this.mockCustomerDatabase = mock(CustomerDatabase.class);
		this.mockPaymentsSystem = mock(PaymentsSystem.class);

		this.mockCurrentlyTravelling = new HashSet<UUID>();
		this.mockEventLog = new ArrayList<JourneyEvent>();
		this.mockReaderId = UUID.randomUUID();
		this.customers = new HashMap<String, Customer>();
		this.tracker = spy(new TravelTracker(mockEventLog, mockCurrentlyTravelling));						
	}
	
	@Given("^a mocked customer database which contains:$")
	public void SetUpCustomers(Map<String, UUID> customers) {
		this.mockCustomerDatabase = mock(CustomerDatabase.class);		
		for (Entry<String, UUID> customer : customers.entrySet()) {
			UUID cardId = customer.getValue();
			Customer c = mock(Customer.class);
			doReturn(cardId).when(c).cardId();
			doReturn(true).when(this.mockCustomerDatabase).isRegisteredId(cardId);
			this.mockCurrentlyTravelling.add(cardId);
			this.customers.put(customer.getKey(), c);
			this.mockCustomerCollection.add(c);
		}					    
	}

	@Given("^a mocked payments system$")
	public void SetUpPaymentsSystem() {
		this.mockPaymentsSystem = mock(PaymentsSystem.class); 
	}
	
	@Given("^(.*) made the following travel: (.*)$")
	public void CustomerMadeTravel(String customer, List<String> travel) throws ParseException {
		UUID cardId = customers.get(customer).cardId();
		for (String journey : travel) {
			String[] events = journey.split("-");
			
			DateFormat format = new SimpleDateFormat("HH:mm");

			String[] info = events[0].split("@");
			Date dt = format.parse(info[1]);			
			JourneyStart start = spy(new JourneyStart(cardId, mockReaderId));
			doReturn(dt.getTime()).when(start).time();			
			mockEventLog.add(start);
			
			info = events[1].split("@");
			dt = format.parse(info[1]);			
			JourneyEnd end = spy(new JourneyEnd(cardId, mockReaderId));
			doReturn(dt.getTime()).when(end).time();			
			mockEventLog.add(end);			
		}
	}	

	@Then("^(.*) is charged for (\\d+\\.\\d+)$")
	public void ChargeCustomer(String customer, String fee) throws Throwable {		       
		doReturn(mockCustomerDatabase).when(this.tracker).getCustomerDatabase();
		doReturn(mockPaymentsSystem).when(this.tracker).getPaymentsSystem();
		doReturn(this.mockCustomerCollection).when(mockCustomerDatabase).getCustomers();
		
		Customer c = this.customers.get(customer);		
		tracker.chargeAccounts();		

		verify(mockPaymentsSystem).charge(eq(c), anyListOf(Journey.class), eq(new BigDecimal(fee)));		
	}
}
