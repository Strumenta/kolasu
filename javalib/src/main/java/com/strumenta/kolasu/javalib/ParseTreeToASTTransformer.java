package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.ASTNode;
import com.strumenta.kolasu.model.BaseASTNode;
import com.strumenta.kolasu.transformation.ASTTransformer;
import com.strumenta.kolasu.transformation.NodeFactory;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;

public class ParseTreeToASTTransformer extends com.strumenta.kolasu.mapping.ParseTreeToASTTransformer {

    protected void registerNodeFactory(Class<?> source, Class<? extends BaseASTNode> target) {
        registerNodeFactory(getKotlinClass(source), getKotlinClass(target));
    }

    protected <S, T extends BaseASTNode> NodeFactory<S, T> registerNodeFactory(Class<S> source, Function1<S, T> function) {
        return registerNodeFactory(getKotlinClass(source), (s, t) -> function.invoke(s));
    }

    protected <S, T extends BaseASTNode> NodeFactory<S, T> registerNodeFactory(Class<S> source, Function2<S, ? super ASTTransformer, T> function) {
        return registerNodeFactory(getKotlinClass(source), function);
    }

}
