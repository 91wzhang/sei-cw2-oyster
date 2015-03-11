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
import cucumber.api.java.en.When;

/**
 * @author DanDan Lyu, Wei Zhang, Ze Chen
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
	
	@When("^(.*) made the following travel: (.*)$")
	public void CustomerMadeTravel(String customer, List<String> travel) throws ParseException {
		UUID cardId = customers.get(customer).cardId();
		for (String journey : travel) {
			String[] events = journey.split("-");
			String startTime = events[0].split("@")[1];
			String endTime = events[1].split("@")[1];
			
			try {
				addMockJourney(cardId, mockReaderId, startTime, endTime);							
			} catch (ParseException e) {
				e.printStackTrace();
			}	
		}
	}	

	@Then("^(.*) is charged for (\\d+\\.\\d+)$")
	public void ChargeCustomer(String customer, String fee) throws Throwable {		       
		doReturn(this.mockCustomerDatabase).when(this.tracker).getCustomerDatabase();
		doReturn(this.mockPaymentsSystem).when(this.tracker).getPaymentsSystem();
		doReturn(this.mockCustomerCollection).when(this.mockCustomerDatabase).getCustomers();
		
		Customer c = this.customers.get(customer);		
		this.tracker.chargeAccounts();		

		verify(this.mockPaymentsSystem).charge(eq(c), anyListOf(Journey.class), eq(new BigDecimal(fee)));		
	}
	
	/**
	 * 
	 * @param cardId
	 * @param readerId
	 * @param startTime
	 * @param endTime
	 * @return
	 * @throws ParseException
	 */
	private void addMockJourney(UUID cardId, UUID readerId, String startTime, String endTime) throws ParseException {
		DateFormat format = new SimpleDateFormat("HH:mm");

		Date dt = format.parse(startTime);			
		JourneyStart start = spy(new JourneyStart(cardId, readerId));
		doReturn(dt.getTime()).when(start).time();					
		
		dt = format.parse(endTime);			
		JourneyEnd end = spy(new JourneyEnd(cardId, readerId));
		doReturn(dt.getTime()).when(end).time();
		
		this.mockEventLog.add(start);
		this.mockEventLog.add(end);		
	}
}
