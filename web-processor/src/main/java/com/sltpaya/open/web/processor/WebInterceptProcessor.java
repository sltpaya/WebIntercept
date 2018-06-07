package com.sltpaya.open.web.processor;

import com.google.auto.service.AutoService;
import com.sltpaya.web.annoation.Intercept;
import com.sltpaya.web.annoation.PathMode;
import com.sltpaya.web.annoation.MatchMode;
import com.sltpaya.web.annoation.Uri;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import static com.sltpaya.open.web.processor.Consts.GENERATE_CLASS_NAME;
import static com.sltpaya.open.web.processor.Consts.PACKAGE_OF_GENERATE_FILE;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

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
    private FieldSpec mapFieldSpec;
    private ClassName uriMatcherTypeName;
    private FieldSpec contextFieldSpec;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        filer = processingEnv.getFiler();
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();

        logger = new Logger(processingEnv.getMessager());
    }

    private Map<Integer, FieldSpec> uriMatcherMap = new HashMap<>();
    private List<RegExpItem> regExpPatternList = new ArrayList<>();
    private List<GenerateItem> generateItems = new ArrayList<>();
    private List<InterceptItem> interceptItems = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        interceptItems.clear();
        uriMatcherMap.clear();
        regExpPatternList.clear();
        generateItems.clear();

        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Intercept.class);

        for (Element typeElement : elements) {
            if (typeElement.getKind() == ElementKind.CLASS) {
                beforeHandAnnoation(typeElement);
            }
        }
        try { generate(); } catch (IOException ignored) { }
        return false;
    }

    private void generateUriMatcher() {
        uriMatcherTypeName = ClassName.get("android.content", "UriMatcher");
        for (InterceptItem interceptItem : interceptItems) {
            int priority = interceptItem.priority;
            if (priority >= 0) {
                if (!uriMatcherMap.containsKey(priority)) {
                    FieldSpec fieldSpec = FieldSpec
                            .builder(
                                    uriMatcherTypeName,
                                    Consts.FIELD_URI_MATCHER_NAME + priority,
                                    PRIVATE, STATIC
                            ).build();
                    uriMatcherMap.put(priority, fieldSpec);
                }
            }
        }
    }

    private ClassName patternTypeName = ClassName.get(Pattern.class);

//    private void generateRegExpPattern() {
//
//        for (InterceptItem interceptItem : interceptItems) {
//            int priority = interceptItem.priority;
//            if (priority >= 0) {
//                if (!regExpPatternList.containsKey(priority)) {
//                    FieldSpec fieldSpec = FieldSpec
//                            .builder(
//                                    patternTypeName,
//                                    Consts.FIELD_REGEXP_NAME + priority,
//                                    PRIVATE, STATIC
//                            ).build();
//                    regExpPatternList.put(priority, fieldSpec);
//                }
//            }
//        }
//    }

    private void generate() throws IOException {
        ////////////////////gen//////////////////////////////
        ClassName contextTypeName = ClassName.get("android.content", "Context");
        ClassName uriTypeName = ClassName.get("android.net", "Uri");
        ClassName webHandlerTypeName = ClassName.get("com.sltpaya.open.web.intercept", "WebHandler");
        ClassName resultTypeName = ClassName.get("com.sltpaya.open.web.intercept", "WebInterceptResult");

        generateUriMatcher();

        mapFieldSpec = FieldSpec
                .builder(
                        ParameterizedTypeName.get(ClassName.get(HashMap.class), ClassName.get(Integer.class), webHandlerTypeName),
                        Consts.FIELD_MAP_NAME,
                        PRIVATE
                ).build();
        contextFieldSpec = FieldSpec.builder(
                contextTypeName,
                "context", PRIVATE).build();
        MethodSpec.Builder methodInitMethodBuilder = generateMethodInitCode();

        ////////////////////add//////////////////////////////


        TypeSpec.Builder builder = TypeSpec
                .classBuilder(GENERATE_CLASS_NAME)
                .addModifiers(PUBLIC)
                .addSuperinterface(ClassName.get("com.sltpaya.open.web.intercept", "WebIntercept"));

        addInitializerBlock(builder);
        builder.addMethod(methodInitMethodBuilder.build());
        //构造函数
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addStatement("init()")
                .build();
        builder.addMethod(constructor);

        //WebHandler handler;
        //
        //handler = map.get(uriMatcher2.match(uri));
        //if (handler != null) {
        //    return handler.handle(this, uri);
        //}
        //
        //handler = map.get(uriMatcher0.match(uri));
        //if (handler != null) {
        //    return handler.handle(this, uri);
        //}
        //
        //return WebInterceptResult.normal();

        MethodSpec.Builder dispatchMethodBuilder = MethodSpec.methodBuilder("dispatch")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(contextTypeName, "context")
                .addParameter(uriTypeName, "uri")
                .returns(resultTypeName)
                .addStatement(
                        "this.$N = context",
                        contextFieldSpec
                )
                .addStatement(
                        "$T handler",
                        webHandlerTypeName
                );

        Set<Integer> integers = uriMatcherMap.keySet();
        ArrayList<Integer> keys = new ArrayList<>(integers);
        Collections.sort(keys);
        Collections.reverse(keys);

        for (Integer key : keys) {
            //dispatchMethodBuilder.addStatement("/////////// UriMatcher priority=$L start ///////////", key);
            FieldSpec uriMatcher = uriMatcherMap.get(key);
            dispatchMethodBuilder
                    .addStatement(
                        "handler = $N.get($N.match(uri))",
                        mapFieldSpec,
                        uriMatcher
                    )
                    .beginControlFlow("if (handler != null)")
                    .addStatement("return handler.handle(this, uri)")
                    .endControlFlow();
            //dispatchMethodBuilder.addStatement("/////////// UriMatcher priority=$L end ///////////", key);
        }


        Collections.reverse(regExpPatternList);
        //生成pattern代码
        for (RegExpItem regExpItem : regExpPatternList) {
            dispatchMethodBuilder
                    .addStatement(
                            "handler = $N.get($L)",
                            mapFieldSpec,
                            regExpItem.code
                    )
                    .beginControlFlow(
                            "if (handler != null && $N.matcher(uri.toString()).find())",
                            regExpItem.fieldSpec
                    )
                    .addStatement("return handler.handle(this, uri)")
                    .endControlFlow();
        }

        dispatchMethodBuilder
                .addStatement(
                        "return $T.normal()",
                        resultTypeName
                );

        //getContext
        MethodSpec getContextMethodSpec = MethodSpec.methodBuilder("getContext")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(contextTypeName)
                .addStatement(
                        "return context"
                ).build();

        builder
                .addMethod(dispatchMethodBuilder.build())
                .addMethod(getContextMethodSpec);


        // Generate Creator
        JavaFile.builder(PACKAGE_OF_GENERATE_FILE, builder.build())
                .build().writeTo(filer);


        logger.info(">>> Generated SUCCESS: " + GENERATE_CLASS_NAME + " <<<");
    }


    /**
     * 统一处理注解
     *
     * @param element element
     */
    private void beforeHandAnnoation(Element element) {
        Intercept annotation = element.getAnnotation(Intercept.class);
        InterceptItem interceptItem = new InterceptItem();
        interceptItem.priority = annotation.priority();
        interceptItem.mode = annotation.mode();
        interceptItem.priority = annotation.priority();
        interceptItem.uri = annotation.path();
        interceptItem.prefix = annotation.prefix();
        interceptItem.host = annotation.host();
        interceptItem.webHandler = ClassName.get((TypeElement) element);
        interceptItem.matchMode = annotation.matchMode();
        interceptItem.regExpFlag = annotation.regExpFlag();
        interceptItems.add(interceptItem);
    }


    /**
     * 生成init方法代码
     */
    private MethodSpec.Builder generateMethodInitCode() {
        for (InterceptItem interceptItem : interceptItems) {
            GenerateItem generateItem = new GenerateItem();
            generateItem.matchMode = interceptItem.matchMode;
            generateItem.regExpFlag = interceptItem.regExpFlag;
            generateItem.priority = interceptItem.priority;
            generateItem.webHandler = interceptItem.webHandler;
            for (String uri : interceptItem.uri) {

                String path = Uri.parse(uri).getPath();
                String authority = Uri.parse(uri).getAuthority();

                generateItem.addUriPart(authority, path, uri, authority);

                switch (interceptItem.mode) {
                    case CLONE_ADD_PREFIX:
                        if (interceptItem.prefix != null) {
                            for (String prefix : interceptItem.prefix) {
                                generateItem.addUriPart(prefix + authority, path, uri, authority);
                            }
                        }
                        break;
                    case CLONE_REPLACE_HOST:
                        if (interceptItem.host != null) {
                            for (String host : interceptItem.host) {
                                host = host.replaceAll("https://", "").replaceAll("http://", "");
                                generateItem.addUriPart(host, path, uri, authority);
                            }
                        }
                        break;
                    default:
                        //nothing
                }
            }

            generateItems.add(generateItem);
        }

        MethodSpec.Builder initMethodBuilder =
                MethodSpec.methodBuilder(Consts.METHOD_INIT).addModifiers(PRIVATE);

        Collections.sort(generateItems);

        for (GenerateItem item : generateItems) {
            String fieldName = "webHandler_" + StringUtils.getRandomStrWithFieldName();
            initMethodBuilder.addStatement(
                    "$T "+fieldName+" = new $T()",
                    item.webHandler,
                    item.webHandler
            );
            for (UriPart part : item.parts) {
                initMethodBuilder.addStatement(
                        "$N.put($L, $N)",
                        mapFieldSpec,
                        part.code,
                        fieldName
                );
            }

            if (item.matchMode == MatchMode.REGEXP) {
                continue;
            }

            for (UriPart part : item.parts) {
                initMethodBuilder.addStatement(
                        "$N.addURI($S, $S, $L)",
                        uriMatcherMap.get(item.priority),
                        part.authority,
                        part.path,
                        part.code
                );
            }
        }

        return initMethodBuilder;
    }

    /**
     * 添加成员变量初始化代码块
     */
    private void addInitializerBlock(TypeSpec.Builder builder) {
        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
        builder.addField(mapFieldSpec);
        //map变量
        codeBlockBuilder.addStatement(
                "$N = new $T<>()",
                mapFieldSpec,
                HashMap.class
        );
        //uriMatcher变量
        for (Map.Entry<Integer, FieldSpec> specEntry : uriMatcherMap.entrySet()) {
            FieldSpec value = specEntry.getValue();
            builder.addField(value);
            codeBlockBuilder.addStatement(
                    "$N = new $T(-1)",
                    value,
                    uriMatcherTypeName
            );
        }

        //pattern变量
        Collections.sort(generateItems);
        for (GenerateItem generateItem : generateItems) {
            if (generateItem.matchMode != MatchMode.REGEXP) {
                continue;
            }
            for (UriPart part : generateItem.parts) {
                FieldSpec fieldSpec = FieldSpec
                        .builder(
                                patternTypeName,
                                Consts.FIELD_REGEXP_NAME + StringUtils.getRandomStrWithFieldName(),
                                PRIVATE, STATIC
                        ).build();
                builder.addField(fieldSpec);
                RegExpItem regExpItem = new RegExpItem();
                regExpItem.generateItem = generateItem;
                regExpItem.fieldSpec = fieldSpec;
                regExpItem.code = part.code;
                regExpPatternList.add(regExpItem);

                if (generateItem.regExpFlag == 0) {
                    codeBlockBuilder.addStatement(
                            "$N = $T.compile($S)",
                            fieldSpec,
                            patternTypeName,
                            part.url
                    );
                } else {
                    codeBlockBuilder.addStatement(
                            "$N = $T.compile($S, $L)",
                            fieldSpec,
                            patternTypeName,
                            part.url,
                            generateItem.regExpFlag
                    );
                }
            }
        }
        //context变量
        builder.addField(contextFieldSpec);
        builder.addInitializerBlock(
                codeBlockBuilder.build()
        );
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
