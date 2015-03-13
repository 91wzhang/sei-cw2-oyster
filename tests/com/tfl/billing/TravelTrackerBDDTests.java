/**
 * 
 */
package com.tfl.billing;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

/**
 * @author Wai
 * 
 */
@RunWith(Cucumber.class)
@CucumberOptions(
		features = "classpath:com/tfl/billing/doc", 
		glue = "classpath:com/tfl/billing",
		plugin = { "progress", "html:reports/cucumber" }
)
public class TravelTrackerBDDTests {
	
}
