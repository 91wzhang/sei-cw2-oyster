Feature: Tracker System

Scenario: Connect Card Readers
    Given a card reader
    When the travel tracker tries to connect
    Then the card reader register the tracker
    
Scenario: Scanning a registered Oyster card to enter a station
    Given an Oyster card with status: "registered"
    When the card is scanned for: "enter"
    Then a journey-start event is recorded for the card
     And the card is recorded as travelling if not already be so
     
Scenario: Scanning an un-registered Oyster card to enter a station
    Given an Oyster card with status: "unregistered"
    When the card is scanned for: "enter"
    Then an unknown Oyster card exception is thrown
    
Scenario: Scanning a registered Oyster card to leave a station
    Given an Oyster card with status: "registered"
    When the card is scanned for: "leave" 
    Then a journey-end event is recorded for the card