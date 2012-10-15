package com.technicolor.eloyente;

import hudson.model.Project;
import hudson.triggers.Trigger;
import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author pardogonzalezj
 */
public class ElOyenteTest {

    public static ElOyente oyente;
    public static ElOyente mockOyente;
    public static Project mockProject;

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
        oyente = new ElOyente(true);
        mockOyente = mock(ElOyente.class);
        mockProject = mock(Project.class, RETURNS_DEEP_STUBS);
        when(mockProject.getParent().getFullName()).thenReturn("ElOyente");
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getActiveJob method, of class ElOyente.
     */
    @Test
    public void testGetActiveJob() {

        ElOyente notActiveJob = when(mockOyente.getActiveJob()).thenReturn(Boolean.FALSE).getMock();
        ElOyente ActiveJob = when(mockOyente.getActiveJob()).thenReturn(Boolean.TRUE).getMock();

        assertEquals(oyente.getActiveJob(), ActiveJob.getActiveJob());
        assertEquals(oyente.getActiveJob(), notActiveJob.getActiveJob());
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

        Project mockPrj0 = mock(Project.class);
        Project mockPrj1 = mock(Project.class);
        Project mockPrj2 = mock(Project.class);
        ArrayList lst = new ArrayList();

        when(mockProject.getAllJobs()).thenReturn(lst);

        oyente.start(mockProject, false);
        oyente.run();
        verify(mockPrj0,never()).scheduleBuild(null);
        verify(mockPrj1,never()).scheduleBuild(null);
        verify(mockPrj2,never()).scheduleBuild(null);

        lst.add(mockPrj0);
        
        oyente.start(mockProject, false);
        oyente.run();
        verify(mockPrj0,times(1)).scheduleBuild(null);
        verify(mockPrj1,never()).scheduleBuild(null);
        verify(mockPrj2,never()).scheduleBuild(null);
        
        lst.add(mockPrj1);
        
        oyente.start(mockProject, false);
        oyente.run();
        verify(mockPrj0,times(2)).scheduleBuild(null);
        verify(mockPrj1,times(1)).scheduleBuild(null);
        verify(mockPrj2,never()).scheduleBuild(null);

        lst.add(mockPrj2);

        oyente.start(mockProject, false);
        oyente.run();
        verify(mockPrj0,times(3)).scheduleBuild(null);
        verify(mockPrj1,times(2)).scheduleBuild(null);
        verify(mockPrj2,times(1)).scheduleBuild(null);
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
