package com.sltpaya.webintercept;

import android.content.Context;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.sltpaya.open.web.intercept.SkinInterceptImpl;
import com.sltpaya.open.web.intercept.SkinsInterceptImpl;
import com.sltpaya.open.web.intercept.WebInterceptResult;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Pattern;

import static junit.framework.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        //测试是否匹配
        SkinsInterceptImpl it = new SkinsInterceptImpl();
        WebInterceptResult dispatch = it.dispatch(appContext, Uri.parse("https://www.google.com123453222"));
        int type = dispatch.type();
        //assertEquals(WebInterceptResult.NO_ACTION, type);
        assertEquals(Skin.getInstance().getName(), "skin_iii");
    }
}
