package com.sltpaya.open.annation;


import android.support.annotation.IntDef;

import com.sltpaya.open.web.intercept.WebInterceptResult;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
@IntDef(value = {
        WebInterceptResult.ACTION,
        WebInterceptResult.NO_ACTION,
        WebInterceptResult.NORMAL,
})
public @interface WebInterceptResultDef {
}
