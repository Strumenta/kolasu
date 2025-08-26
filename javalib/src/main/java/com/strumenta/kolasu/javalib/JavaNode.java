package com.strumenta.kolasu.javalib;

import com.strumenta.kolasu.model.*;
import kotlin.reflect.KCallable;
import kotlin.reflect.full.KAnnotatedElements;
import org.jetbrains.annotations.NotNull;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.strumenta.kolasu.model.ModelKt.getRESERVED_FEATURE_NAMES;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;

/**
 * A subclass of {@link Node} that uses Java's reflection to compute its feature set.
 * Kotlin's reflection does not work well with Java classes following the JavaBeans naming convention.
 */
public class JavaNode extends Node {
    @Override
    @Internal
    public @NotNull List<PropertyDescription> getDerivedProperties() {
        return getProperties().stream().filter(PropertyDescription::getDerived).collect(Collectors.toList());
    }

    @Override
    @Internal
    public @NotNull List<PropertyDescription> getOriginalProperties() {
        return getProperties().stream().filter(p -> !p.getDerived()).collect(Collectors.toList());
    }

    @Override
    @Internal
    public @NotNull List<PropertyDescription> getProperties() {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(getClass());
            return Arrays.stream(beanInfo.getPropertyDescriptors())
                    .filter(this::isPotentialFeature)
                    .map(this::getPropertyDescription)
                    .collect(Collectors.toList());
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isPotentialFeature(PropertyDescriptor p) {
        if (getRESERVED_FEATURE_NAMES().contains(p.getName())) {
            return false;
        }
        try {
            if (p.getReadMethod() == null || p.getReadMethod().equals(Object.class.getDeclaredMethod("getClass"))) {
                return false;
            }
        } catch (NoSuchMethodException e) {
            // Can't happen
        }
        KCallable<?> kCallable = getKotlinClass(Node.class).getMembers().stream()
                .filter(m -> m.getName().equals(p.getName())).findFirst().orElse(null);
        return kCallable == null || (
                KAnnotatedElements.findAnnotations(kCallable, getKotlinClass(Internal.class)).isEmpty() &&
                KAnnotatedElements.findAnnotations(kCallable, getKotlinClass(Link.class)).isEmpty());
    }

    @NotNull
    private PropertyDescription getPropertyDescription(PropertyDescriptor p) {
        String name = p.getName();
        boolean provideNodes = Node.class.isAssignableFrom(p.getPropertyType());
        Multiplicity multiplicity = Multiplicity.OPTIONAL;
        if (Collection.class.isAssignableFrom(p.getPropertyType())) {
            multiplicity = Multiplicity.MANY;
        } else if (p.getReadMethod().isAnnotationPresent(NotNull.class)) {
            multiplicity = Multiplicity.SINGULAR;
        }
        Object value;
        try {
            value = p.getReadMethod().invoke(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        PropertyType propertyType = provideNodes ? PropertyType.CONTAINMENT : PropertyType.ATTRIBUTE;
        if (ReferenceByName.class.isAssignableFrom(p.getPropertyType())) {
            propertyType = PropertyType.REFERENCE;
        }
        boolean derived =
                p.getReadMethod().isAnnotationPresent(Derived.class) ||
                        (p.getWriteMethod() != null && p.getWriteMethod().isAnnotationPresent(Derived.class));
        return new PropertyDescription(name, provideNodes, multiplicity, value, propertyType, derived);
    }
}
