package com.sltpaya.webintercept;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.sltpaya.open.web.intercept.WebHandler;
import com.sltpaya.open.web.intercept.WebIntercept;
import com.sltpaya.open.web.intercept.WebInterceptResult;
import com.sltpaya.web.annoation.Intercept;
import com.sltpaya.web.annoation.PathMode;
import com.sltpaya.web.annoation.MatchMode;

import java.util.regex.Pattern;

@Intercept(
        path = {
                "https://www.google.com\\w+"
        },
        mode = PathMode.CLONE_ADD_PREFIX,
        prefix = "test",
        matchMode = MatchMode.REGEXP,
        priority = 2,
        regExpFlag = Pattern.LITERAL
)
public class EventCCHandler implements WebHandler {

    @NonNull
    @Override
    public WebInterceptResult handle(WebIntercept intercept, Uri uri) {
        return WebInterceptResult.normal();
    }

}
