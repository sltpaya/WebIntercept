package com.sltpaya.open.web.processor;

import com.google.auto.service.AutoService;
import com.sltpaya.web.annoation.Intercept;
import com.sltpaya.web.annoation.InterceptGroup;
import com.sltpaya.web.annoation.MatchMode;
import com.sltpaya.web.annoation.PathMode;
import com.sltpaya.web.annoation.Uri;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SuppressWarnings("unused")
@SupportedAnnotationTypes({
        "com.sltpaya.web.annoation.Intercept"
})
public class WebInterceptProcessor extends AbstractProcessor {

    private Filer filer;
    private Types typeUtils;
    private Elements elementUtils;
    private Logger logger;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        filer = processingEnv.getFiler();
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        logger = new Logger(processingEnv.getMessager());
    }

    private Map<String, GroupItem> groups = new HashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        groups.clear();

        //默认的Group
        GroupItem groupItem = new GroupItem();
        groups.put(groupItem.name, groupItem);

        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Intercept.class);

        for (Element element : elements) {
            boolean isValid = Utils.checkElement(element, logger, elementUtils, typeUtils);
            InterceptGroup annotation = element.getAnnotation(InterceptGroup.class);
            if (isValid) {
                GroupItem group = groups.get(Consts.GENERATE_CLASS_NAME);
                if (annotation != null) {
                    String[] names = annotation.name();
                    for (String name : names) {
                        if (name != null && name.length() > 0 && name.trim().length() > 0) {
                            if (groups.containsKey(name)) {
                                group = groups.get(name);
                            } else {
                                group = new GroupItem(name);
                                groups.put(name, group);
                            }
                        }
                    }
                }
                group.addElement((TypeElement) element);
                logger.info("group: "+group.name);
            }
        }

        try {
            for (Map.Entry<String, GroupItem> entry : groups.entrySet()) {
                GroupItem value = entry.getValue();
                value.write(logger, filer);
            }
        } catch (IOException ignored) { }
        return false;
    }

    static class InterceptItem {

        int priority = 0;
        TypeName webHandler;
        PathMode mode;
        String[] uri;
        String[] host;
        String[] prefix;
        MatchMode matchMode;
        int regExpFlag = 0;

    }

    static class GenerateItem implements Comparable<GenerateItem> {

        int priority;
        TypeName webHandler;
        List<UriPart> parts = new ArrayList<>();
        MatchMode matchMode = MatchMode.URI_MATCHER;
        int regExpFlag = 0;
        int code;

        void addUriPart(String authority, String path, String url, String rawAuthority) {
            UriPart uriPart = new UriPart(authority, path);
            Uri parse = Uri.parse(url);

            url = url.replace(rawAuthority, authority);

            uriPart.url = url;
            parts.add(uriPart);
        }

        @Override
        public int compareTo(GenerateItem generateItem) {
            if (priority > generateItem.priority) {
                return 1;
            }
            return -1;
        }
    }

    static class UriPart {

        String url;
        String authority;
        String path;
        int code;

        UriPart(String authority, String path) {
            this.authority = authority;
            this.path = path;
            this.code = generateCode();
        }

        static int startCode = -1;

        static int generateCode() {
            return ++startCode;
        }

    }


    static class RegExpItem {

        GenerateItem generateItem;
        FieldSpec fieldSpec;
        int code;

    }


}
