package com.pushpole.sdk.util;

import android.os.PowerManager;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Created by exshinigami on 12/13/15.
 */


public class UtilityTest extends TestCase {
    //private Context context = Mockito.mock(Context.class);

    private PowerManager fakePowerManager = Mockito.mock(PowerManager.class);

    @Before
    public void setUp() throws Exception {
        Mockito.when(fakePowerManager.isScreenOn()).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {

    }


    @Test
    public void testDecrypter() throws Exception {
//        final String SENTRY_SERVER = "http://sentry.pushpole.com";
//        String dec = Utility.decrypter(Utility.SENTRY_SERVER_UIDs);

//        assertEquals(SENTRY_SERVER, dec);
        //assertEquals(1, 2);

    }


    @Test
    public void testCheckScreenOnLteAPI19() throws Exception {
//        Boolean result = null;
//        fakePowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//        Mockito.when(fakePowerManager.isScreenOn()).thenReturn(true);
//        result = fakePowerManager.isScreenOn();
//        assertNull(result);
//        assertFalse(result);
    }

    @Test
    public void testCheckScreenOnGteAPI20() throws Exception {

    }

    @Test
    public void testCancelAlarm() throws Exception {

    }
}