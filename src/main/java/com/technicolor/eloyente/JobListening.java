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

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Project;
import hudson.model.listeners.ItemListener;
import java.util.Iterator;
import jenkins.model.Jenkins;

@Extension
public final class JobListening extends ItemListener {

    @Override
    public void onCreated(Item item) {
    }

    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        System.out.println("Job " + item.getName() + " renombrado\n");
    }

    @Override
    public void onDeleted(Item item) {
        System.out.println("Job " + item.getName() + " borrado\n");
    }

    @Override
    public void onUpdated(Item item) {
        System.out.println("Job " + item.getName() + " actualizado\n");


    }

    @Override
    public void onLoaded() {

        /////////////////////////////////////////////////////////
////////////////////////////////////////////////////////
// WARNING DO NOT TOUCH 
/////////////////////////////////////////////////////////
////////////////////////////////////////////////////////

//        System.out.println("Jobs iniciados\n");
//
//        String server;
//        String user;
//        String password;
//
//        HandleXMPP handler = new HandleXMPP();
//
//        for (Item job : Jenkins.getInstance().getAllItems()) {
//            ElOyente instance = (ElOyente) ((AbstractProject) job).getTrigger(ElOyente.class);
//            if (instance != null) {
//                server = instance.getDescriptor().getServer();
//                user = instance.getDescriptor().getUser();
//                password = instance.getDescriptor().getPassword();
//                if (!handler.checkAnyParameterEmpty(server, user, password)) {
//                    if (handler.connectionOK(server, user, password)) {
//                        handler.createConnection((Project) job, server, user, password);
//                        instance.subscribeIfNecessary((Project) job);
//                    }
//                }
//            }
//        }
    }
}
