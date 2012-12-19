/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.technicolor.eloyente;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;

/////////////////////////////////////////////////////////
////////////////////////////////////////////////////////
// WARNING DO NOT TOUCH 
/////////////////////////////////////////////////////////
////////////////////////////////////////////////////////

@Extension
public final class SaveableListenerImpl extends SaveableListener {

    @Override
    public void onChange(final Saveable o, final XmlFile file) {
       System.out.println("Changed " + file + "\n");
    }
}
