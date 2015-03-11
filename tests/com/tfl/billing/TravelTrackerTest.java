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
 * @author DanDan Lyu, Wei Zhang, Ze Chen 
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
		this.mockEventLog = spy(new ArrayList<JourneyEvent>());
		this.tracker = spy(new TravelTracker(this.mockEventLog, this.mockCurrentlyTravelling));
		
		doReturn(this.mockCustomerDatabase).when(this.tracker).getCustomerDatabase();
		doReturn(this.mockPaymentsSystem).when(this.tracker).getPaymentsSystem();
		doNothing().when(this.mockPaymentsSystem).charge(any(Customer.class), anyListOf(Journey.class), any(BigDecimal.class));
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
		
		doReturn(mockCustomerCollection).when(this.mockCustomerDatabase).getCustomers();
		
		try {
			addMockJourney(cardId1, readerId, "08:00", "08:10");
			addMockJourney(cardId2, readerId, "11:10", "11:25");			
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		this.tracker.chargeAccounts();						

		verify(this.mockPaymentsSystem).charge(eq(mockCustomer1), anyListOf(Journey.class), eq(PEAK_JOURNEY_SHORT_PRICE));
		verify(this.mockPaymentsSystem).charge(eq(mockCustomer2), anyListOf(Journey.class), eq(OFF_PEAK_SHORT_JOURNEY_PRICE));				
	}

	/**
	 * Test method for {@link com.tfl.billing.TravelTracker#connect(com.oyster.OysterCardReader[])}.
	 */
	@Test
	public void testConnect() {		
		OysterCardReader mockCardReader1 = mock(OysterCardReader.class);
		OysterCardReader mockCardReader2 = mock(OysterCardReader.class);
		
		this.tracker.connect(mockCardReader1, mockCardReader2);
		
		verify(mockCardReader1).register(this.tracker);
		verify(mockCardReader2).register(this.tracker);		
	}

	/**
	 * Test method for {@link com.tfl.billing.TravelTracker#cardScanned(java.util.UUID, java.util.UUID)}.
	 */
	@Test
	public void testCardScannedForLeave() {
		JourneyEnd mockJourneyEnd = mock(JourneyEnd.class);
		UUID cardId = UUID.randomUUID();
		UUID readerId = UUID.randomUUID();
		doReturn(mockJourneyEnd).when(this.tracker).getNewJourneyEnd(cardId, readerId);		
		doReturn(true).when(this.mockCurrentlyTravelling).contains(cardId);		
		
		this.tracker.cardScanned(cardId, readerId);
		
		verify(this.mockEventLog).add(mockJourneyEnd);
        verify(this.mockCurrentlyTravelling).remove(cardId);
	}	
	
	/**
	 * Test method for {@link com.tfl.billing.TravelTracker#cardScanned(java.util.UUID, java.util.UUID)}.
	 */
	@Test
	public void testCardScannedForEnterWithRegisteredCard() {
		JourneyStart mockJourneyStart = mock(JourneyStart.class);
		UUID cardId = UUID.randomUUID();
		UUID readerId = UUID.randomUUID();
		
		doReturn(mockJourneyStart).when(this.tracker).getNewJourneyStart(cardId, readerId);		
		doReturn(false).when(this.mockCurrentlyTravelling).contains(cardId);
		doReturn(true).when(this.mockCustomerDatabase).isRegisteredId(cardId);
				
		this.tracker.cardScanned(cardId, readerId);
		
		verify(this.mockCurrentlyTravelling).add(cardId);
		verify(this.mockEventLog).add(mockJourneyStart);
	}
	
	/**
	 * Test method for {@link com.tfl.billing.TravelTracker#cardScanned(java.util.UUID, java.util.UUID)}.
	 */
	@Test
	public void testCardScannedForEnterWithUnregisteredCard() {
		doReturn(false).when(this.mockCurrentlyTravelling).contains(any(UUID.class));
		doReturn(false).when(this.mockCustomerDatabase).isRegisteredId(any(UUID.class));
		doNothing().when(this.tracker).raiseUnknownOysterCardException(any(UUID.class));
		
		UUID cardId = UUID.randomUUID();		UUID readerId = UUID.randomUUID();
		
		this.tracker.cardScanned(cardId, readerId);
		
		verify(this.tracker).raiseUnknownOysterCardException(cardId);
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
