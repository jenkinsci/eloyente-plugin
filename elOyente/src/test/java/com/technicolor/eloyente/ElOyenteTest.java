package com.technicolor.eloyente;

import hudson.model.Project;
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

    public static ElOyente oyente = new ElOyente(true);
    public static ElOyente mockOyente = mock(ElOyente.class);
    public static Project mockProject = mock(Project.class, RETURNS_DEEP_STUBS);

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

        verify(mockOyente, atLeastOnce()).start(mockProject, true);
        verify(mockOyente, atLeastOnce()).start(mockProject, false);

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
        lst.add(mockPrj1);
        lst.add(mockPrj2);

        when(mockProject.getAllJobs()).thenReturn(lst);
        when(mockProject.getParent().getFullName()).thenReturn("fonske");

        oyente.start(mockProject, false);
        oyente.run();
        verify(mockPrj1).scheduleBuild(null);
        verify(mockPrj2).scheduleBuild(null);
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
