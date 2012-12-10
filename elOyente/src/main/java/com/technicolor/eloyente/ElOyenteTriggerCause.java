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


/**
 * @author Frank Vanderhallen
 */

public final class ElOyenteTriggerCause extends Cause {

	private EnvVars vars;

	public ElOyenteTriggerCause(EnvVars vars) {
		this.vars = vars;
	}

	@Override
	public String getShortDescription() {
		return new String("El Oyente received an XMPP event");
	}

	public EnvVars getEnvVars() {
		return vars;
	}

}

// vim: set tabstop=4 softtabstop=4 shiftwidth=4 noexpandtab :
