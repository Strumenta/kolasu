package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.Node;
import com.strumenta.kolasu.transformation.NodeFactory;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;

public class ASTTransformer extends com.strumenta.kolasu.transformation.ASTTransformer {

    protected void registerNodeFactory(Class<?> source, Class<? extends Node> target) {
        registerNodeFactory(getKotlinClass(source), getKotlinClass(target));
    }

    protected <S, T extends Node> NodeFactory<S, T> registerNodeFactory(Class<S> source, Function1<S, T> function) {
        return registerNodeFactory(getKotlinClass(source), (s, t) -> function.invoke(s));
    }

    protected <S, T extends Node> NodeFactory<S, T> registerNodeFactory(Class<S> source, Function2<S, ? super com.strumenta.kolasu.transformation.ASTTransformer, T> function) {
        return registerNodeFactory(getKotlinClass(source), function);
    }

}
