/**
 * 
 */
package com.tfl.billing;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import static org.mockito.Mockito.*;

import org.mockito.runners.MockitoJUnitRunner;

import com.oyster.OysterCardReader;
import com.tfl.external.Customer;
import com.tfl.external.CustomerDatabase;
import com.tfl.external.PaymentsSystem;

import static org.hamcrest.CoreMatchers.instanceOf;


/**
 * @author Wai
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TravelTrackerTest {

	@Mock
	private List<JourneyEvent> mockEventLog;
	
	@Mock
    private Set<UUID> mockCurrentlyTravelling;
	
	@Mock
	private CustomerDatabase mockCustomerDatabase;
	
	@Mock
	private PaymentsSystem mockPaymentsSystem;
		
	private TravelTracker tracker;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		tracker = spy(new TravelTracker(mockEventLog, mockCurrentlyTravelling));
		
		doReturn(mockCustomerDatabase).when(tracker).getCustomerDatabase();
		doReturn(mockPaymentsSystem).when(tracker).getPaymentsSystem();				
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link com.tfl.billing.TravelTracker#chargeAccounts()}.
	 */
	@Test
	public void testChargeAccounts() {
		UUID cardId = UUID.randomUUID();
		Customer mockCustomer = mock(Customer.class);
		doReturn(cardId).when(mockCustomer).cardId();
		
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link com.tfl.billing.TravelTracker#connect(com.oyster.OysterCardReader[])}.
	 */
	@Test
	public void testConnect() {		
		OysterCardReader mockCardReader1 = mock(OysterCardReader.class);
		OysterCardReader mockCardReader2 = mock(OysterCardReader.class);
		
		tracker.connect(mockCardReader1, mockCardReader2);
		
		verify(mockCardReader1).register(tracker);
		verify(mockCardReader2).register(tracker);		
	}

	/**
	 * Test method for {@link com.tfl.billing.TravelTracker#cardScanned(java.util.UUID, java.util.UUID)}.
	 */
	@Test
	public void testCardScannedForLeave() {
		JourneyEnd mockJourneyEnd = mock(JourneyEnd.class);
		doReturn(mockJourneyEnd).when(tracker).getNewJourneyEnd(any(UUID.class), any(UUID.class));		
		doReturn(true).when(mockCurrentlyTravelling).contains(any(UUID.class));
		
		UUID cardId = UUID.randomUUID();
		UUID readerId = UUID.randomUUID();
		tracker.cardScanned(cardId, readerId);
		
		verify(tracker).getNewJourneyEnd(cardId, readerId);
		verify(mockEventLog).add(mockJourneyEnd);
        verify(mockCurrentlyTravelling).remove(cardId);
	}	
	
	/**
	 * Test method for {@link com.tfl.billing.TravelTracker#cardScanned(java.util.UUID, java.util.UUID)}.
	 */
	@Test
	public void testCardScannedForEnterWithRegisteredCard() {
		JourneyStart mockJourneyStart = mock(JourneyStart.class);
		doReturn(mockJourneyStart).when(tracker).getNewJourneyStart(any(UUID.class), any(UUID.class));		
		doReturn(false).when(mockCurrentlyTravelling).contains(any(UUID.class));
		doReturn(true).when(mockCustomerDatabase).isRegisteredId(any(UUID.class));
		
		UUID cardId = UUID.randomUUID();
		UUID readerId = UUID.randomUUID();
		tracker.cardScanned(cardId, readerId);
		
		verify(mockCurrentlyTravelling).add(cardId);
		verify(tracker).getNewJourneyStart(cardId, readerId);
		verify(mockEventLog).add(mockJourneyStart);
	}
	
	/**
	 * Test method for {@link com.tfl.billing.TravelTracker#cardScanned(java.util.UUID, java.util.UUID)}.
	 */
	@Test
	public void testCardScannedForEnterWithUnregisteredCard() {
		doReturn(false).when(mockCurrentlyTravelling).contains(any(UUID.class));
		doReturn(false).when(mockCustomerDatabase).isRegisteredId(any(UUID.class));
		doNothing().when(tracker).raiseUnknownOysterCardException(any(UUID.class));
		
		UUID cardId = UUID.randomUUID();		UUID readerId = UUID.randomUUID();
		
		tracker.cardScanned(cardId, readerId);
		
		verify(tracker).raiseUnknownOysterCardException(cardId);
	}

}
