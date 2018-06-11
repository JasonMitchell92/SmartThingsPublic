/**
 *  Smart Home Monitor - Notification
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
    name: "Smart Home Monitor - Notification",
    namespace: "jasonstuff",
    author: "Jason Mitchell",
    description: "Notifies the user when an alarm changes state.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

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
    subscribe(location, "alarmSystemStatus", alarmHandler)
}

def alarmHandler(evt) {
	def state = location.currentState("alarmSystemStatus")?.value
    def message = "Smart Home Monitor - "
    
    switch (state) {
        case 'away':
            message+= 'Fully Armed (Away Mode)'
            break
        case 'stay':
            message+= 'Partially Armed (Home Mode)'
            break
        case 'off':
        	message+= 'Disarmed'
            break
        default:
            message+= 'Error'
            break
    }   
    
    log.debug(message)
    
    if (sendPush) {
        sendPush(message)
    }
}