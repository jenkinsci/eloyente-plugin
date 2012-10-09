/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.technicolor.eloyente;

import com.technicolor.eloyente.ElOyente.DescriptorImpl;
import hudson.model.Project;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pardogonzalezj
 */
public class ElOyenteTest {
    
    public ElOyenteTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getActiveJob method, of class ElOyente.
     */
    @Test
    public void testGetActiveJob() {
        System.out.println("getActiveJob");
        ElOyente instance = null;
        boolean expResult = false;
        boolean result = instance.getActiveJob();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of start method, of class ElOyente.
     */
    @Test
    public void testStart() {
        System.out.println("start");
        Project project = null;
        boolean newInstance = false;
        ElOyente instance = null;
        instance.start(project, newInstance);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of run method, of class ElOyente.
     */
    @Test
    public void testRun() {
        System.out.println("run");
        ElOyente instance = null;
        instance.run();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of stop method, of class ElOyente.
     */
    @Test
    public void testStop() {
        System.out.println("stop");
        ElOyente instance = null;
        instance.stop();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getDescriptor method, of class ElOyente.
     */
    @Test
    public void testGetDescriptor() {
        System.out.println("getDescriptor");
        ElOyente instance = null;
        DescriptorImpl expResult = null;
        DescriptorImpl result = instance.getDescriptor();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}
