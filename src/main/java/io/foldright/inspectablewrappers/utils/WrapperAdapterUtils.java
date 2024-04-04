package io.foldright.inspectablewrappers.utils;

import edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.foldright.inspectablewrappers.Attachable;
import io.foldright.inspectablewrappers.Wrapper;
import io.foldright.inspectablewrappers.WrapperAdapter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static io.foldright.inspectablewrappers.utils.WrapperAdapterProxyRelatedMethod.*;
import static java.util.Objects.requireNonNull;


/**
 * Utility class for creating {@link WrapperAdapter} instances
 * without writing boilerplate codes of creating new adapter classes.
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 */
@DefaultAnnotationForParameters(NonNull.class)
public final class WrapperAdapterUtils {
    /**
     * Creates a {@link WrapperAdapter} instance of the given biz interface type by
     * the underlying({@link Wrapper#unwrap()}) and adaptee({@link WrapperAdapter#adaptee()}) instances.
     *
     * @param <T>          the type of instances that be wrapped
     * @param bizInterface the class of instances that be wrapped
     * @param underlying   the underlying instance that be wrapped, more info see {@link Wrapper#unwrap()}
     * @param adaptee      the adapted/existed wrapper instance, more info see {@link WrapperAdapter#adaptee()}
     * @return the new {@link WrapperAdapter} instance
     * @throws IllegalArgumentException if {@code bizInterface} is not an interface,
     *                                  or {@code bizInterface} is {@link Wrapper}/{@link WrapperAdapter}/{@link Attachable},
     *                                  or underlying is not an instance of {@code bizInterface},
     *                                  or adaptee is not an instance of {@code bizInterface},
     *                                  or adaptee is an instance of {@link Wrapper}
     * @throws NullPointerException     if any argument is null
     * @see Wrapper#unwrap()
     * @see WrapperAdapter#adaptee()
     */
    @NonNull
    public static <T> T createWrapperAdapter(Class<? super T> bizInterface, T underlying, T adaptee) {
        return createWrapperAdapter0(
                requireNonNull(bizInterface, "bizInterface is null"),
                requireNonNull(underlying, "underlying is null"),
                requireNonNull(adaptee, "adaptee is null"),
                null);
    }

    /**
     * Creates a {@link WrapperAdapter} instance of the given biz interface type and {@link Attachable} type by
     * the underlying({@link Wrapper#unwrap()}), adaptee({@link WrapperAdapter#adaptee()}) and attachable instances.
     *
     * @param <T>          the type of instances that be wrapped
     * @param bizInterface the class of instances that be wrapped
     * @param underlying   the underlying instance that be wrapped, more info see {@link Wrapper#unwrap()}
     * @param adaptee      the adapted/existed wrapper instance, more info see {@link WrapperAdapter#adaptee()}
     * @param attachable   the attachable instance, more info see {@link Attachable}
     * @return the new {@link WrapperAdapter} instance
     * @throws IllegalArgumentException if {@code bizInterface} is not an interface,
     *                                  or {@code bizInterface} is {@link Wrapper}/{@link WrapperAdapter}/{@link Attachable},
     *                                  or underlying is not an instance of {@code bizInterface},
     *                                  or adaptee is not an instance of {@code bizInterface},
     *                                  or adaptee is an instance of {@link Wrapper}
     * @throws NullPointerException     if any argument is null
     * @see Wrapper#unwrap()
     * @see WrapperAdapter#adaptee()
     * @see Attachable#getAttachment(Object)
     * @see Attachable#setAttachment(Object, Object)
     */
    @NonNull
    public static <T> T createWrapperAdapter(
            Class<? super T> bizInterface, T underlying, T adaptee, Attachable<?, ?> attachable) {
        return createWrapperAdapter0(
                requireNonNull(bizInterface, "bizInterface is null"),
                requireNonNull(underlying, "underlying is null"),
                requireNonNull(adaptee, "adaptee is null"),
                requireNonNull(attachable, "attachable is null"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T createWrapperAdapter0(
            Class<? super T> bizInterface, T underlying, T adaptee, @Nullable Attachable<?, ?> attachable) {
        checkTypeRequirements(bizInterface, underlying, adaptee);

        final InvocationHandler handler = (proxy, method, args) -> {
            if (UNWRAP.sameSignatureAs(method)) return underlying;
            if (ADAPTEE.sameSignatureAs(method)) return adaptee;

            if (attachable != null && GET_ATTACHMENT.sameSignatureAs(method)) {
                return ((Attachable) attachable).getAttachment(args[0]);
            }
            if (attachable != null && SET_ATTACHMENT.sameSignatureAs(method)) {
                ((Attachable) attachable).setAttachment(args[0], args[1]);
                return null;
            }

            if (TO_STRING.sameSignatureAs(method)) {
                return "[WrapperAdapter proxy created by WrapperAdapterUtils] " + adaptee;
            }

            return method.invoke(adaptee, args);
        };

        return (T) Proxy.newProxyInstance(
                adaptee.getClass().getClassLoader(),
                attachable == null
                        ? new Class[]{bizInterface, WrapperAdapter.class}
                        : new Class[]{bizInterface, WrapperAdapter.class, Attachable.class},
                handler);
    }

    private static <T> void checkTypeRequirements(Class<T> bizInterface, T underlying, T adaptee) {
        if (!bizInterface.isInterface()) {
            throw new IllegalArgumentException("bizInterface(" + bizInterface.getName() + ") is not an interface");
        }
        if (bizInterface == Wrapper.class
                || bizInterface == WrapperAdapter.class
                || bizInterface == Attachable.class) {
            throw new IllegalArgumentException(bizInterface.getName() +
                    " is auto implemented by proxy, not a valid biz interface");
        }

        if (!bizInterface.isAssignableFrom(underlying.getClass())) {
            throw new IllegalArgumentException("underlying(" + underlying.getClass().getName() +
                    ") is not an instance of " + bizInterface.getName());
        }
        if (!bizInterface.isAssignableFrom(adaptee.getClass())) {
            throw new IllegalArgumentException("adaptee(" + adaptee.getClass().getName() +
                    ") is not an instance of " + bizInterface.getName());
        }

        if (adaptee instanceof Wrapper) {
            throw new IllegalArgumentException("adaptee(" + adaptee.getClass().getName() +
                    ") is an instance of Wrapper, adapting a Wrapper to a Wrapper is UNNECESSARY");
        }
    }

    /**
     * NO need to create instance at all
     */
    private WrapperAdapterUtils() {
    }
}

/**
 * Uses {@code Clazz.class.getMethod(...)} rather than lonely method name constants
 * to get {@link Method} instance; The former is IDE aware, so safer and more refactor friendly.
 */
enum WrapperAdapterProxyRelatedMethod {
    /**
     * {@link Wrapper#unwrap()}
     */
    UNWRAP(() -> Wrapper.class.getMethod("unwrap")),
    /**
     * {@link WrapperAdapter#adaptee()}
     */
    ADAPTEE(() -> WrapperAdapter.class.getMethod("adaptee")),
    /**
     * {@link Attachable#getAttachment(Object)}
     */
    GET_ATTACHMENT(() -> Attachable.class.getMethod("getAttachment", Object.class)),
    /**
     * {@link Attachable#setAttachment(Object, Object)}
     */
    SET_ATTACHMENT(() -> Attachable.class.getMethod("setAttachment", Object.class, Object.class)),
    /**
     * {@link Object#toString()}
     */
    TO_STRING(() -> Object.class.getMethod("toString")),
    ;

    private final String methodName;
    private final Class<?>[] parameterTypes;

    WrapperAdapterProxyRelatedMethod(Callable<Method> method) {
        try {
            Method m = method.call();
            this.methodName = m.getName();
            this.parameterTypes = m.getParameterTypes();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    boolean sameSignatureAs(Method method) {
        return methodName.equals(method.getName()) && Arrays.equals(parameterTypes, method.getParameterTypes());
    }
}
