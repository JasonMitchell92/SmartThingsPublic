/**
 *  Lights On when Entering
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
    name: "Lights On when Entering",
    namespace: "jasonstuff",
    author: "Jason Mitchell",
    description: "Turn the lights on when I arrive and the door opens",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Choose the door and presence sensors..."){
		input "contact", "capability.contactSensor", title: "Door Opens", required: true, multiple: true
		input "presenceSensors", "capability.presenceSensor", title: "Presence Of", required: true, multiple: true
	}
	section("And turn on these lights...") {
		input "myLights", "capability.switch", title: "Lights To Be Turned On", required: true, multiple: true
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
	subscribe(contact, "contact.open", checkPresence)
}

def checkPresence(evt) {

	presenceSensors.findAll { sensor ->
    	if(sensor.currentState("presence").value == "present") {
        	myLights?.on()
        }
    }
}