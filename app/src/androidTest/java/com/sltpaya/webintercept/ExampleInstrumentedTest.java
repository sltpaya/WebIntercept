package com.sltpaya.webintercept;

import android.content.Context;
import android.content.UriMatcher;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private Pattern pattern = Pattern.compile("https://m.baidu.com/\\w+\\?navstyle=\\w+&color=\\w+");

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        UriMatcher matcher = new UriMatcher(-1);
        matcher.addURI("m.baidu.com", "/abc", 5);

        Uri parse = Uri.parse("https://m.baidu.com/abc?navstyle=0&color=0x3455&rid=*");

        int match = matcher.match(parse);

        String navstyle = parse.getQueryParameter("navstyle");

        Log.i("测试", "navstyle: "+navstyle);
        Log.i("测试", "onCreate: "+match);

        Matcher matcher1 = pattern.matcher("https://m.baidu.com/abc?navstyle=0&color=0x3455&rid=*");
        if (matcher1.find()) {
            System.out.println("匹配上了");
        }

        assertEquals(4, match);
    }
}
