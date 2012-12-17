/*
 * Copyright 2012 technicolor.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.technicolor.eloyente;
import hudson.EnvVars;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author fernandezdiazi
 */
public class ElOyenteTriggerCauseTest {
    
    @Test
    public void testEmpty() throws Exception
    {
        ElOyenteTriggerCause ej= new ElOyenteTriggerCause(null);
        assertEquals(null,ej.getEnvVars());
        assertEquals("El Oyente received an XMPP event",ej.getShortDescription());
    }
    
    @Test
    public void testNonEmpty() throws Exception
    {
        EnvVars env = new EnvVars();
        
        ElOyenteTriggerCause ej= new ElOyenteTriggerCause(env);
        assertEquals(env,ej.getEnvVars());
        assertEquals("El Oyente received an XMPP event",ej.getShortDescription());
    }
    
}
