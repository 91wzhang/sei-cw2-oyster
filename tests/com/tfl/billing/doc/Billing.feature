Feature: Travelling record	
 	
Background: 
    Given a mocked customer database which contains:
        | Fred Bloggs   | 38400000-8cf0-11bd-b23e-10b96e4ef00d |   
        | Shelly Cooper | 3f1b3b55-f266-4426-ba1b-bcc506541866 |
    And a mocked payments system   

Scenario Outline: Billing
    Given <customer> made the following travel: <travel>
    Then <customer> is charged for <fee>
    
Examples: 
    |    customer   |                                  travel                                 |  fee  |
    | Fred Bloggs   | PADDINGTON@08:00-BAKER_STREET@08:30, BAKER_STREET@10:40-ROYAL_OAK@11:20 |  6.50 |
    | Shelly Cooper | PADDINGTON@08:00-BAKER_STREET@08:30                                     |  3.80 |
    | Shelly Cooper | NOTTING_HILL_GATE@11:10-BAKER_STREET@11:30                              |  1.60 |