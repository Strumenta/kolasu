package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.Node;
import com.strumenta.kolasu.transformation.NodeFactory;
import com.strumenta.kolasu.validation.Issue;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function4;
import kotlin.reflect.KClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;

public class ASTTransformer extends com.strumenta.kolasu.transformation.ASTTransformer {
    public ASTTransformer(@NotNull List<Issue> issues) {
        super(issues);
    }

    public ASTTransformer(@NotNull List<Issue> issues, boolean allowGenericNode) {
        super(issues, allowGenericNode);
    }

    public ASTTransformer(@NotNull List<Issue> issues, boolean allowGenericNode, boolean throwOnUnmappedNode) {
        super(issues, allowGenericNode, throwOnUnmappedNode);
    }

    public ASTTransformer(@NotNull List<Issue> issues, boolean allowGenericNode, boolean throwOnUnmappedNode, boolean faultTollerant) {
        super(issues, allowGenericNode, throwOnUnmappedNode, faultTollerant);
    }

    public ASTTransformer(@NotNull List<Issue> issues, boolean allowGenericNode, boolean throwOnUnmappedNode, boolean faultTollerant, @Nullable Function4<Object, ? super Node, ? super KClass<? extends Node>, ? super com.strumenta.kolasu.transformation.ASTTransformer, ? extends List<? extends Node>> defaultTransformation) {
        super(issues, allowGenericNode, throwOnUnmappedNode, faultTollerant, defaultTransformation);
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

    protected <S, T extends Node> NodeFactory<S, T> registerNodeFactory(Class<S> source, Function2<S, ? super com.strumenta.kolasu.transformation.ASTTransformer, T> function) {
        return registerNodeFactory(getKotlinClass(source), function);
    }

}
