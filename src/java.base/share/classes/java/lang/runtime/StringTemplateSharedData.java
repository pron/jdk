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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Supplier;
import java.util.List;

import jdk.internal.vm.annotation.Stable;

/**
 * StringTemplate shared data. Used to hold information for a {@link StringTemplate}
 * constructed at a specific {@link java.lang.invoke.CallSite CallSite}.
 */
final class StringTemplateSharedData {
    /**
     * owner field {@link VarHandle}.
     */
    private static final VarHandle OWNER_VH;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            OWNER_VH = lookup.findVarHandle(StringTemplateSharedData.class, "owner", Object.class);
        } catch (ReflectiveOperationException ex) {
            throw new InternalError(ex);
        }
    }


    /**
     * List of string fragments for the string template. This value of this list is shared by
     * all instances created at the {@link java.lang.invoke.CallSite CallSite}.
     */
    @Stable
    private final List<String> fragments;

    /**
     * Description of the {@link StringTemplate StringTemplate's} carrier elements.
     */
    @Stable
    private final Carriers.CarrierElements elements;

    /**
     * List of input argument types.
     */
    @Stable
    private final List<Class<?>> types;

    /**
     * Specialized {@link MethodHandle} used to implement the {@link StringTemplate StringTemplate's}
     * {@code values} method. This {@link MethodHandle} is shared by all instances created at the
     * {@link java.lang.invoke.CallSite CallSite}.
     */
    @Stable
    private final MethodHandle valuesMH;

    /**
     * Specialized {@link MethodHandle} used to implement the {@link StringTemplate StringTemplate's}
     * {@code join} method. This {@link MethodHandle} is shared by all instances created at the
     * {@link java.lang.invoke.CallSite CallSite}.
     */
    @Stable
    private final MethodHandle joinMH;

    /**
     * Owner of metadata. Metadata is used to cache information at a
     * {@link java.lang.invoke.CallSite CallSite} by a processor. Only one
     * cache is aavailable, first processor wins. This is under the assumption
     * that each {@link StringTemplate} serves one purpose. A processor should
     * have a fallback if it does not win the cache.
     */
    @Stable
    private Object owner;

    /**
     *  Metadata cache.
     */
    @Stable
    private Object metaData;

    /**
     * Constructor. Contents are bound to the {@link java.lang.invoke.CallSite CallSite}.
     * @param fragments       list of string fragments
     * @param elements        carrier elements
     * @param types;          list of value types
     * @param valuesMH        {@link MethodHandle} to produce list of values
     * @param joinMH          {@link MethodHandle} to produce interpolation
     */
    StringTemplateSharedData(List<String> fragments, Carriers.CarrierElements elements, List<Class<?>> types,
                             MethodHandle valuesMH, MethodHandle joinMH) {
        this.fragments = fragments;
        this.elements = elements;
        this.types = types;
        this.valuesMH = valuesMH;
        this.joinMH = joinMH;
        this.owner = null;
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
    MethodHandle joinMH() {
        return joinMH;
    }

    /**
     * Get owner meta data.
     *
     * @param owner     owner object, should be unique to the processor
     * @param supplier  supplier of meta data
     * @return meta data
     *
     * @param <S> type of owner
     * @param <T> type of meta data
     */
    @SuppressWarnings("unchecked")
    <S, T> T getMetaData(S owner, Supplier<T> supplier) {
        if (this.owner == null && (Object)OWNER_VH.compareAndExchange(this, null, owner) == null) {
            metaData = supplier.get();
        }
        return this.owner == owner ? (T)metaData : null;
    }

}
