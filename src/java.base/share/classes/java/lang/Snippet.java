package java.lang;

import jdk.internal.snippets.SnippetImpl;
import jdk.internal.vm.annotation.Stable;

import java.lang.annotation.Annotation;
import java.lang.invoke.*;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A templated string models an instance of a templated string literal in a Java program. Templated strings contain
 * <em>values</em>: the expressions embedded in the templated string literal. Moreover, templated strings are always
 * associated with an underlying {@linkplain Template template}. The template describes the static properties of the
 * templated string literal: such as the string fragments occurring in the templated string literal, as well as
 * the types (and annotations, where present) associated with the embedded expressions in the templated string literal.
 * @param <X> the templated string concrete type
 */
public sealed interface Snippet<X extends Snippet.Language<X>> permits SnippetImpl {

    /**
     * {@return the template associated with this templated string {@link Snippet }}
     */
    Template<X> template();

    /**
     * {@return a list of embedded expression results for this {@link Snippet }}
     * @implSpec the list returned is immutable
     */
    List<Object> values();

    /**
     * {@return the embedded expression result at given index for this {@link Snippet }}
     * @param i index
     */
    Object value(int i);

    /**
     * Produces a diagnostic string that describes the template and values of this
     * {@link Snippet}.
     *
     * @return diagnostic string representing this templated string
     */
    @Override
    String toString();

    /**
     * Test this {@link Snippet} against another {@link Snippet} for equality.
     *
     * @param other  other {@link Snippet}
     *
     * @return true if the {@link Snippet#template()} and {@link Snippet#values()}
     * of the two {@link Snippet templated strings} are equal.
     */
    @Override
    boolean equals(Object other);

    /**
     * Return a hashCode that derived from this {@link Snippet StringTemplate's}
     * fragments and values.
     *
     * @return a hash code for a sequences of fragments and values
     */
    @Override
    int hashCode();

    /**
     * {@return a new templated string that is the combination of this templated string with the provided templated string}.
     * The template of the returned templated string is obtained by {@linkplain Template#combine(Template) combining} the
     * templates associated with both templated strings. The values of the returned templated string are the union of the values
     * associated with both templated strings.
     * @param that the templated string to be combined with this templated string
     */
    Snippet<X> combine(Snippet<X> that);

    /**
     * Bootstrap method for creating a templated string.
     * The static arguments include the template fragments and parameters.
     *
     * @param lookup          method lookup from call site
     * @param name            method name - not used
     * @param type            the method type
     * @param template        the snippet template
     *
     * @return a new snippet of the provided type
     * @throws NullPointerException if any of the arguments is null
     * @throws IllegalArgumentException if type does not return a {@link Snippet} subclass
     */
    static CallSite makeSnippet(MethodHandles.Lookup lookup, String name, MethodType type, Template<?> template) {
        return new ConstantCallSite(Template.TMPLT_APPLY_HANDLE.bindTo(template)
                .asCollector(Object[].class, template.parameters().size())
                .asType(type));
    }

    /**
     * The template of a templated string.
     * @param <X> the type of the templated string associated with this template.
     */
    abstract class Template<X extends Language<X>> {

        private final List<String> fragments;
        private final List<Parameter> parameters;
        private final Language<X> language;

        static final MethodType TMPLT_APPLY_TYPE = MethodType.methodType(Snippet.class, Object[].class);
        static final MethodHandle TMPLT_APPLY_HANDLE;

        static {
            try {
                TMPLT_APPLY_HANDLE = MethodHandles.lookup().findVirtual(Template.class, "apply", TMPLT_APPLY_TYPE);
            } catch (ReflectiveOperationException ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }

        /**
         * Constructs a new template with given type, fragments and parameters
         * @param language the template language
         * @param fragments the template fragments
         * @param parameters the template parameters
         */
        public Template(Language<X> language, List<String> fragments, List<Parameter> parameters) {
            this.language = language;
            this.fragments = fragments;
            this.parameters = parameters;
        }

        // @@@: Maybe this should extend Parameter -- e.g. to capture cases where a template nests in another template.
        // But, there are cases where the nesting can be described statically, and cases where the nesting is more dynamic.
        // Example:
        // " hello \{ "bar \{ 42 }" }" // everything static
        // vs.
        // FormatString fs = ...
        // " hello \{ fs }" // dynamic (what is the shape of "fs"?)

        /**
         * {@return the string fragments associated with this template}
         */
        public final List<String> fragments() {
            return fragments;
        }

        /**
         * {@return the parameters associated with this template}
         */
        public final List<Parameter> parameters() {
            return parameters;
        }

        /**
         * {@return a new templated string with the provided values}. The type of the returned templated string
         * is the one associated with this template.
         * @param values the values from which the templated string is to be constructed
         */
        @SuppressWarnings("unchecked")
        public final Snippet<X> apply(Object... values) {
            return new SnippetImpl<>(this, values);
        }

        /**
         * {@return the template type}
         */
        public final Language<X> language() {
            return language;
        }

        /**
         * {@return a new snippet template that is the combination of this template with the provided template}.
         * The fragments of the returned template string are obtained by joining the fragments in both templates.
         * In this process, the last fragment of this template is merged with the first fragment of the provided template.
         * The parameters of the returned template are the union of the parameters associated with both templates.
         * @param that the templated string to be combined with this templated string
         */
        public final Template<X> combine(Template<X> that) {
            List<String> newFragments = new ArrayList<>(fragments());
            String last = newFragments.getLast();
            newFragments.set(newFragments.size() - 1, last + that.fragments().getFirst());
            newFragments.addAll(that.fragments().subList(1, that.fragments().size()));
            List<Parameter> newParameters = new ArrayList<>(parameters());
            newParameters.addAll(that.parameters());
            return language.makeTemplate(newFragments, newParameters);
        }

        /**
         * {@return a snippet that has the same type parameter as that of this template}
         * @param snippet the snippet to be converted
         */
        @SuppressWarnings("unchecked")
        public final Snippet<X> cast(Snippet<?> snippet) {
            if (snippet.template().language().getClass().equals(language.getClass())) {
                return (Snippet<X>)snippet;
            } else {
                throw new IllegalArgumentException("Snippet not compatible with language " + language.getClass());
            }
        }

        /**
         * Produces a diagnostic string that describes the fragments, parameters and type of this
         * {@link Template}.
         *
         * @return diagnostic string representing this template
         */
        @Override
        public final String toString() {
            return "Template<" + language.getClass().getSimpleName() + ">{" +
                    "fragments=" + fragments() +
                    ", parameters=" + parameters() +
                    '}';
        }

        /**
         * Test this {@link Template} against another {@link Template} for equality.
         *
         * @param other  other {@link Template}
         *
         * @return true if the two templates have the same {@linkplain Template#fragments() fragments},
         * {@linkplain Template#parameters() parameters} and they have the same type.
         */
        @Override
        public final boolean equals(Object other) {
            return other instanceof Snippet.Template<?> that &&
                    Objects.equals(fragments(), that.fragments()) &&
                    Objects.equals(parameters(), that.parameters()) &&
                    Objects.equals(language.getClass(), that.language.getClass());
        }

        /**
         * {@return a hash code derived from this {@link Template template's}
         * type, fragments and parameters}
         */
        @Override
        public final int hashCode() {
            return Objects.hash(fragments(), parameters(), language.getClass());
        }

        /**
         * Bootstrap method for creating templates.
         * The static arguments include the template fragments and parameters.
         *
         * @param lookup          method lookup from call site
         * @param name            method name - not used
         * @param type            the templated string type
         * @param language        the template language
         * @param fragments       fragment array for template
         * @param parameters      parameter array for template
         * @param <Z> the template language type
         *
         * @return a new template of the provided type
         * @throws NullPointerException if any of the arguments is null
         * @throws IllegalArgumentException if type does not return a {@link Snippet}
         */
        @SuppressWarnings("unchecked")
        public static <Z extends Language<Z>> Template<Z> makeTemplate(MethodHandles.Lookup lookup, String name, Class<Z> type,
                                                      Language<Z> language, String[] fragments, Parameter[] parameters) {
            return language.makeTemplate(List.of(fragments), List.of(parameters));
        }

        /**
         * A template parameter. Template parameters
         * have a type and an optional list of annotations.
         */
        public interface Parameter extends AnnotatedElement {
            /**
             * {@return the type of the template parameter}
             */
            Class<?> type();

            /**
             * Bootstrap method for creating a template parameter.
             * The static arguments include the template parameter annotations.
             *
             * @param lookup          method lookup from call site
             * @param name            method name - not used
             * @param type            type - not used
             * @param parameterType   the parameter type
             * @param annotations     the parameter annotations
             *
             * @return a new template parameter with the provided type and annotations
             * @throws NullPointerException if any of the arguments is null
             * @throws IllegalArgumentException if type does not return a {@link Snippet} subclass
             */
            static Parameter makeParameter(MethodHandles.Lookup lookup, String name, Class<?> type, String parameterType, Annotation[] annotations) {
                record ParameterImpl(Class<?> type, List<Annotation> annotations) implements Parameter {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                        return (T)annotations.stream()
                                .filter(a -> a.annotationType().equals(annotationClass))
                                .findFirst().orElse(null);
                    }

                    @Override
                    public Annotation[] getAnnotations() {
                        return annotations.toArray(Annotation[]::new);
                    }

                    @Override
                    public Annotation[] getDeclaredAnnotations() {
                        return annotations.toArray(Annotation[]::new);
                    }
                }
                MethodType desc = MethodType.fromMethodDescriptorString("()" + parameterType, lookup.lookupClass().getClassLoader());
                return new ParameterImpl(desc.returnType(), List.of(annotations));
            }
        }
    }

    /**
     * The snippet language. A factory for snippet templates.
     * @param <X> the snippet language type
     */
    interface Language<X extends Language<X>> {
        /**
         * {@return a new template for this language with given string fragments and parameters}
         * @param fragments the template fragments
         * @param parameters the template parameters
         */
        Template<X> makeTemplate(List<String> fragments, List<Template.Parameter> parameters);

        /**
         * Bootstrap method for creating a snippet language.
         *
         * @param lookup          method lookup from call site
         * @param name            method name - not used
         * @param type            the snippet language type
         * @param <Z> the snippet language type
         *
         * @return a new template of the provided type
         * @throws NullPointerException if any of the arguments is null
         * @throws IllegalArgumentException if the snippet language type cannot be constructed
         */
        @SuppressWarnings("unchecked")
        static <Z extends Language<Z>> Z makeLanguage(MethodHandles.Lookup lookup, String name, Class<Z> type) {
            try {
                return type.getConstructor().newInstance();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalArgumentException("Not a valid language type: " + type);
            }
        }
    }

    /**
     * A template for plain text snippets. Plain text snippets can be interpolated using {@link #join(Snippet)}.
     */
    class PlainText implements Language<PlainText> {

        /**
         * Constructs a new instance of the plain text language
         */
        public PlainText() { }

        static class Template extends Snippet.Template<PlainText> {
            private static final MethodHandle OBJECT_TO_STRING;
            private static final MethodHandle TEMPLATE_TO_STRING;

            static {
                try {
                    MethodHandles.Lookup lookup = MethodHandles.lookup();

                    MethodType mt = MethodType.methodType(String.class, Object.class);
                    OBJECT_TO_STRING = lookup.findStatic(Template.class, "objectToString", mt);

                    mt = MethodType.methodType(String.class, Snippet.class);
                    TEMPLATE_TO_STRING = lookup.findStatic(Template.class, "snippetToString", mt);
                } catch (ReflectiveOperationException ex) {
                    throw new AssertionError("carrier static init fail", ex);
                }
            }

            @Stable
            final MethodHandle joinerHandle;

            /**
             * Constructs a new plain text template with given type, fragments, and parameters.
             *
             * @param language       the plain text template language.
             * @param fragments  the plain text template fragments.
             * @param parameters the plain text template parameters.
             */
            @SuppressWarnings("this-escape")
            public Template(PlainText language, List<String> fragments, List<Parameter> parameters) {
                super(language, fragments, parameters);
                joinerHandle = makeJoinMH(this);
            }

            private static List<MethodHandle> filters(List<Parameter> parameters) {
                List<MethodHandle> filters = new ArrayList<>();
                for (Parameter parameter : parameters) {
                    MethodHandle filter;
                    Class<?> type = parameter.type();
                    if (type == Snippet.class) {
                        filter = TEMPLATE_TO_STRING;
                    } else if (type == Object.class) {
                        filter = OBJECT_TO_STRING;
                    } else {
                        filter = MethodHandles.identity(type);
                    }
                    filters.add(filter);
                }
                return filters;
            }

            private static MethodHandle makeJoinMH(Template template) {
                List<MethodHandle> filters = filters(template.parameters());
                List<Class<?>> ftypes = filters.stream()
                        .<Class<?>>map(f -> f.type().returnType()).toList();
                try {
                    MethodHandle joinMH = StringConcatFactory.makeConcatWithTemplate(template.fragments(), ftypes);
                    joinMH = MethodHandles.filterArguments(joinMH, 0, filters.toArray(MethodHandle[]::new));
                    return SnippetImpl.bindTo(template, joinMH);
                } catch (StringConcatException ex) {
                    throw new InternalError("constructing internal string template", ex);
                }
            }

            private static String objectToString(Object object) {
                if (object instanceof Snippet<?> snippet) {
                    if (snippet.template() instanceof Template template) {
                        return PlainText.join(template.cast(snippet));
                    } else {
                        throw new IllegalArgumentException("Unexpected nested snippet: " + snippet);
                    }
                } else {
                    return String.valueOf(object);
                }
            }

            @SuppressWarnings("unchecked")
            private static String snippetToString(Snippet<?> snippet) {
                if (snippet != null) {
                    if (snippet.template() instanceof Template) {
                        return PlainText.join((Snippet<PlainText>) snippet);
                    } else {
                        throw new IllegalArgumentException("Unexpected nested snippet: " + snippet);
                    }
                } else {
                    return "null";
                }
            }
        }

        @Override
        public Snippet.Template<PlainText> makeTemplate(List<String> fragments, List<Snippet.Template.Parameter> parameters) {
            return new Template(this, fragments, parameters);
        }

        /**
         * Returns the string interpolation of the fragments and values for the specified
         * plain text {@link Snippet}.
         * {@snippet lang = java:
         * String student = "Mary";
         * String teacher = "Johnson";
         * Snippet<PlainText> st = "The student \{student} is in \{teacher}'s classroom.";
         * String result = PlainText.join(st); // @highlight substring="join()"
         *}
         * In the above example, the value of  {@code result} will be
         * {@code "The student Mary is in Johnson's classroom."}. This is
         * produced by the interleaving concatenation of fragments and values from the supplied
         * {@link Snippet}. To accommodate concatenation, values are converted to strings
         * as if invoking {@link String#valueOf(Object)}.
         *
         * @param textSnippet target {@link Snippet}
         * @return interpolation of this {@link Snippet}
         *
         * @throws NullPointerException if textSnippet is null
         */
        public static String join(Snippet<PlainText> textSnippet) {
            Objects.requireNonNull(textSnippet, "textSnippet should not be null");
            try {
                return (String) ((Template)textSnippet.template()).joinerHandle.invokeExact(textSnippet);
            } catch (Throwable ex) {
                throw new InternalError(ex);
            }
        }
    }
}
