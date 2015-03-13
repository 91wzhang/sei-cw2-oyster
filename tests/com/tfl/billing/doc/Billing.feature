Feature: Billing	
 	
Background: 
    Given a mocked customer database which contains:
        | Fred Bloggs   | 38400000-8cf0-11bd-b23e-10b96e4ef00d |   
        | Shelly Cooper | 3f1b3b55-f266-4426-ba1b-bcc506541866 |
    And a mocked payments system   

Scenario Outline: Billing
    When <customer> made the following travel: <travel>
    Then <customer> is charged for <fee>
    
Examples: 
    |    customer   |                                  travel                                 |  fee  |
    # peak1 long below_cap
    | Shelly Cooper | KENSAL_GREEN@8:00-PADDINGTON@8:30                              		  |  3.80 |

    # peak2 long below_cap:
    | Fred Bloggs   | PADDINGTON@17:40-BAKER_STREET@18:20                              		  |  3.80 |
    
    # off_peak1 long below_cap:
    | Shelly Cooper | KENSAL_GREEN@11:10-PADDINGTON@11:40                              		  |  2.70 |    
    
    # off_peak2 long below_cap:
    | Fred Bloggs   | PADDINGTON@20:10-BAKER_STREET@20:36                              		  |  2.70 |
  
    # peak1 short below_cap:
    | Shelly Cooper | KENSAL_GREEN@8:00-PADDINGTON@8:10                              		  |  2.90 |   

    # peak2 short below_cap: 
    | Fred Bloggs   | PADDINGTON@17:10-BAKER_STREET@17:20                              		  |  2.90 | 

    # off_peak1 short below_cap:
    | Shelly Cooper | NOTTING_HILL_GATE@11:10-SOUTH_KENSINGTON@11:25                          |  1.60 | 
    
    # off_peak2 short below_cap:
    | Fred Bloggs   | KENSAL_GREEN@20:10-PADDINGTON@20:17                              		  |  1.60 | 

    # peak above_cap:
	| Shelly Cooper | PADDINGTON@08:00-BAKER_STREET@08:30, PADDINGTON@10:40-KINGS_CROSS@11:20, KENSAL_GREEN@8:00-PADDINGTON@8:30 |  9.00 |

    # off_peak above_cap:
	| Fred Bloggs   | KENSAL_GREEN@11:10-PADDINGTON@11:40, PADDINGTON@10:40-KINGS_CROSS@11:20, KENSAL_GREEN@20:10-PADDINGTON@20:17 |  7.00 |
	