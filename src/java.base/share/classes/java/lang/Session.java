/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.util.List;
import java.util.function.Supplier;

/**
 * TBD
 * @param <T> TBD
 */
public non-sealed interface Session<T> extends TryResource {
    /**
     * TBD
     * @return TBD
     * @throws Exception TBD
     */
    T open() throws Exception;

    /**
     * TBD
     * @throws Exception TBD
     */
    @Override
    void close() throws Exception;

    /**
     * TBD
     * @param sac TBD
     * @return TBD
     * @param <T> TBD
     */
    static <T extends AutoCloseable> Session<T> of(Supplier<T> sac) {
        return new AutoCloseableSession<>(sac);
    }

    /**
     * TBD
     * @param <T> TBD
     */
    final class AutoCloseableSession<T extends AutoCloseable> implements Session<T> {
        private final Supplier<T> sac;
        private T ac;

        private AutoCloseableSession(Supplier<T> sac) {
            this.sac = sac;
        }

        @Override
        public T open() {
            if (ac != null)
                throw new IllegalStateException("open already called");
            ac = sac.get();
            return ac;
        }

        @Override
        public void close() throws Exception {
            throw new AssertionError(); //
        }
    }

}
