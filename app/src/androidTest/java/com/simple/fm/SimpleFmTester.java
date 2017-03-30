package com.simple.fm;

import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.test.InstrumentationTestCase;
import android.test.InstrumentationTestRunner;

/**
 * Created by Alan on 16/7/21.
 */
public class SimpleFmTester extends InstrumentationTestCase{

    private UiDevice mDevice;
    @Override
    public void setUp() throws Exception {
        mDevice = UiDevice.getInstance(getInstrumentation());

        super.setUp();

    }

    public void testLauncher() throws Exception {
//        mDevice.pressHome();

//        mDevice.wait(1000);

//        mDevice.pressRecentApps();

//        mDevice.wait(1000);

        mDevice.pressHome();

        UiObject2 qingting = mDevice.findObject(By.text("蜻蜓FM"));
        qingting.click();

    }
}
