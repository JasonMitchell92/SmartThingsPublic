/**
 *  Circadian Rhythym 2.0
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
    name: "Circadian Rhythym Coordinator",
    namespace: "jasonstuff",
    author: "Jason Mitchell",
    description: "Sync your lighting to a natural color temperature for improved sleep patterns.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    onlyInstance: true
)

preferences {
    page(name: "pageOne", title: "Bulb and Light Selection", nextPage: "pageTwo", uninstall: true) {
        section("Choose the lights") {
            input "ctBulbs", "capability.colorTemperature", title: "Color Temperature Bulbs", multiple: true, required: true
            input "dBulbs", "capability.switchLevel", title: "Dimmable Bulbs", multiple: true, required: false
        }
    }
    page(name: "pageTwo", title: "Color Temperature Setup", nextPage: "pageThree") {
        section {
        	input "CTSunrise", "number", title: "Sunrise", description: "Color Temperature at Sunrise", defaultValue: 3500, required: true
            input "CT09", "number", title: "Mid-Morning", description: "Color Temperature at Mid-Morning", defaultValue: 5000, required: true
            input "CT12", "number", title: "High Noon", description: "Color Temperature at High Noon", defaultValue: 6000, required: true
            input "CT15", "number", title: "Afternoon", description: "Color Temperature at Afternoon", defaultValue: 4200, required: true
            input "CTSunset", "number", title: "Sunset", description: "Color Temperature at Sunset", defaultValue: 3500, required: true
            input "CT00", "number", title: "Night", description: "Color Temperature at Night", defaultValue: 2200, required: true
        }
    }
    page(name: "pageThree", title: "Brightness Level Setup", install: true, uninstall: true) {
        section {
        	input "DMSunrise", "number", title: "Dawn", description: "Brightness Level at Sunrise", defaultValue: 85, required: true
            input "DM09", "number", title: "Mid-Morning", description: "Brightness Level at Mid-Morning", defaultValue: 90, required: true
            input "DM12", "number", title: "High Noon", description: "Brightness Level at High Noon", defaultValue: 100, required: true
            input "DM15", "number", title: "Afternoon", description: "Brightness Level at Afternoon", defaultValue: 90, required: true
            input "DMSunset", "number", title: "Sunset", description: "Brightness Level at Sunset", defaultValue: 75, required: true
            input "DM00", "number", title: "Night", description: "Brightness Level at Night", defaultValue: 50, required: true
        }
    }
}


//	----- INSTALL, UPDATE, INITIALIZE -----
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
	//Place the input variables into a list
	state.tMap  = [ 0.0,  6.0,  9.0, 12.0, 15.0, 19.0, 24.0]
    state.ctMap = [CT00, CTSunrise, CT09, CT12, CT15, CTSunset, CT00]
    state.dMap  = [DM00, DMSunrise, DM09, DM12, DM15, DMSunset, DM00]
    
    //Dump the input variables we just used
    CT00.dump(); CTSunrise.dump(); CT09.dump(); CT12.dump(); CT15.dump(); CTSunset.dump()
    DM00.dump(); DMSunrise.dump(); DM09.dump(); DM12.dump(); DM15.dump(); DMSunset.dump()
    
    
    //Subscribe to a schedule that runs every half hour
    schedule("0 0/15 * * * ?", scheduledTimeHandler)
    
    //Subscribe to every instance a light switches on
	subscribe(ctBulbs,"switch.on",switchOnHandler)
	subscribe(dBulbs,"switch.on",switchOnHandler)
    
    //Subscribe to whenever the sunrise or sunset schedule changes
    subscribe(location, "sunriseTime", syncTimeMap)
    subscribe(location, "sunsetTime", syncTimeMap)
    
    
    //Sync the time map after installing
    syncTimeMap()
    
    //Syncronize every bulb after installing
    scheduledTimeHandler()
}



//	----- INSTALL, UPDATE, INITIALIZE -----
def syncTimeMap(evt) {
	def sunrise = syncWithSun("sunriseTime")
    def sunset = syncWithSun("sunsetTime")
    
	def highnoon = sunrise + (sunset - sunrise)/2
    
	state.tMap  = [ 0.0,  sunrise,  9.0, highnoon, 15.0, sunset, 24.0]
    //log.debug "sunrise: ${sunrise}, noon: ${highnoon}, sunset: ${sunset}"
}

def syncWithSun(sunriseOrSunset) {
    def nextSunriseSunsetTimeDate = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", location.currentValue(sunriseOrSunset))
    def Time = new Date(nextSunriseSunsetTimeDate.time)
    
    int hour = Time.format('hh',location.timeZone) as Integer
    int minute = Time.format('mm',location.timeZone) as Integer
    
    if(sunriseOrSunset == "sunsetTime") {hour += 12}
    
    return hour + (minute / 60)
}


//Gets the hour integer in current locale
private getTime() {
	def cal = Calendar.getInstance(location.getTimeZone())
    return cal.get(Calendar.HOUR_OF_DAY) + (cal.get(Calendar.MINUTE)/60)
}

private getInterpolatedValue(time, map) {
	def times = state.tMap
    int result = 4000
    int I = 0
    
    //look through all the time bands
    while(I < 7) {
    	def lowerTime = times[I]
        def upperTime = times[I+1]
        
        //if the time is within a time band
        if( time >= lowerTime  &&  time < upperTime) {
            def lowerValue = map[I]
            def upperValue = map[I+1]
            
			//interpolate between the values associated with the two times
            result = lowerValue + ((time - lowerTime) * ((upperValue - lowerValue) / (upperTime - lowerTime)))
            
        	break
        }
        I += 1
    }
    return result
}

private void setCTBulb(ctBulb, colorTemperature) {
    if(ctBulb.currentValue("switch") == "on") {
        ctBulb.setColorTemperature(colorTemperature)
        
        log.debug "${ctBulb} color temperature set to ${colorTemperature}"
    }
}

private void setDBulb(dBulb, brightness) {
    if(dBulb.currentValue("switch") == "on") {
        dBulb.setLevel(brightness)

        log.debug "${dBulb} level set to ${brightness}"
    }
}

//	----- INSTALL, UPDATE, INITIALIZE -----
def scheduledTimeHandler() {
    def time = getTime()
    
	//get environmental variables
    def brightness = getInterpolatedValue(time, state.dMap)
    def colorTemperature = getInterpolatedValue(time, state.ctMap)
    
    log.debug "Brightness set to ${brightness}"
    log.debug "Color Temperature set to ${colorTemperature}"
    
    //change all bulbs that need to be changed
    for(ctBulb in ctBulbs) { setCTBulb(ctBulb, colorTemperature) }
    for(dBulb in dBulbs) { setDBulb(dBulb, brightness) }
}

//When a light switches on
def switchOnHandler(evt) {
	def bulb = evt.getDevice()
    def time = getTime()
    
    //If the bulb is color temp changing
    if(bulb.deviceNetworkId in ctBulbs?.deviceNetworkId) {
    	def colorTemperature = getInterpolatedValue(time, state.ctMap)
        
    	setCTBulb(bulb, colorTemperature)
    }
    
    //If the bulb is dimmable
    if(bulb.deviceNetworkId in dBulbs?.deviceNetworkId) {
    	def brightness = getInterpolatedValue(time, state.dMap)
        
    	setDBulb(bulb, brightness)
    }
}