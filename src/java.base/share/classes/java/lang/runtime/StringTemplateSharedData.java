/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.vm.annotation.Stable;

import java.lang.invoke.MethodHandle;
import java.util.function.Supplier;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * StringTemplate shared data.
 */
final class StringTemplateSharedData {
    /**
     * List of string fragments for the string template. This value of this list is shared by
     * all instances created at the {@link java.lang.invoke.CallSite CallSite}.
     */
    private final List<String> fragments;

    /**
     * Carrier elements.
     */
    private final Carriers.CarrierElements elements;

    /**
     * List of input argument types.
     */
    private final List<Class<?>> types;

    /**
     * Specialized {@link MethodHandle} used to implement the {@link StringTemplate StringTemplate's}
     * {@code values} method. This {@link MethodHandle} is shared by all instances created at the
     * {@link java.lang.invoke.CallSite CallSite}.
     */
    private final MethodHandle valuesMH;

    /**
     * Specialized {@link MethodHandle} used to implement the {@link StringTemplate StringTemplate's}
     * {@code interpolate} method. This {@link MethodHandle} is shared by all instances created at the
     * {@link java.lang.invoke.CallSite CallSite}.
     */
    private final MethodHandle interpolateMH;

    /**
     * Owner of metadata.
     */
    private final AtomicReference<Object> owner;

    /**
     *  Value of metadata.
     */
    @Stable
    private Object metaData;

    /**
     * Constructor.
     * @param fragments       list of string fragments (bound in (bound at callsite)
     * @param elements        carrier elements
     * @param types;          list of value types
     * @param valuesMH        {@link MethodHandle} to produce list of values (bound at callsite)
     * @param interpolateMH   {@link MethodHandle} to produce interpolation (bound at callsite)
     */
    StringTemplateSharedData(List<String> fragments, Carriers.CarrierElements elements, List<Class<?>> types,
                             MethodHandle valuesMH, MethodHandle interpolateMH) {
        this.fragments = fragments;
        this.elements = elements;
        this.types = types;
        this.valuesMH = valuesMH;
        this.interpolateMH = interpolateMH;
        this.owner = new AtomicReference<>(null);
        this.metaData = null;

    }

    /**
     * {@return list of string fragments}
     */
    List<String> fragments() {
        return fragments;
    }

    /**
     * {@return return carrrier elements}
     */
    Carriers.CarrierElements elements() {
        return elements;
    }

    /**
     * {@return list of input argument types}
     */
    List<Class<?>> types() {
        return types;
    }

    /**
     * {@return MethodHandle to return list of values}
     */
    MethodHandle valuesMH() {
        return valuesMH;
    }

    /**
     * {@return MethodHandle to return string interpolation }
     */
    MethodHandle interpolateMH() {
        return interpolateMH;
    }


    /**
     * Get processor meta data.
     *
     * @param owner     owner object
     * @param supplier  supplier of meta data
     * @return meta data
     *
     * @param <S> type of owner
     * @param <T> type of meta data
     */
    @SuppressWarnings("unchecked")
    <S, T> T getMetaData(S owner, Supplier<T> supplier) {
        boolean isOwner = this.owner.get() == owner;
        Object temp = isOwner && metaData != null ? metaData : supplier.get();
        if (!isOwner && this.owner.compareAndExchange(null, owner) == null) {
            metaData = temp;
        }
        return (T)temp;
    }

}
