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

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.*;

final class CheckedExceptions {
    private CheckedExceptions() {}

    private static class WrappedException extends RuntimeException {
        @java.io.Serial
        static final long serialVersionUID = 1L;

        public WrappedException(Throwable cause) {
            super(cause);
        }
    }

    public static <X extends Throwable> X rethrowUncheckedException(X x) {
        if (x instanceof Error er) throw er;
        if (x instanceof RuntimeException rex) throw rex;
        return x;
    }

    public static RuntimeException wrap(Exception ex) {
        if (ex instanceof RuntimeException re)
            return re;
        return new WrappedException(ex);
    }

    @SuppressWarnings("unchecked")
    public static <throws X> X unwrap(RuntimeException ex) {
        if (ex instanceof WrappedException) {
            return (X)ex.getCause();
        }
        throw ex;
    }

    @SuppressWarnings({"unchecked", "overloads"})
    public static <T, U> BiConsumer<T, U> eraseException(BiConsumer<T, U, ?> x) { return (BiConsumer<T, U>)x; }

    @SuppressWarnings({"unchecked", "overloads"})
    public static <T, U, R> BiFunction<T, U, R> eraseException(BiFunction<T, U, R, ?> x) { return (BiFunction<T, U, R>)x; }

    @SuppressWarnings({"unchecked", "overloads"})
    public static <T> BinaryOperator<T> eraseException(BinaryOperator<T, ?> x) { return (BinaryOperator<T>)x; }

    @SuppressWarnings({"unchecked", "overloads"})
    public static <T, U> BiPredicate<T, U> eraseException(BiPredicate<T, U, ?> x) { return (BiPredicate<T, U>)x; }

    @SuppressWarnings({"unchecked", "overloads"})
    public static <T> Consumer<T> eraseException(Consumer<T, ?> x) { return (Consumer<T>)x; }

    @SuppressWarnings({"unchecked", "overloads"})
    public static <T, R> Function<T, R> eraseException(Function<T, R, ?> x) { return (Function<T, R>)x; }

    @SuppressWarnings({"unchecked", "overloads"})
    public static <T> Predicate<T> eraseException(Predicate<T, ?> x) { return (Predicate<T>)x; }

    @SuppressWarnings({"unchecked", "overloads"})
    public static <T> Supplier<T> eraseException(Supplier<T, ?> x) { return (Supplier<T>)x; }

    @SuppressWarnings({"unchecked", "overloads"})
    public static <T> UnaryOperator<T> eraseException(UnaryOperator<T, ?> x) { return (UnaryOperator<T>)x; }

    @SuppressWarnings({"unchecked", "overloads"})
    public static <T> Iterator<T> eraseException(Iterator<T, ?> x) { return (Iterator<T>)x; }

    @SuppressWarnings({"unchecked", "overloads"})
    public static <T> Spliterator<T> eraseException(Spliterator<T, ?> x) { return (Spliterator<T>)x; }
}
