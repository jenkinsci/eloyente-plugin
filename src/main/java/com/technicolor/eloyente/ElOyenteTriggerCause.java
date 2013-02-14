/*
   Copyright 2012 Technicolor

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.technicolor.eloyente;

import hudson.model.Cause;
import hudson.EnvVars;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Frank Vanderhallen
 */

public final class ElOyenteTriggerCause extends Cause {

	private String event;
	private String filter;
	private EnvVars vars;

	public ElOyenteTriggerCause(String event, String filter, EnvVars vars) {
		this.event = event;
		this.filter = filter;
		this.vars = vars;
	}

	@Override
	public String getShortDescription() {
            String e= event;
            
            e=e.replaceAll("<", "&lt;");
            e=e.replaceAll(">", "&gt;");
            
            String cause = "";

		cause+="<table>";
		cause+="<tr><td colspan=2><b>El Oyente received an XMPP event:</b></td></tr>";
		cause+="<tr><td colspan=2></td></tr>";
		cause+="<tr><td><b>Event</b></td><td>" + e + "</td></tr>";
		if (null != filter) {
			cause+="<tr><td><b>Filter</b></td><td>" + filter + "</td></tr>";
		}
		if (null != vars) {
			for (Entry<String, String> var : vars.entrySet()) {
				cause+="<tr><td><b>Variable</b></td><td>" + 
					var.getKey() + "=" + 
					var.getValue() + "</td></tr>";
			}
		}
		cause+="</table>";
               
		return cause;
	}

	public EnvVars getEnvVars() {
		return vars;
	}

}

// vim: set tabstop=4 softtabstop=4 shiftwidth=4 noexpandtab :
