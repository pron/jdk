/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang.runtime;

import java.lang.invoke.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class synthesizes {@link StringTemplate StringTemplates} based on
 * fragments and bootstrap method type. Usage is primarily from
 * {@link java.lang.runtime.TemplateRuntime}.
 *
 * @since 21
 *
 * Warning: This class is part of PreviewFeature.Feature.STRING_TEMPLATES.
 *          Do not rely on its availability.
 */
final class StringTemplateImplFactory {

    /**
     * Private constructor.
     */
    StringTemplateImplFactory() {
        throw new AssertionError("private constructor");
    }

    /*
     * {@link StringTemplateImpl} constructor MethodHandle.
     */
    private static final MethodHandle CONSTRUCTOR;

    /**
     * Nullable list constructing MethodHandle;
     */
    private static final MethodHandle TO_LIST;

    /**
     * Object to string, special casing {@link StringTemplate};
     */
    private static final MethodHandle OBJECT_TO_STRING;

    /**
     * {@link StringTemplate} to string using interpolation.
     */
    private static final MethodHandle TEMPLATE_TO_STRING;

    /*
     * Frequently used method types.
     */
    private static final MethodType MT_STRING_STIMPL =
            MethodType.methodType(String.class, StringTemplateImpl.class);
    private static final MethodType MT_LIST_STIMPL =
            MethodType.methodType(List.class, StringTemplateImpl.class);

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodType mt = MethodType.methodType(void.class, int.class, int.class, StringTemplateSharedData.class);
            CONSTRUCTOR = lookup.findConstructor(StringTemplateImpl.class, mt)
                    .asType(mt.changeReturnType(Carriers.CarrierObject.class));

            mt = MethodType.methodType(List.class, Object[].class);
            TO_LIST = lookup.findStatic(StringTemplateImplFactory.class, "toList", mt);

            mt = MethodType.methodType(String.class, Object.class);
            OBJECT_TO_STRING = lookup.findStatic(StringTemplateImplFactory.class, "objectToString", mt);

            mt = MethodType.methodType(String.class, StringTemplate.class);
            TEMPLATE_TO_STRING = lookup.findStatic(StringTemplateImplFactory.class, "templateToString", mt);
        } catch(ReflectiveOperationException ex) {
            throw new AssertionError("carrier static init fail", ex);
        }
    }

    /**
     * Create a new {@link StringTemplateImpl}.
     *
     * @param fragments  string template fragments
     * @param type       value types with a StringTemplate return
     *
     * @return {@link MethodHandle} that can construct a {@link StringTemplateImpl} with arguments
     * used as values.
     */
    static MethodHandle createStringTemplateImplMH(List<String> fragments, MethodType type) {
        Carriers.CarrierElements elements = Carriers.CarrierFactory.of(type);
        List<MethodHandle> components = elements.components();
        List<MethodHandle> getters = components.stream()
                .map(c -> c.asType(c.type().changeParameterType(0, StringTemplateImpl.class)))
                .toList();
        List<Class<?>> ptypes = new ArrayList<>();
        for (MethodHandle c : components) {
            ptypes.add(c.type().returnType());
        }
        int[] permute = new int[ptypes.size()];
        MethodType constructorMT = MethodType.methodType(StringTemplate.class, ptypes);

        MethodType valuesMT = MethodType.methodType(List.class, ptypes);
        MethodHandle valuesMH = TO_LIST.asCollector(Object[].class, getters.size()).asType(valuesMT);
        valuesMH = MethodHandles.filterArguments(valuesMH, 0, getters.toArray(MethodHandle[]::new));
        valuesMH = MethodHandles.permuteArguments(valuesMH, MT_LIST_STIMPL, permute);

        List<MethodHandle> filters = filterGetters(getters);
        ptypes.clear();
        for (MethodHandle f : filters) {
            ptypes.add(f.type().returnType());
        }

        MethodHandle joinMH;

        try {
            joinMH = StringConcatFactory.makeConcatWithTemplate(fragments, ptypes);
        } catch (StringConcatException ex) {
            throw new RuntimeException("constructing internal string template", ex);
        }
        joinMH = MethodHandles.filterArguments(joinMH, 0, filters.toArray(MethodHandle[]::new));
        joinMH = MethodHandles.permuteArguments(joinMH, MT_STRING_STIMPL, permute);

        StringTemplateSharedData sharedData = new StringTemplateSharedData(
                fragments, elements, type.parameterList(), valuesMH, joinMH);

        MethodHandle constructor = MethodHandles.insertArguments(CONSTRUCTOR, 0,
                elements.primitiveCount(), elements.objectCount(), sharedData);
        constructor = MethodHandles.foldArguments(elements.initializer(), 0, constructor);

        constructor = constructor.asType(constructorMT);

        return constructor;
    }

    /**
     * Interpolate nested {@link StringTemplate StringTemplates}.
     * @param getters {@link Carriers} component getters
     * @return getters filtered to translate {@link StringTemplate StringTemplates} to strings
     */
    private static List<MethodHandle> filterGetters(List<MethodHandle> getters) {
        List<MethodHandle> filters = new ArrayList<>();
        for (MethodHandle getter : getters) {
            Class<?> type = getter.type().returnType();
            if (type == StringTemplate.class) {
                getter = MethodHandles.filterArguments(TEMPLATE_TO_STRING, 0, getter);
            } else if (type == Object.class) {
                getter = MethodHandles.filterArguments(OBJECT_TO_STRING, 0, getter);
            }
            filters.add(getter);
        }
        return filters;
    }

    /**
     * Filter object for {@link StringTemplate} and convert to string, {@link String#valueOf(Object)} otherwise.
     * @param object object to filter
     * @return {@link StringTemplate} interpolation otherwise result of {@link String#valueOf(Object)}.
     */
    private static String objectToString(Object object) {
        if (object instanceof StringTemplate st) {
            return st.join();
        } else {
            return String.valueOf(object);
        }
    }

    /**
     * Filter {@link StringTemplate} to strings.
     * @param st {@link StringTemplate} to filter
     * @return {@link StringTemplate} interpolation otherwise "null"
     */
    private static String templateToString(StringTemplate st) {
        if (st != null) {
            return st.join();
        } else {
            return "null";
        }
    }

    /**
     * Generic {@link StringTemplate}.
     *
     * @param fragments  immutable list of string fragments from string template
     * @param values     immutable list of expression values
     */
    private record SimpleStringTemplate(List<String> fragments, List<Object> values)
            implements StringTemplate {
        @Override
        public String toString() {
            return StringTemplate.toString(this);
        }
    }

    /**
     * Returns a new StringTemplate composed from fragments and values.
     *
     * @param fragments array of string fragments
     * @param values    array of expression values
     *
     * @return StringTemplate composed from fragments and values
     */
    static StringTemplate newTrustedStringTemplate(String[] fragments, Object[] values) {
        return new SimpleStringTemplate(List.of(fragments), toList(values));
    }

    /**
     * Returns a new StringTemplate composed from fragments and values.
     *
     * @param fragments list of string fragments
     * @param values    array of expression values
     *
     * @return StringTemplate composed from fragments and values
     */
    static StringTemplate newTrustedStringTemplate(List<String> fragments, Object[] values) {
        return new SimpleStringTemplate(List.copyOf(fragments), toList(values));
    }

    /**
     * Returns a new StringTemplate composed from fragments and values.
     *
     * @param fragments list of string fragments
     * @param values    list of expression values
     *
     * @return StringTemplate composed from fragments and values
     */

    static StringTemplate newStringTemplate(List<String> fragments, List<?> values) {
        @SuppressWarnings("unchecked")
        List<Object> copy = (List<Object>)values.stream().toList();
        return new SimpleStringTemplate(List.copyOf(fragments), copy);
    }

    /**
     * Collect nullable elements from an array into a unmodifiable list.
     * Elements are guaranteed to be safe.
     *
     * @param elements  elements to place in list
     *
     * @return unmodifiable list.
     */
    private static List<Object> toList(Object[] elements) {
        return Arrays.stream(elements).toList();
    }

}
