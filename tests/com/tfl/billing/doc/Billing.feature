Feature: Travelling record	
 	
Background: 
    Given a mocked customer database which contains:
        | Fred Bloggs   | 38400000-8cf0-11bd-b23e-10b96e4ef00d |   
        | Shelly Cooper | 3f1b3b55-f266-4426-ba1b-bcc506541866 |
    And a mocked payments system   

Scenario Outline: B1
    Given <customer> made the following travel: <travel>
    Then <customer> is charged for <fee>
    
Examples: 
    |    customer   |                                  travel                                 |  fee  |
    | Fred Bloggs   | PADDINGTON@08:00-BAKER_STREET@08:30, PADDINGTON@10:40-KINGS_CROSS@11:20 |  5.60 |
    | Shelly Cooper | PADDINGTON@08:00-BAKER_STREET@08:30                                     |  3.20 |
    | Shelly Cooper | NOTTING_HILL_GATE@11:10-BAKER_STREET@11:40                              |  2.40 |