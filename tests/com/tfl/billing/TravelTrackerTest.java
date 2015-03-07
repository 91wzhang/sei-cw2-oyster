/**
 * 
 */
package com.tfl.billing;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.oyster.OysterCardReader;
import com.tfl.external.Customer;
import com.tfl.external.CustomerDatabase;
import com.tfl.external.PaymentsSystem;


/**
 * @author Wai
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TravelTrackerTest {
	
	@Mock
    private Set<UUID> mockCurrentlyTravelling;
	
	@Mock
	private CustomerDatabase mockCustomerDatabase;
	
	@Mock
	private PaymentsSystem mockPaymentsSystem;

	private List<JourneyEvent> mockEventLog;
	private TravelTracker tracker;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		mockEventLog = spy(new ArrayList<JourneyEvent>());
		tracker = spy(new TravelTracker(mockEventLog, mockCurrentlyTravelling));
		
		doReturn(mockCustomerDatabase).when(tracker).getCustomerDatabase();
		doReturn(mockPaymentsSystem).when(tracker).getPaymentsSystem();
		doNothing().when(mockPaymentsSystem).charge(any(Customer.class), anyListOf(Journey.class), any(BigDecimal.class));
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
		UUID readerId = UUID.randomUUID();
		Customer mockCustomer1 = mock(Customer.class);
		Customer mockCustomer2 = mock(Customer.class);
		doReturn(cardId).when(mockCustomer1).cardId();
		doReturn(cardId).when(mockCustomer2).cardId();
		
		List<Customer> mockCustomerCollection = new ArrayList<Customer>();
		mockCustomerCollection.add(mockCustomer1);
		mockCustomerCollection.add(mockCustomer2);
		
		doReturn(mockCustomerCollection).when(mockCustomerDatabase).getCustomers();
		
		JourneyStart mockStart = new JourneyStart(cardId, readerId);
		JourneyEnd mockEnd = new JourneyEnd(cardId, readerId);
		mockEventLog.add(mockStart);
		mockEventLog.add(mockEnd);
		doReturn(true).doReturn(false).when(tracker).peak(any(Journey.class));
		tracker.chargeAccounts();
		
		verify(mockPaymentsSystem).charge(mockCustomer1, anyListOf(Journey.class), tracker.roundToNearestPenny(TravelTracker.PEAK_JOURNEY_PRICE));
		verify(mockPaymentsSystem).charge(mockCustomer2, anyListOf(Journey.class), tracker.roundToNearestPenny(TravelTracker.OFF_PEAK_JOURNEY_PRICE));				
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
