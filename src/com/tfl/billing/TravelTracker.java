package com.tfl.billing;

import com.oyster.*;
import com.tfl.external.Customer;
import com.tfl.external.CustomerDatabase;
import com.tfl.external.PaymentsSystem;

import java.math.BigDecimal;
import java.util.*;

public class TravelTracker implements ScanListener {

    static final BigDecimal OFF_PEAK_JOURNEY_PRICE = new BigDecimal(2.40);
    static final BigDecimal PEAK_JOURNEY_PRICE = new BigDecimal(3.20);

    private final List<JourneyEvent> eventLog;
    private final Set<UUID> currentlyTravelling;
    
    /**
     * Default constructor
     */
    public TravelTracker() {
    	eventLog = new ArrayList<JourneyEvent>();
    	currentlyTravelling = new HashSet<UUID>();        
    }
    
    /**
     * For testing only
     * @param eventLog
     * @param currentlyTravelling
     */
    protected TravelTracker(List<JourneyEvent> eventLog, Set<UUID> currentlyTravelling) {
    	this.eventLog = eventLog;
    	this.currentlyTravelling = currentlyTravelling;        
    }

    public void chargeAccounts() {
        CustomerDatabase customerDatabase = getCustomerDatabase();

        List<Customer> customers = customerDatabase.getCustomers();
        for (Customer customer : customers) {
            totalJourneysFor(customer);
        }
    }

    private void totalJourneysFor(Customer customer) {
        List<JourneyEvent> customerJourneyEvents = getCustomerEvents(customer, eventLog);
        List<Journey> journeys = getJourneysFromLog(customerJourneyEvents);
        BigDecimal customerTotal = getCustomerTotal(journeys);        
        getPaymentsSystem().charge(customer, journeys, roundToNearestPenny(customerTotal));
    }

    private BigDecimal roundToNearestPenny(BigDecimal poundsAndPence) {
        return poundsAndPence.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private boolean peak(Journey journey) {
        return peak(journey.startTime()) || peak(journey.endTime());
    }

    private boolean peak(Date time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return (hour >= 6 && hour <= 9) || (hour >= 17 && hour <= 19);
    }

    public void connect(OysterCardReader... cardReaders) {
        for (OysterCardReader cardReader : cardReaders) {
            cardReader.register(this);
        }
    }

    public void cardScanned(UUID cardId, UUID readerId) {
        if (currentlyTravelling.contains(cardId)) {
            eventLog.add(getNewJourneyEnd(cardId, readerId));
            currentlyTravelling.remove(cardId);
        } else {
            if (getCustomerDatabase().isRegisteredId(cardId)) {
                currentlyTravelling.add(cardId);
                eventLog.add(getNewJourneyStart(cardId, readerId));
            } else {
            	raiseUnknownOysterCardException(cardId);
            }
        }
    }
    
    protected CustomerDatabase getCustomerDatabase() {
    	return CustomerDatabase.getInstance();
    }
    
    protected PaymentsSystem getPaymentsSystem() {
    	return PaymentsSystem.getInstance();
    }
    
    protected JourneyStart getNewJourneyStart(UUID cardId, UUID readerId) {
    	return new JourneyStart(cardId, readerId);
    }
    
    protected JourneyEnd getNewJourneyEnd(UUID cardId, UUID readerId) {
    	return new JourneyEnd(cardId, readerId);
    }
    
    protected void raiseUnknownOysterCardException(UUID cardId) {
    	throw new UnknownOysterCardException(cardId);
    }
    
    protected List<JourneyEvent> getCustomerEvents(Customer customer, List<JourneyEvent> eventLog) {
    	List<JourneyEvent> customerJourneyEvents = new ArrayList<JourneyEvent>();
        for (JourneyEvent journeyEvent : eventLog) {
            if (journeyEvent.cardId().equals(customer.cardId())) {
                customerJourneyEvents.add(journeyEvent);
            }
        }
        
        return customerJourneyEvents;
    }
    
    protected List<Journey> getJourneysFromLog(List<JourneyEvent> customerJourneyEvents) {
    	List<Journey> journeys = new ArrayList<Journey>();

        JourneyEvent start = null;
        for (JourneyEvent event : customerJourneyEvents) {
            if (event instanceof JourneyStart) {
                start = event;
            }
            if (event instanceof JourneyEnd && start != null) {
                journeys.add(new Journey(start, event));
                start = null;
            }
        }
        
        return journeys;
    }
    
    protected BigDecimal getCustomerTotal(List<Journey> journeys) {
    	BigDecimal customerTotal = new BigDecimal(0);
    	for (Journey journey : journeys) {
            BigDecimal journeyPrice = OFF_PEAK_JOURNEY_PRICE;
            if (peak(journey)) {
                journeyPrice = PEAK_JOURNEY_PRICE;
            }
            customerTotal = customerTotal.add(journeyPrice);
        }
    	
    	return customerTotal;
    }
}
