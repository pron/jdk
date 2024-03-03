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

package java.lang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.function.Function;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import jdk.internal.access.JavaTemplateAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.javac.PreviewFeature;

/**
 * {@link StringTemplate} is the run-time representation of a string template or
 * text block template in a template expression.
 * <p>
 * In the source code of a Java program, a string template or text block template
 * contains an interleaved succession of <em>fragment literals</em> and <em>embedded
 * expressions</em>. The {@link StringTemplate#fragments()} method returns the
 * fragment literals, and the {@link StringTemplate#values()} method returns the
 * results of evaluating the embedded expressions. {@link StringTemplate} does not
 * provide access to the source code of the embedded expressions themselves; it is
 * not a compile-time representation of a string template or text block template.
 * <p>
 * {@link StringTemplate} is primarily used in conjunction with APIs
 * to produce a string or other meaningful value. Evaluation of a template expression
 * produces an instance of {@link StringTemplate}, with fragements and the values
 * of embedded expressions evaluated from left to right.
 * <p>
 * For example, the following code contains a template expression, which simply yields
 * a {@link StringTemplate}:
 * {@snippet :
 * int x = 10;
 * int y = 20;
 * StringTemplate st = "\{x} + \{y} = \{x + y}";
 * List<String> fragments = st.fragments();
 * List<Object> values = st.values();
 * }
 * {@code fragments} will be equivalent to {@code List.of("", " + ", " = ", "")},
 * which includes the empty first and last fragments. {@code values} will be the
 * equivalent of {@code List.of(10, 20, 30)}.
 * <p>
 * The following code contains a template expression with the same template but converting
 * to a string using the {@link #join()} method:
 * {@snippet :
 * int x = 10;
 * int y = 20;
 * String s = "\{x} + \{y} = \{x + y}".join();
 * }
 * When the template expression is evaluated, an instance of {@link StringTemplate} is
 * produced that returns the same lists from {@link StringTemplate#fragments()} and
 * {@link StringTemplate#values()} as shown above. The {@link #join()} method
 * uses these lists to yield an interpolated string. The value of {@code s} will
 * be equivalent to {@code "10 + 20 = 30"}.
 * <p>
 * The {@code join(List, List)} method provides a direct way to perform string interpolation
 * of fragments and values.
 * {@snippet :
 * List<String> fragments = st.fragments();
 * List<Object> values    = st.values();
 * ... check or manipulate the fragments and/or values ...
 * String result = StringTemplate.join(fragments, values);
 * }
 * The factory methods {@link StringTemplate#of(String)} and
 * {@link StringTemplate#of(List, List)} can be used to construct a {@link StringTemplate}.
 *
 * @implNote Implementations of {@link StringTemplate} must minimally implement the
 * methods {@link StringTemplate#fragments()} and {@link StringTemplate#values()}.
 * Instances of {@link StringTemplate} are considered immutable. To preserve the
 * semantics of string templates and text block templates, the list returned by
 * {@link StringTemplate#fragments()} must be one element larger than the list returned
 * by {@link StringTemplate#values()}.
 *
 * @since 21
 *
 * @jls 15.8.6 Process Template Expressions
 */
@PreviewFeature(feature=PreviewFeature.Feature.STRING_TEMPLATES)
public interface StringTemplate {
    /**
     * Returns a list of fragment literals for this {@link StringTemplate}.
     * The fragment literals are the character sequences preceding each of the embedded
     * expressions in source code, plus the character sequence following the last
     * embedded expression. Such character sequences may be zero-length if an embedded
     * expression appears at the beginning or end of a template, or if two embedded
     * expressions are directly adjacent in a template.
     * In the example: {@snippet :
     * String student = "Mary";
     * String teacher = "Johnson";
     * StringTemplate st = "The student \{student} is in \{teacher}'s classroom.";
     * List<String> fragments = st.fragments(); // @highlight substring="fragments()"
     * }
     * {@code fragments} will be equivalent to
     * {@code List.of("The student ", " is in ", "'s classroom.")}
     *
     * @return list of string fragments
     *
     * @implSpec the list returned is immutable
     */
    List<String> fragments();

    /**
     * Returns a list of embedded expression results for this {@link StringTemplate}.
     * In the example:
     * {@snippet :
     * String student = "Mary";
     * String teacher = "Johnson";
     * StringTemplate st = "The student \{student} is in \{teacher}'s classroom.";
     * List<Object> values = st.values(); // @highlight substring="values()"
     * }
     * {@code values} will be equivalent to {@code List.of(student, teacher)}
     *
     * @return list of expression values
     *
     * @implSpec the list returned is immutable
     */
    List<Object> values();

    /**
     * Returns the string interpolation of the fragments and values for this
     * {@link StringTemplate}.
     * {@snippet :
     * String student = "Mary";
     * String teacher = "Johnson";
     * StringTemplate st = "The student \{student} is in \{teacher}'s classroom.";
     * String result = st.join(); // @highlight substring="join()"
     * }
     * In the above example, the value of  {@code result} will be
     * {@code "The student Mary is in Johnson's classroom."}. This is
     * produced by the interleaving concatenation of fragments and values from the supplied
     * {@link StringTemplate}. To accommodate concatenation, values are converted to strings
     * as if invoking {@link String#valueOf(Object)}.
     *
     * @return interpolation of this {@link StringTemplate}
     *
     * @implSpec The default implementation returns the result of invoking
     * {@code StringTemplate.join(this.fragments(), this.values())}.
     */
    default String join() {
        return StringTemplate.join(fragments(), values());
    }


    /**
     * Produces a diagnostic string that describes the fragments and values of the supplied
     * {@link StringTemplate}.
     *
     * @param stringTemplate  the {@link StringTemplate} to represent
     *
     * @return diagnostic string representing the supplied string template
     *
     * @throws NullPointerException if stringTemplate is null
     */
    static String toString(StringTemplate stringTemplate) {
        Objects.requireNonNull(stringTemplate, "stringTemplate should not be null");
        return "StringTemplate{ fragments = [ \"" +
                String.join("\", \"", stringTemplate.fragments()) +
                "\" ], values = " +
                stringTemplate.values() +
                " }";
    }

    /**
     * Returns a {@link StringTemplate} as if constructed by invoking
     * {@code StringTemplate.of(List.of(string), List.of())}. That is, a {@link StringTemplate}
     * with one fragment and no values.
     *
     * @param string  single string fragment
     *
     * @return StringTemplate composed from string
     *
     * @throws NullPointerException if string is null
     */
    static StringTemplate of(String string) {
        Objects.requireNonNull(string, "string must not be null");
        JavaTemplateAccess JTA = SharedSecrets.getJavaTemplateAccess();
        return JTA.of(List.of(string), List.of());
    }

    /**
     * Returns a StringTemplate with the given fragments and values.
     *
     * @implSpec The {@code fragments} list size must be one more that the
     * {@code values} list size.
     *
     * @param fragments list of string fragments
     * @param values    list of expression values
     *
     * @return StringTemplate composed from string
     *
     * @throws IllegalArgumentException if fragments list size is not one more
     *         than values list size
     * @throws NullPointerException if fragments is null or values is null or if any fragment is null.
     *
     * @implNote Contents of both lists are copied to construct immutable lists.
     */
    static StringTemplate of(List<String> fragments, List<?> values) {
        Objects.requireNonNull(fragments, "fragments must not be null");
        Objects.requireNonNull(values, "values must not be null");
        if (values.size() + 1 != fragments.size()) {
            throw new IllegalArgumentException(
                    "fragments list size is not one more than values list size");
        }
        JavaTemplateAccess JTA = SharedSecrets.getJavaTemplateAccess();
        return JTA.of(fragments, values);
    }

    /**
     * Creates a string that interleaves the elements of values between the
     * elements of fragments. To accommodate interpolation, values are converted to strings
     * as if invoking {@link String#valueOf(Object)}.
     *
     * @param fragments  list of String fragments
     * @param values     list of expression values
     *
     * @return String interpolation of fragments and values
     *
     * @throws IllegalArgumentException if fragments list size is not one more
     *         than values list size
     * @throws NullPointerException fragments or values is null or if any of the fragments is null
     */
    static String join(List<String> fragments, List<?> values) {
        Objects.requireNonNull(fragments, "fragments must not be null");
        Objects.requireNonNull(values, "values must not be null");
        int fragmentsSize = fragments.size();
        int valuesSize = values.size();
        if (fragmentsSize != valuesSize + 1) {
            throw new IllegalArgumentException("fragments must have one more element than values");
        }
        JavaTemplateAccess JTA = SharedSecrets.getJavaTemplateAccess();
        return JTA.join(fragments, values);
    }

    /**
     * Combine zero or more {@link StringTemplate StringTemplates} into a single
     * {@link StringTemplate}.
     * {@snippet :
     * StringTemplate st = StringTemplate.combine("\{a}", "\{b}", "\{c}");
     * assert st.join().equals("\{a}\{b}\{c}".join());
     * }
     * Fragment lists from the {@link StringTemplate StringTemplates} are combined end to
     * end with the last fragment from each {@link StringTemplate} concatenated with the
     * first fragment of the next. To demonstrate, if we were to take two strings and we
     * combined them as follows: {@snippet lang = "java":
     * String s1 = "abc";
     * String s2 = "xyz";
     * String sc = s1 + s2;
     * assert Objects.equals(sc, "abcxyz");
     * }
     * the last character {@code "c"} from the first string is juxtaposed with the first
     * character {@code "x"} of the second string. The same would be true of combining
     * {@link StringTemplate StringTemplates}.
     * {@snippet lang ="java":
     * StringTemplate st1 = "a\{""}b\{""}c";
     * StringTemplate st2 = "x\{""}y\{""}z";
     * StringTemplate st3 = "a\{""}b\{""}cx\{""}y\{""}z";
     * StringTemplate stc = StringTemplate.combine(st1, st2);
     *
     * assert Objects.equals(st1.fragments(), List.of("a", "b", "c"));
     * assert Objects.equals(st2.fragments(), List.of("x", "y", "z"));
     * assert Objects.equals(st3.fragments(), List.of("a", "b", "cx", "y", "z"));
     * assert Objects.equals(stc.fragments(), List.of("a", "b", "cx", "y", "z"));
     * }
     * Values lists are simply concatenated to produce a single values list.
     * The result is a well-formed {@link StringTemplate} with n+1 fragments and n values, where
     * n is the total of number of values across all the supplied
     * {@link StringTemplate StringTemplates}.
     *
     * @param stringTemplates  zero or more {@link StringTemplate}
     *
     * @return combined {@link StringTemplate}
     *
     * @throws NullPointerException if stringTemplates is null or if any of the
     * {@code stringTemplates} are null
     *
     * @implNote If zero {@link StringTemplate} arguments are provided then a
     * {@link StringTemplate} with an empty fragment and no values is returned, as if invoking
     * <code>StringTemplate.of("")</code> . If only one {@link StringTemplate} argument is provided
     * then it is returned unchanged.
     */
    static StringTemplate combine(StringTemplate... stringTemplates) {
        JavaTemplateAccess JTA = SharedSecrets.getJavaTemplateAccess();
        return JTA.combine(stringTemplates);
    }

    /**
     * Combine a list of {@link StringTemplate StringTemplates} into a single
     * {@link StringTemplate}.
     * {@snippet :
     * StringTemplate st = StringTemplate.combine(List.of("\{a}", "\{b}", "\{c}"));
     * assert st.join().equals("\{a}\{b}\{c}".join());
     * }
     * Fragment lists from the {@link StringTemplate StringTemplates} are combined end to
     * end with the last fragment from each {@link StringTemplate} concatenated with the
     * first fragment of the next. To demonstrate, if we were to take two strings and we
     * combined them as follows: {@snippet lang = "java":
     * String s1 = "abc";
     * String s2 = "xyz";
     * String sc = s1 + s2;
     * assert Objects.equals(sc, "abcxyz");
     * }
     * the last character {@code "c"} from the first string is juxtaposed with the first
     * character {@code "x"} of the second string. The same would be true of combining
     * {@link StringTemplate StringTemplates}.
     * {@snippet lang ="java":
     * StringTemplate st1 = "a\{""}b\{""}c";
     * StringTemplate st2 = "x\{""}y\{""}z";
     * StringTemplate st3 = "a\{""}b\{""}cx\{""}y\{""}z";
     * StringTemplate stc = StringTemplate.combine(List.of(st1, st2));
     *
     * assert Objects.equals(st1.fragments(), List.of("a", "b", "c"));
     * assert Objects.equals(st2.fragments(), List.of("x", "y", "z"));
     * assert Objects.equals(st3.fragments(), List.of("a", "b", "cx", "y", "z"));
     * assert Objects.equals(stc.fragments(), List.of("a", "b", "cx", "y", "z"));
     * }
     * Values lists are simply concatenated to produce a single values list.
     * The result is a well-formed {@link StringTemplate} with n+1 fragments and n values, where
     * n is the total of number of values across all the supplied
     * {@link StringTemplate StringTemplates}.
     *
     * @param stringTemplates  list of {@link StringTemplate}
     *
     * @return combined {@link StringTemplate}
     *
     * @throws NullPointerException if stringTemplates is null or if any of the
     * its elements are null
     *
     * @implNote If {@code stringTemplates.size() == 0} then a {@link StringTemplate} with
     * an empty fragment and no values is returned, as if invoking
     * <code>StringTemplate.of("")</code> . If {@code stringTemplates.size() == 1}
     * then the first element of the list is returned unchanged.
     */
    static StringTemplate combine(List<StringTemplate> stringTemplates) {
        JavaTemplateAccess JTA = SharedSecrets.getJavaTemplateAccess();
        return JTA.combine(stringTemplates.toArray(new StringTemplate[0]));
    }

}
