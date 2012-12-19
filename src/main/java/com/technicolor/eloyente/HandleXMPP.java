/*
 * Copyright 2012 Technicolor.
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

import hudson.model.Project;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

public class HandleXMPP {

    private final static Map<String, Connection> connections = new HashMap<String, Connection>();

    /**
     * Checks if all the parameters from the main configuration are complete.
     *
     * @param server Server from the main configuration
     * @param user User from the main configuration
     * @param password Password from the main configuration
     */
    public boolean checkAnyParameterEmpty(String server, String user, String password) {
        if (server != null && !server.isEmpty() && user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Check if it is possible to connect to the XMPP server.
     *
     * Check if it is possible to connect to the XMPP server with the parameters
     * specified in the main configuration.
     *
     * @param server Server from the main configuration
     * @param user User from the main configuration
     * @param password Password from the main configuration
     */
    public boolean connectionOK(String server, String user, String password) {

        try {
            ConnectionConfiguration config = new ConnectionConfiguration(server);
            Connection con = new XMPPConnection(config);
            con.connect();
            con.login(user, password);
            con.disconnect();
            return true;
        } catch (XMPPException ex) {
            return false;
        }
    }

    /**
     *
     * @param project Project being started
     * @param server Server to which the plugin is connected
     * @param user User for the server
     * @param password Password for the server
     * @throws XMPPException
     */
    public Connection createConnection(Project project, String server, String user, String password) {
        if (!connections.containsKey(project.getName())) {
            ConnectionConfiguration config = new ConnectionConfiguration(server);
            Connection con = new XMPPConnection(config);
            try {
                con.connect();
                con.login(user, password, project.getName());
            } catch (XMPPException ex) {
                System.err.println(ex);
            }
            connections.put(project.getName(), con);
            return con;
        } else {
            return connections.get(project.getName());
        }
    }
}
