package com.sltpaya.open.web.processor;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class Utils {

    /**
     * 检查Element是否符合规范
     * 接口实现WebHandler
     * 是类
     *
     * @param logger Logger
     */
    public static boolean checkElement(Element element, Logger logger, Elements elementUtils, Types typeUtils) {
        boolean b = false;
        if (element.getKind() == ElementKind.CLASS) {
            TypeElement typeElement = (TypeElement) element;
            List<? extends TypeMirror> interfaces = typeElement.getInterfaces();
            for (TypeMirror anInterface : interfaces) {
                TypeElement webHandlerTypeElement = elementUtils.getTypeElement("com.sltpaya.open.web.intercept.WebHandler");
                boolean isSubtype = typeUtils.isSubtype(webHandlerTypeElement.asType(), anInterface);
                if (isSubtype) {
                    b = true;
                }
            }
            if (!b) {
                logger.error("The Annotation Intercept Class Must Implement WebHandler!!");
            }
        }
        return b;
    }

}
