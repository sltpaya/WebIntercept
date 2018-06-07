package com.sltpaya.webintercept;

import android.net.Uri;
import android.support.annotation.NonNull;
import com.sltpaya.open.web.intercept.WebHandler;
import com.sltpaya.open.web.intercept.WebIntercept;
import com.sltpaya.open.web.intercept.WebInterceptResult;
import com.sltpaya.web.annoation.Intercept;

@Intercept(path = {
        "https://www.google.com/test?abc=*",
        "https://www.google.com/test1?abc=*",
        "https://www.google.com/test2?abc=*",
        "https://www.google.com/test3?abc=*",
}, priority = 1)
public class EventWebHandler implements WebHandler {

    @NonNull
    @Override
    public WebInterceptResult handle(WebIntercept intercept, Uri uri) {
        return WebInterceptResult.normal();
    }

}
