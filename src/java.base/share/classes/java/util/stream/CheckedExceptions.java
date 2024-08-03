/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package java.util.stream;

import java.util.Spliterator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class CheckedExceptions {
    private CheckedExceptions() {}

    private static class WrappedException extends RuntimeException {
        @java.io.Serial
        static final long serialVersionUID = 1L;

        public WrappedException(Throwable cause) {
            super(cause);
        }
    }

    public static RuntimeException wrap(Exception ex) {
        if (ex instanceof RuntimeException re)
            return re;
        return new WrappedException(ex);
    }

    public static <X extends Exception> X unwrap(RuntimeException ex) {
        if (ex instanceof WrappedException) {
            @SuppressWarnings("unchecked")
            var unwrapped = (X)ex.getCause();
            return unwrapped;
        }
        throw ex;
    }


    public static <T, X extends Exception> T unwrap(Supplier<T> s) throws X {
        try {
            return s.get();
        } catch (WrappedException ex) {
            @SuppressWarnings("unchecked")
            var unwrapped = (X)ex.getCause();
            throw unwrapped;
        }
    }

    public static <X extends Exception> void unwrap(Runnable r) throws X {
        try {
            r.run();
        } catch (WrappedException ex) {
            @SuppressWarnings("unchecked")
            var unwrapped = (X)ex.getCause();
            throw unwrapped;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, X extends Exception> Spliterator<T> wrap(Spliterator<T, X> s) {
        return (Spliterator<T>)(Object)new StreamSpliterators.DelegatingSpliterator<T, X, Spliterator<T, X>>(() -> s) {
            @Override
            public boolean tryAdvance(Consumer<? super T> consumer) {
                try {
                    return get().tryAdvance(consumer);
                } catch (Exception ex) {
                    throw CheckedExceptions.wrap(ex);
                }
            }

            @Override
            public void forEachRemaining(Consumer<? super T> consumer) {
                try {
                    get().forEachRemaining(consumer);
                } catch (Exception ex) {
                    throw CheckedExceptions.wrap(ex);
                }
            }
        };
    }

    @SuppressWarnings("overloads")
    public static <T> Consumer<T> wrap(Consumer<T, ?> c) {
        return t -> {
            try { c.accept(t); } catch (Exception ex) { throw wrap(ex); }
        };
    }

    @SuppressWarnings("overloads")
    public static <T> Supplier<T> wrap(Supplier<T, ?> s) {
        return () -> {
            try { return s.get(); } catch (Exception ex) { throw wrap(ex); }
        };
    }

    @SuppressWarnings("overloads")
    public static <T> Predicate<T> wrap(Predicate<T, ?> p) {
        return t -> {
            try { return p.test(t); } catch (Exception ex) { throw wrap(ex); }
        };
    }

    @SuppressWarnings("overloads")
    public static <T, R> Function<T, R> wrap(Function<T, R, ?> f) {
        return x -> {
            try { return f.apply(x); } catch (Exception ex) { throw wrap(ex); }
        };
    }

}
