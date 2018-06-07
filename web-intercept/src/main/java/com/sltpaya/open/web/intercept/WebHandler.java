package com.sltpaya.open.web.intercept;

import android.net.Uri;
import android.support.annotation.NonNull;

public interface WebHandler {

    @NonNull WebInterceptResult handle(WebIntercept intercept, Uri uri);

}
