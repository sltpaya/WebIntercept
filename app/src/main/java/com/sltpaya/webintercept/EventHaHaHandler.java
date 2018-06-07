package com.sltpaya.webintercept;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.sltpaya.open.web.intercept.WebHandler;
import com.sltpaya.open.web.intercept.WebIntercept;
import com.sltpaya.open.web.intercept.WebInterceptResult;
import com.sltpaya.web.annoation.Intercept;
import com.sltpaya.web.annoation.PathMode;

@Intercept(path = {
        "https://www.google.com/hashass?abc=*",
        "https://www.google.com/hashass1?abc=*"
}, mode = PathMode.CLONE_ADD_PREFIX, prefix = "test")
public class EventHaHaHandler implements WebHandler {

    @NonNull
    @Override
    public WebInterceptResult handle(WebIntercept intercept, Uri uri) {
        return WebInterceptResult.normal();
    }

}
