package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.Node;
import com.strumenta.kolasu.model.Source;
import com.strumenta.kolasu.transformation.ASTTransformer;
import com.strumenta.kolasu.transformation.NodeFactory;
import com.strumenta.kolasu.validation.Issue;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;

public class ParseTreeToASTTransformer extends com.strumenta.kolasu.mapping.ParseTreeToASTTransformer {
    public ParseTreeToASTTransformer(@NotNull List<Issue> issues, boolean allowGenericNode, @Nullable Source source, boolean throwOnUnmappedNode) {
        super(issues, allowGenericNode, source, throwOnUnmappedNode);
    }

    public ParseTreeToASTTransformer(@NotNull List<Issue> issues, boolean allowGenericNode, @Nullable Source source) {
        super(issues, allowGenericNode, source);
    }

    public ParseTreeToASTTransformer(@NotNull List<Issue> issues, boolean allowGenericNode) {
        super(issues, allowGenericNode);
    }

    public ParseTreeToASTTransformer(@NotNull List<Issue> issues) {
        super(issues);
    }

    protected <S, T extends Node> @NotNull NodeFactory<S, T> registerNodeFactory(Class<S> source, Class<T> target) {
        return registerNodeFactory(source, target, target.getName());
    }

    protected <S, T extends Node> @NotNull NodeFactory<S, T> registerNodeFactory(
            Class<S> source, Class<T> target, String nodeType
    ) {
        return registerNodeFactory(getKotlinClass(source), getKotlinClass(target), nodeType);
    }

    protected <S, T extends Node> NodeFactory<S, T> registerNodeFactory(Class<S> source, Function1<S, T> function) {
        return registerNodeFactory(getKotlinClass(source), (s, t) -> function.invoke(s));
    }

    protected <S, T extends Node> NodeFactory<S, T> registerNodeFactory(Class<S> source, Function2<S, ? super ASTTransformer, T> function) {
        return registerNodeFactory(getKotlinClass(source), function);
    }

}
