package jdk.internal.snippets;

import java.lang.Snippet.Template;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class SnippetImpl<X extends Snippet.Language<X>> implements Snippet<X> {

    final Template<X> template;
    final Object[] values;

    public SnippetImpl(Template<X> template, Object[] values) {
        this.template = template;
        this.values = values;
    }

    @Override
    public Template<X> template() {
        return template;
    }

    @Override
    public List<Object> values() {
        return Arrays.asList(values);
    }

    @Override
    public Object value(int index) {
        return values[index];
    }

    @Override
    public Snippet<X> combine(Snippet<X> that) {
        Object[] values = Stream.concat(values().stream(), that.values().stream()).toArray();
        return template.combine(that.template()).apply(values);
    }

    @Override
    public String toString() {
        return "Snippet<" + template().language().getClass().getSimpleName() + ">{" +
                "values=" + values() +
                ", template=" + template() +
                '}';
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SnippetImpl<?> that &&
                that.values().equals(values()) &&
                that.template().equals(template());
    }

    @Override
    public int hashCode() {
        return Objects.hash(values(), template());
    }

    // internal API

    private static final MethodHandle GET_VALUE;

    static {
        try {
            GET_VALUE = MethodHandles.lookup().findStatic(SnippetImpl.class, "getValue",
                    MethodType.methodType(Object.class, Snippet.class, int.class));
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    static MethodHandle getter(int index, Class<?> ptype) {
        return MethodHandles.insertArguments(GET_VALUE, 1, index)
                .asType(MethodType.methodType(ptype, Snippet.class));
    }

    static Object getValue(Snippet<?> st, int index) {
        return ((SnippetImpl<?>)st).values[index];
    }

    // @@@: do we really need this? Throughput win is really minimal, but a lot more bytecode spinning
    public static MethodHandle bindTo(Template<?> template, MethodHandle mh) {
        Objects.requireNonNull(mh, "mh must not be null");
        MethodHandle[] getters = new MethodHandle[template.parameters().size()];
        for (int i = 0 ; i < getters.length ; i++) {
            getters[i] = getter(i, template.parameters().get(i).type());
        }
        int[] permute = new int[getters.length];
        mh = MethodHandles.filterArguments(mh, 0, getters);
        MethodType mt = MethodType.methodType(mh.type()
                .returnType(), Snippet.class);
        mh = MethodHandles.permuteArguments(mh, mt, permute);
        return mh.asType(mt);
    }
}
