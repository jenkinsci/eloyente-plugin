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
import hudson.model.Item;
import hudson.model.Project;
import hudson.model.listeners.ItemListener;

@Extension
public final class JobListening extends ItemListener {

    /**
     * Used when a job is renamed.
     * 
     * It will unsubscribe that job, delete it's connection to the server, 
     * remove it's listeners and clean the connections and listeners fields.
     * Then it will redo everything for the new job name.
     *
     */
//    @Override
//    public void onRenamed(Item item, String oldName, String newName) {
//
//        ElOyente instance = (ElOyente) ((AbstractProject) item).getTrigger(ElOyente.class);
//
//        if (instance != null) {
//            instance.stop();
//            instance.start((Project) item, false);
//        }
//    }

     /**
     * Used when a job is deleted.
     *
     * It will unsubscribe that job, delete it's connection to the server, 
     * remove it's listeners and clean the connections and listeners fields.
     */
    @Override
    public void onDeleted(Item item) {
        System.out.println("Job " + item.getName() + " borrado\n");

        ElOyente instance = (ElOyente) ((AbstractProject) item).getTrigger(ElOyente.class);
        if (instance != null) {
            instance.stop();
        }
    }
}
