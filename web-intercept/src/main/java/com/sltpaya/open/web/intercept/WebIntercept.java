package com.sltpaya.open.web.intercept;

import android.content.Context;
import android.net.Uri;

public interface WebIntercept {

    WebInterceptResult dispatch(Context context, Uri uri);

    Context getContext();

}
