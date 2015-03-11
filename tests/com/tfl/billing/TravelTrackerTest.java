/**
 * 
 */
package com.tfl.billing;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
	
	static final BigDecimal PEAK_JOURNEY_LONG_PRICE = new BigDecimal("3.80");
	static final BigDecimal PEAK_JOURNEY_SHORT_PRICE = new BigDecimal("2.90");
	static final BigDecimal OFF_PEAK_LONG_JOURNEY_PRICE = new BigDecimal("2.70");
	static final BigDecimal OFF_PEAK_SHORT_JOURNEY_PRICE = new BigDecimal("1.60");
	static final BigDecimal PEAK_DAILY_CAPS = new BigDecimal("7.00");
	static final BigDecimal OFF_PEAK_DAILY_CAPS = new BigDecimal("9.00");
	
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
		UUID cardId1 = UUID.randomUUID();
		UUID cardId2 = UUID.randomUUID();

		UUID readerId = UUID.randomUUID();
		Customer mockCustomer1 = mock(Customer.class);
		Customer mockCustomer2 = mock(Customer.class);
		doReturn(cardId1).when(mockCustomer1).cardId();
		doReturn(cardId2).when(mockCustomer2).cardId();
		
		List<Customer> mockCustomerCollection = new ArrayList<Customer>();
		mockCustomerCollection.add(mockCustomer1);
		mockCustomerCollection.add(mockCustomer2);
		
		doReturn(mockCustomerCollection).when(mockCustomerDatabase).getCustomers();
		
		JourneyEvent[] mockJourney;		
		try {
			mockJourney = generateMockJourney(cardId1, readerId, "08:00", "08:10");
			mockEventLog.add(mockJourney[0]);
			mockEventLog.add(mockJourney[1]);
			mockJourney = generateMockJourney(cardId2, readerId, "11:10", "11:25");
			mockEventLog.add(mockJourney[0]);
			mockEventLog.add(mockJourney[1]);
		} catch (ParseException e) {
			e.printStackTrace();
		}	
		
		tracker.chargeAccounts();						

		verify(mockPaymentsSystem).charge(eq(mockCustomer1), anyListOf(Journey.class), eq(PEAK_JOURNEY_SHORT_PRICE));
		verify(mockPaymentsSystem).charge(eq(mockCustomer2), anyListOf(Journey.class), eq(OFF_PEAK_SHORT_JOURNEY_PRICE));				
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
	
	/**
	 * 
	 * @param cardId
	 * @param readerId
	 * @param startTime
	 * @param endTime
	 * @return
	 * @throws ParseException
	 */
	private JourneyEvent[] generateMockJourney(UUID cardId, UUID readerId, String startTime, String endTime) throws ParseException {
		DateFormat format = new SimpleDateFormat("HH:mm");
		JourneyEvent[] journey = new JourneyEvent[2];

		Date dt = format.parse(startTime);			
		journey[0] = spy(new JourneyStart(cardId, readerId));
		doReturn(dt.getTime()).when(journey[0]).time();					
		
		dt = format.parse(endTime);			
		journey[1] = spy(new JourneyEnd(cardId, readerId));
		doReturn(dt.getTime()).when(journey[1]).time();
		
		return journey;				
	}

}
