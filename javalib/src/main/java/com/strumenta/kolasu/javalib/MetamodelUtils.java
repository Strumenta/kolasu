package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.ASTNode;
import com.strumenta.kolasu.model.LionwebKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KClassesImplKt;
import org.lionweb.lioncore.java.metamodel.Concept;

public class MetamodelUtils {

    public static Concept getConcept(Class<? extends ASTNode> clazz) {
        return LionwebKt.getConcept(kotlin.jvm.JvmClassMappingKt.getKotlinClass(clazz));
    }
}
