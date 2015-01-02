/*
 Copyright (c) 2014 Technicolor Delivery Technologies SAS

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
package com.technicolor.elboca;

import java.io.IOException;
import java.io.StringReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.Arrays;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.packet.*;
import org.jivesoftware.smackx.pubsub.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.kohsuke.stapler.DataBoundConstructor;

import org.kohsuke.stapler.bind.BoundObjectTable;
import org.kohsuke.stapler.bind.BoundObjectTable.Table;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import hudson.tasks.Builder;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildStepDescriptor;
import hudson.Extension;
import hudson.Launcher;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Environment;
import jenkins.model.Jenkins;
import hudson.model.Project;
import hudson.util.ListBoxModel;
import hudson.model.Descriptor;
import hudson.EnvVars;
/** 
 * This plug-in allows projects to send pubsub events
 * In combination with the eloyente plugin, it can start
 * builds on a different jenkins server.
 * 
 * @author Dennis Jacobs 
 */
public class ElBoca extends Builder {
 	private ConnectionConfiguration config;
	private Connection con;
	private PubSubManager mgr;
	private String jid;
	private final String node;
	private final String element;
	private final String payload;

	@DataBoundConstructor
	public ElBoca(String node, String element, String payload) {
		this.node = node;
		this.element = element;
		this.payload = payload;
	}

	/**
	 * This method will return the value entered as nodename
	 * in the project config.
	 * 
	 * @return String containing the nodename.
	 */
	public String getNode() {
		return node;
	}
	/**
	 * This method with return the value entered as the xml-element
	 * in the project config.
	 * 
	 * @return String containing the provided xml-element.
	 */
	public String getElement() {
		return element;
	}
	/**
	 * This method with return the value entered as the configured
	 * payload in the project config.
	 * 
	 * @return String containing the payload.
	 */
	public String getPayload() {
		return payload;
	}

	/**
	 * This method is invoked when a build should be started.
	 * For now it will just return a message and return true.
	 * @param build The build which should be started.
	 * @param launcher The launcher which should start the job.
	 * @param listener The listener which will output some messages.
	 * @return boolean True if build succeeded, false upon error.
	 */
	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener ) {
		// The line below should be uncommented in case there are some problems
		// with sending the payload.
		//XMPPConnection.DEBUG_ENABLED = true;
		PayloadItem<SimplePayload> item = null;
                PrintStream logger = listener.getLogger();
		Map<String,String> vars = new HashMap<String, String>();
		vars = build.getBuildVariables();
		logger.println("Connecting to xmppserver " + getDescriptor().getServer() + ".");
		// Connect to the xmpp-server
		getDescriptor().connectXMPP(listener);
		// check if the node is already known at xmpp-server side.
		if ( node != null && !node.isEmpty())
	 	  try {
		       checkAndAdd(node);
		  } catch ( XMPPException e) {
			logger.println("Failed adding node \"" + getNode() + "\".");
		  }
                // Create the actual message.
		logger.println("Creating new event.");
                try {
                    item = newMessage(listener, getElement(), getPayload(), vars);
                } catch (ParserConfigurationException e){
                    logger.println("Parser error upon creating new event.");
                } catch( IOException e){
                    logger.println("IO Error upon creating new event.");
                } catch (SAXException e){
                    logger.println("Failed creating new event.");
                } catch (XMPPException e){
                    logger.println("Failed creating new event.");
                }
		// if the message creation fails, exit now.
                if ( item == null) {
		  return false;
		}
		// Try to send the message.
                try {
		  send(node, item, listener);
		} catch (XMPPException e){
		// return false on error.
		  logger.println("Failed sending event.");
		  return false;
                }
                logger.println("Succesfuly sended event.");
	 	return true;    	
	}
	/**
	 * Checks if all the parameters from the main configuration are filled in.
	 *
	 * @param server Server from the main configuration.
	 * @param user User from the main configuration.
	 * @param password Password from the main configuration.
	 * @return boolean True if all is ok, false upon error.
	 */
	public static boolean checkAllParametersDefined(String server, String user, String password){
                boolean result = true;
		if (server == null && server.isEmpty()){
                    result = false;
                }
                if (user == null && user.isEmpty()){
                    result = false;
                }
                if (password == null && password.isEmpty()){
		    result =  false;
		}
		return result;
	}
	/**
	 * This method checks if the node is already known in the xmpp server.
	 * If not this function will add the specified node.
	 * @param nodename the nodename which should exist or be created.
	 */
	public void checkAndAdd(String nodename) throws XMPPException {
       		if ( nodename != null && nodename.length() != 0 ) {
		  DiscoverItems discoverNodes = getDescriptor().mgr.discoverNodes(null); //get all nodes
	    	  Iterator<DiscoverItems.Item> items = discoverNodes.getItems();
	    	  Map<String, DiscoverItems.Item> nodes = new HashMap<String, DiscoverItems.Item>();
	    	  while (items.hasNext()) {
	            DiscoverItems.Item item = items.next();
	            nodes.put(item.getNode(), item);
	          }
	          if ( !nodes.containsKey(nodename)) {
	            ConfigureForm form = new ConfigureForm(FormType.submit);
	            form.setAccessModel(AccessModel.open);
	            form.setDeliverPayloads(true);
	            form.setNotifyRetract(true);
	            form.setSubscribe(true);
	            form.setPersistentItems(true);
	            form.setPublishModel(PublishModel.open);
	            getDescriptor().mgr.createNode(nodename, form);
	        }
              }
	}
	/** 
	 * The function below should take an xml object (as string? or xml)
	 * and check the syntax of the xml and return it as a PayloadItem.
	 * @param listener The logger for the jenkins console output.
	 * @param element The element name.
	 * @param xml_payload The payload in xml structure but in string format.
	 * @throws XMMPExceptioni when creatin a new stanza failed.
	 * @throws SAXException upon a parsing error in the xml_payload.
	 * @throws IOException IO error when parsing the xml_payload.
	 * @throws ParseConfigurationException Parse configuration error.
	 * @return PayloadItem<SimplePayload> the new stanza ready to be sent or null on error.
	 */
	public PayloadItem<SimplePayload> newMessage(BuildListener listener, String element, String xml_payload,Map<String,String> vars) throws XMPPException, SAXException, IOException, ParserConfigurationException{
                PrintStream logger = listener.getLogger();
		int i = 0;
		// replace vars.
		Set<String> keys = vars.keySet();
		Collection<String> values = vars.values();
		int var_count = vars.size();
		String tmp = xml_payload;
		i = 0;
		for (String key :keys){ 
			tmp = tmp.replaceAll(key, (String)values.toArray()[i]);
			i++;
		}
                int start, end;
		// find the root xml-tag
		while (tmp.charAt(i) != '<'){
			i++;
               	}
		start = i+1;
               	while ( tmp.charAt(i) != '>' ){
			i++;
		}
		end = i;
                String rootelem = tmp.substring(start, end);
		SimplePayload payload = new SimplePayload(element, rootelem, tmp);
		String payload_id = "Message_" + System.currentTimeMillis();
		PayloadItem<SimplePayload> payload_item = new PayloadItem<SimplePayload>(payload_id, payload);
		// Convert the given payload to a String-type.
		String payload_str = payload_item.toString();
		// Remove the jivesoftware prefix
		String payload_substr = payload_str.substring(54, (payload_str.length()-1));
		// remove the closing ']' from the string.
	  	logger.println("Payload with id \"" + payload_id + "\" accepted.");
		logger.println(tmp);
	  	return(payload_item); 
  	}

	/**
	 * This method will actually send the payload.
	 * @param nodename The nodename to which we should publish this event.
	 * @param item The actual event to be published.
	 * @throws XMPPException upon failure due to sending event.
	 */
	public void send(String nodename, PayloadItem<SimplePayload> item, BuildListener listener) throws XMPPException{
                PrintStream logger = listener.getLogger();
		listener.getLogger().println("Sending event as user \"" + getDescriptor().getUser() + "\" to node \"" + getNode() + "\" to server \"" + getDescriptor().getServer() + "\".");
		// get the node.
		LeafNode node = (LeafNode) getDescriptor().mgr.getNode(nodename);
		// Publish the item with payload
		node.send(item);
	}

	/**
	 * This method will log out out of the xmppserver.
	 */
	public void logout() {
		getDescriptor().con.disconnect();
	}

	// If your plugin doesn't really define any property on Descriptor,
	// you don't have to do this.
	@Override
	public DescriptorImpl getDescriptor() {
	    return (DescriptorImpl)super.getDescriptor();
	}

	/**
	 * Descriptor for {@link ElBoca}. Used as a singleton.
	 * The class is marked as public so that it can be accessed from views.
	 *
	 * <p>
	 * See <tt>src/main/resources/com/technicolor/elboca/ElBoca/*.jelly</tt>
	 * for the actual HTML fragment for the configuration screen.
	 */
	@Extension // This indicates to Jenkins that this is an implementation of an extension point.
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
	/**
	 * To persist global configuration information,
	 * simply store it in a field and call save().
	 *
	 * <p>
	 * If you don't want fields to be persisted, use <tt>transient</tt>.
	 */
	private String server;
	private String user;
	private String password;
	protected transient ConnectionConfiguration config;
	protected transient XMPPConnection con;
	protected transient PubSubManager mgr;
	
	
	/**
	 * In order to load the persisted global configuration, you have to 
	 * call load() in the constructor.
	 */
	public DescriptorImpl() {
		load();
	}
	
	/**
	 * Performs on-the-fly validation of the form field 'payload'.
	 *
	 * @param payload
	 *      This parameter receives the value that the user has typed.
	 * @return
	 * Indicates the outcome of the validation. This is sent to the browser.
	 * <p>
	 * Note that returning {@link FormValidation#error(String)} does not
	 * prevent the form from being saved. It just means that a message
	 * will be displayed to the user. 
	 */
	public FormValidation doCheckPayload(@QueryParameter String payload) throws IOException, ServletException, XMPPException, SAXException, ParserConfigurationException {
		if ( validXmlStruct( payload ) == false ){
		  return FormValidation.error("Invalid Payload provided.");
		}
		return FormValidation.ok("Payload Accepted.");
	}
	
	public boolean isApplicable(Class<? extends AbstractProject> aClass) {
	    // Indicates that this builder can be used with all kinds of project types 
	    return true;
	}
	
	/**
	 * This human readable name is used in the configuration screen.
	 */
	public String getDisplayName() {
	    return "PubSub Event";
	}
	/**
	 * Invoked when the global configuration page is submitted.
	 * When click on the "Save" button of the main configuration this method
	 * will be called. It will then stop all the jobs that are using
	 * ElBoca, get the new credentials, make them persistent and start all
	 * those jobs back with the new credentials specified.
	 * @param req
	 * @param formData
	 * @throws Descriptor.FormException
	 */
	@Override
	public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
	    // To persist global configuration information,
	    // set that to properties and call save().
	    server = formData.getString("server");
	    user = formData.getString("user");
	    password = formData.getString("password");
	    save();
	    return super.configure(req, formData);
	}
	
	/**
	 * This method returns the URL of the XMPP server.
	 * global.jelly calls this method to obtain the value of field server.
	 */
	public String getServer() {
	    return server;
	}
	/**
	 * Performs on-the-fly validation of the form field 'server'.
	 * This method checks if the connection to the XMPP server specified is
	 * available. It shows a notification describing the status of the
	 * server connection.
	 * @param server Server from the the main configuration.
	 */
	public FormValidation doCheckServer(@QueryParameter String server) {
	    try {
	        config = new ConnectionConfiguration(server);
	        Connection con = new XMPPConnection(config);
	        if (server.isEmpty()) {
	            return FormValidation.warningWithMarkup("No server specified");
	        }
	        con.connect();
	        if (con.isConnected()) {
	            con.disconnect();
	            return FormValidation.okWithMarkup("Connection available");
	        }
	        return FormValidation.errorWithMarkup("Couldn't connect");
	    } catch (XMPPException ex) {
	        return FormValidation.errorWithMarkup("Couldn't connect: " + ex.getMessage());
	    }
	}
	/**
	 * This method returns the username for the XMPP connection.
	 * global.jelly calls this method to obtain the value of field user.
	 */
	public String getUser() {
	    return user;
	}
	/**
	 * This method returns the password for the XMPP connection.
	 * global.jelly calls this method to obtain the value of field password.
	 */
	public String getPassword() {
	    return password;
	}
	/**
	 * Performs on-the-fly validation of the form fields 'user' and
	 * 'password'.
	 * This method checks if the user and password of the XMPP server
	 * specified are correct and valid. It shows a notification describing
	 * the status of the login.
	 * @param user User from the the main configuration.
	 * @param password Password from the the main configuration.
	 */
	public FormValidation doCheckPassword(@QueryParameter String user, @QueryParameter String password, @QueryParameter String server) {
	    config = new ConnectionConfiguration(server);
	    Connection con = new XMPPConnection(config);
	    if ((user.isEmpty() || password.isEmpty()) || server.isEmpty()) {
	        return FormValidation.warningWithMarkup("Not authenticated");
	    }
	    try {
	        con.connect();
	        con.login(user, password);
	        if (con.isAuthenticated()) {
	            con.disconnect();
	            return FormValidation.okWithMarkup("Authentication succeeded");
	        }
	        return FormValidation.warningWithMarkup("Not authenticated");
	    } catch (XMPPException ex) {
	        return FormValidation.errorWithMarkup("Authentication failed");
	    }
	}
	
	
	/**
	 * This method will connect to the xmpp server.
	 * @param server the servername
	 * @param user The user-name used to log in into the xmppserver.
	 * @param password The password used to log in into the xmppserver.
	 */
	private synchronized boolean connectXMPP(BuildListener listener) {
              PrintStream logger = listener.getLogger();
              if ( checkAllParametersDefined(getServer(), getUser(), getPassword()) == true) { 
	      	config = new ConnectionConfiguration(getServer());
	      	con = new XMPPConnection(config);
	        if (!con.isConnected()) {
	          try {
	            con.connect();
		    con.login(getUser(), getPassword());
	            mgr = new PubSubManager(con);
	            return true;
	          } catch (XMPPException ex) {
	            logger.println("Failed to connect");
	            return false;
	          }
	        } else {
	           logger.println("Already connected");
	           return true;
	        }
	      } else {
	         logger.println("Empty fields in main configuration!");
	         return false;
	      }
	}
        /**
         * This method will check the valid if the provided xml_payload
         * contains a valid XML structure.
         * @param xml_payload The payload which should be checked.
         * @throws XMPPException
         * @throws SAXException
         * @throws IOException
         * @throws ParserConfigurationException
         * @return boolean Yes if it is valid, no if the payload is invalid.
         */
        public boolean validXmlStruct(String xml_payload) throws  XMPPException, SAXException, IOException , ParserConfigurationException {
              try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setValidating(false);
                dbf.setNamespaceAware(true);
                DocumentBuilder db = dbf.newDocumentBuilder();
                InputSource is = new InputSource();
                is.setCharacterStream(new StringReader(xml_payload));
                db.parse(is);
              } catch ( SAXException e) {
                System.err.println("Invalid payload provided.");
                e.printStackTrace();
                return(false);
              } catch (IOException e){
                System.err.println("Problem with connectivity");
                e.printStackTrace();
                return(false);
              }
              return(true);
        }
   }
}
