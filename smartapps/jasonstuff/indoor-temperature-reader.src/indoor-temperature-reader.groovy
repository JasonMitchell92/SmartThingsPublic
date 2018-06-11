/**
 *  Indoor Temperature Reader
 *
 *  Copyright 2018 Jason Mitchell
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Indoor Temperature Reader",
    namespace: "jasonstuff",
    author: "Jason Mitchell",
    description: "Reads and analyzes current indoor temperature based on multiple sensors. Optionally applies that temperature to a thermostat.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Indoor Temperature Sensors") {
        input "sensors", "capability.temperatureMeasurement", title: "Temperature Sensors", multiple: true, required: true
    }
	section("Virtual Thermometer to Set") {
        input "thermostat", "capability.thermostat", title: "Thermostat", multiple: false, required: true
	}
	section("Value to store") {
        input "valueType", "enum", title: "Average or Median Temperature?", options: ["Average", "Median", "Middle"], required: true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    
	unsubscribe()
	initialize()
}

def initialize() {
    subscribe(sensors, "temperature", temperatureChangedHandler)
    subscribe(sensors, "temperature", temperatureChangedHandler)
    state.temperatures = []
    
    temperatureChangedHandler();
}

private calcAvgTemp() {
    def avgTemp = 0
    def count = 0

    for(sensor in sensors) {
    	avgTemp += sensor.currentTemperature
        count++
    }
	return Math.round(avgTemp / count)
}

private calcMidTemp() {
    def minTemp = 100
    def maxTemp = 0

    for(sensor in sensors) {
    	def temp = sensor.currentTemperature
        
        minTemp = temp < minTemp ? temp : minTemp
        maxTemp = temp > maxTemp ? temp : maxTemp
    }
    
    return Math.round((minTemp + maxTemp) / 2)
}

private calcMedTemp() {
	def temps = []
    int result = 0
    int count = 0
    
    for(sensor in sensors) {
        temps.push(sensor.currentTemperature)
        count += 1
    }
    temps = temps.sort()
    
    if(count % 2 == 1) { 
    	//if odd number
    	result = temps[Math.floor(count/2)]
    }
    else
    {
    	//if even number
    	int Mid = (int)(count / 2)
        
        //result is average of two middle numbers
        result = (int)((temps[Mid] + temps[Mid-1])/2)
	}
    
    return result
}

def temperatureChangedHandler(evt) {
    def calcTemp = 0
    
    switch (valueType) {
        case "Average":
            calcTemp = calcAvgTemp()
        	log.debug "Calculated Average Temperature is ${calcTemp}F"
            break
        case "Middle":
            calcTemp = calcMidTemp()
        	log.debug "Calculated Median Temperature is ${calcTemp}F"
            break
        case "Median":
            calcTemp = calcMedTemp()
        	log.debug "Calculated Median Temperature is ${calcTemp}F"
            break
        default:
        	log.debug "Error in valueType case switch"
    }
    
    
    if(calcTemp != thermostat.currentTemperature)
	{
    	thermostat.setTemperature(calcTemp)
    }
    else
    {
		log.debug "Indoor Temperature has not changed."
    }
}