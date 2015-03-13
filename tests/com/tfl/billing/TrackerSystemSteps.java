/**
 * 
 */
package com.tfl.billing;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

import com.oyster.OysterCardReader;
import com.tfl.external.CustomerDatabase;

import cucumber.api.PendingException;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

/**
 * @author Wai
 *
 */
public class TrackerSystemSteps {
		
	private List<JourneyEvent> mockEventLog = new ArrayList<JourneyEvent>();
    private Set<UUID> mockCurrentlyTravelling = new HashSet<UUID>();    
	private TravelTracker tracker;
	private CustomerDatabase mockCustomerDatabase = mock(CustomerDatabase.class);
	private OysterCardReader mockCardReader;
	private UUID mockCardId = UUID.randomUUID();
	private UUID mockReaderId = UUID.randomUUID();
	private JourneyStart mockStart = new JourneyStart(mockCardId, mockReaderId);
	private JourneyEnd mockEnd = new JourneyEnd(mockCardId, mockReaderId);
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);	
		this.mockEventLog.clear();
		this.mockCurrentlyTravelling.clear();
		tracker = spy(new TravelTracker(mockEventLog, mockCurrentlyTravelling));
		doReturn(this.mockCustomerDatabase)
			.when(this.tracker)
			.getCustomerDatabase();
		doReturn(this.mockStart)
			.when(this.tracker)
			.getNewJourneyStart(this.mockCardId, this.mockReaderId);
		doReturn(this.mockEnd)
			.when(this.tracker)
			.getNewJourneyEnd(this.mockCardId, this.mockReaderId);
		doNothing()
			.when(this.tracker)
			.raiseUnknownOysterCardException(this.mockCardId);
	}

	@Given("^a card reader$")
	public void initReader() {
	    this.mockCardReader = mock(OysterCardReader.class);
	}

	@When("^the travel tracker tries to connect$")
	public void connect() {
	    this.tracker.connect(this.mockCardReader);
	}

	@Then("^the card reader register the tracker$")
	public void registerTracker() {
	    verify(this.mockCardReader).register(this.tracker);
	}

	@Given("^an Oyster card with status: \"(.*?)\"$")
	public void registerOysterCard(String status) {
		boolean isRegistered = status.equals("registered") ? true : false;
	    doReturn(isRegistered).when(this.mockCustomerDatabase).isRegisteredId(mockCardId);
	}

	@When("^the card is scanned for: \"(.*?)\"$")
	public void cardScanned(String status) {
		if (status.equals("leave")) {
			this.mockCurrentlyTravelling.add(this.mockCardId);
			this.mockEventLog.add(this.mockStart);
		}
		
	    this.tracker.cardScanned(this.mockCardId, this.mockReaderId);
	}

	@Then("^a journey-start event is recorded for the card$")
	public void journeyStartRecorded() {		
		assertTrue(this.mockEventLog.contains(this.mockStart));
	}

	@Then("^the card is recorded as travelling if not already be so$")
	public void travellingRecorded() {
	    assertTrue(this.mockCurrentlyTravelling.contains(this.mockCardId));		
	}

	@Then("^an unknown Oyster card exception is thrown$")
	public void unknownExceptionThrown() {		
	    verify(this.tracker).raiseUnknownOysterCardException(this.mockCardId);
	}

	@Then("^a journey-end event is recorded for the card$")
	public void journeyEndRecorded() {
		assertTrue(this.mockEventLog.contains(this.mockEnd));
	}


}
