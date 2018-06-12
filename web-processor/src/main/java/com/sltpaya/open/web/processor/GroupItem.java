package com.sltpaya.open.web.processor;

import com.sltpaya.web.annoation.Intercept;
import com.sltpaya.web.annoation.MatchMode;
import com.sltpaya.web.annoation.Uri;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.processing.Filer;
import javax.lang.model.element.TypeElement;

import static com.sltpaya.open.web.processor.Consts.PACKAGE_OF_GENERATE_FILE;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

/**
 * Group 表示组
 */
class GroupItem {

    private TypeSpec.Builder builder;

    GroupItem() {
        this(Consts.GENERATE_CLASS_NAME);
    }

    GroupItem(String name) {
        this.name = name;
    }

    public String name;

    private Map<Integer, FieldSpec> uriMatcherMap = new HashMap<>();
    private List<WebInterceptProcessor.RegExpItem> regExpPatternList = new ArrayList<>();
    private List<WebInterceptProcessor.GenerateItem> generateItems = new ArrayList<>();
    private List<WebInterceptProcessor.InterceptItem> interceptItems = new ArrayList<>();

    private Logger logger;
    private ClassName patternTypeName = ClassName.get(Pattern.class);
    private FieldSpec mapFieldSpec;
    private ClassName uriMatcherTypeName;

    private FieldSpec contextFieldSpec;

    private List<TypeElement> elements = new ArrayList<>();

    public void addElement(TypeElement element) {
        elements.add(element);
    }

    private void process() {
        interceptItems.clear();
        uriMatcherMap.clear();
        regExpPatternList.clear();
        generateItems.clear();
        for (TypeElement element : elements) {
            beforeHandAnnoation(element);
        }
    }

    public void write(Logger logger, Filer filer) throws IOException {
        this.logger = logger;
        process();
        generate();
        // Generate Creator
        JavaFile.builder(PACKAGE_OF_GENERATE_FILE, builder.build())
                .build().writeTo(filer);

        logger.info(">>> Generated SUCCESS: " + name + " <<<");
    }

    /**
     * 统一处理注解
     *
     * @param element element
     */
    private void beforeHandAnnoation(TypeElement element) {
        Intercept annotation = element.getAnnotation(Intercept.class);
        WebInterceptProcessor.InterceptItem interceptItem = new WebInterceptProcessor.InterceptItem();
        interceptItem.priority = annotation.priority();
        interceptItem.mode = annotation.mode();
        interceptItem.priority = annotation.priority();
        interceptItem.uri = annotation.path();
        interceptItem.prefix = annotation.prefix();
        interceptItem.host = annotation.host();
        interceptItem.webHandler = ClassName.get(element);
        interceptItem.matchMode = annotation.matchMode();
        interceptItem.regExpFlag = annotation.regExpFlag();
        interceptItems.add(interceptItem);
    }

    private void generate() {
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


        builder = TypeSpec
                .classBuilder(name)
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
        for (WebInterceptProcessor.RegExpItem regExpItem : regExpPatternList) {
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
    }

    private void generateUriMatcher() {
        uriMatcherTypeName = ClassName.get("android.content", "UriMatcher");
        for (WebInterceptProcessor.InterceptItem interceptItem : interceptItems) {
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

    /**
     * 生成init方法代码
     */
    private MethodSpec.Builder generateMethodInitCode() {
        for (WebInterceptProcessor.InterceptItem interceptItem : interceptItems) {
            WebInterceptProcessor.GenerateItem generateItem = new WebInterceptProcessor.GenerateItem();
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

        for (WebInterceptProcessor.GenerateItem item : generateItems) {
            String fieldName = "webHandler_" + StringUtils.getRandomStrWithFieldName();
            initMethodBuilder.addStatement(
                    "$T " + fieldName + " = new $T()",
                    item.webHandler,
                    item.webHandler
            );
            for (WebInterceptProcessor.UriPart part : item.parts) {
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

            for (WebInterceptProcessor.UriPart part : item.parts) {
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
        for (WebInterceptProcessor.GenerateItem generateItem : generateItems) {
            if (generateItem.matchMode != MatchMode.REGEXP) {
                continue;
            }
            for (WebInterceptProcessor.UriPart part : generateItem.parts) {
                FieldSpec fieldSpec = FieldSpec
                        .builder(
                                patternTypeName,
                                Consts.FIELD_REGEXP_NAME + StringUtils.getRandomStrWithFieldName(),
                                PRIVATE, STATIC
                        ).build();
                builder.addField(fieldSpec);
                WebInterceptProcessor.RegExpItem regExpItem = new WebInterceptProcessor.RegExpItem();
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

}
