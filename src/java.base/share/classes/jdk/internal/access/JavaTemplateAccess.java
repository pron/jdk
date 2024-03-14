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

package jdk.internal.access;

import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.function.Supplier;

public interface JavaTemplateAccess {
//    /**
//     * Combine one or more {@link StringTemplate StringTemplates} to produce a combined {@link StringTemplate}.
//     * {@snippet :
//     * StringTemplate st = StringTemplate.combine("\{a}", "\{b}", "\{c}");
//     * assert st.join().equals("\{a}\{b}\{c}");
//     * }
//     *
//     * @param flatten  if true will flatten nested {@link StringTemplate StringTemplates} into the
//     *                 combination
//     * @param sts      zero or more {@link StringTemplate}
//     * @return combined {@link StringTemplate}
//     * @throws NullPointerException if sts is null or if any element of sts is null
//     */
//    StringTemplate combine(boolean flatten, StringTemplate... sts);

    /**
     * Return a list of the embedded expression types.
     *
     * @param st {@link StringTemplate} to query
     * @return list of the embedded expression types
     * @throws NullPointerException if st is null
     * @throws IllegalArgumentException if the {@link StringTemplate} is not a literal
     */
    List<Class<?>> getTypes(StringTemplate st);

    /**
     * Return the metadata associated with the owner, getting from the supplier if
     * the metadata is not available.
     *
     * @param st        target {@link StringTemplate}
     * @param owner     metadata owner
     * @param supplier  metadata supplier
     * @return metadata associated with the owner
     * @param <T> type of metadata
     * @throws NullPointerException if st, owner or supplier is null
     * @throws IllegalArgumentException if the {@link StringTemplate} is not a literal
     */
    <T> T getMetaData(StringTemplate st, Object owner, Supplier<T> supplier);

    /**
     * Bind the getters of this {@link StringTemplate StringTemplate's} values to the inputs of the
     * supplied  {@link MethodHandle}.
     *
     * @param st target {@link StringTemplate}
     * @param mh {@link MethodHandle} to bind to
     * @return bound {@link MethodHandle}
     * @throws NullPointerException     if the {@link StringTemplate} or {@link MethodHandle} is null
     * @throws IllegalArgumentException if the {@link StringTemplate} is not a literal or
     *                                  if the types of the {@link MethodHandle} don't align with
     *                                  the getters of the {@link StringTemplate}.
     */
    MethodHandle bindTo(StringTemplate st, MethodHandle mh);

}

