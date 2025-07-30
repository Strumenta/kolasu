package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.Node;

import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;

public class ParseTreeToASTTransformer extends com.strumenta.kolasu.mapping.ParseTreeToASTTransformer {

    void registerNodeFactory(Class<?> source, Class<? extends Node> target) {
        registerNodeFactory(getKotlinClass(source), getKotlinClass(target));
    }

}
