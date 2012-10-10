/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.technicolor.eloyente;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.*;

/**
 *
 * @author pardogonzalezj
 */
public class ElOyenteTest {

    ElOyente mockedElOyente = mock(ElOyente.class);

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
    public void testGetActiveJob() throws IOException {
        when(mockedElOyente.getActiveJob()).thenReturn(Boolean.TRUE);
        when(mockedElOyente.getActiveJob()).thenReturn(Boolean.FALSE);
        when(mockedElOyente.getActiveJob()).thenThrow(new Exception());
    }

    /**
     * Test of start method, of class ElOyente.
     */
    @Test
    public void testStart() {
    }

    /**
     * Test of run method, of class ElOyente.
     */
    @Test
    public void testRun() {
    }

    /**
     * Test of stop method, of class ElOyente.
     */
    @Test
    public void testStop() {
    }

    /**
     * Test of getDescriptor method, of class ElOyente.
     */
    @Test
    public void testGetDescriptor() {
    }
}
