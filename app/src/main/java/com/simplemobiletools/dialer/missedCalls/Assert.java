package com.simplemobiletools.dialer.missedCalls;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import javax.annotation.CheckReturnValue;

public class Assert {
    private static boolean areThreadAssertsEnabled = true;

    public static void setAreThreadAssertsEnabled(boolean areThreadAssertsEnabled) {
        Assert.areThreadAssertsEnabled = areThreadAssertsEnabled;
    }

    /**
     * Called when a truly exceptional case occurs.
     *
     * @throws AssertionError
     * @deprecated Use throw Assert.create*FailException() instead.
     */
    @Deprecated
    public static void fail() {
        throw new AssertionError("Fail");
    }

    /**
     * Called when a truly exceptional case occurs.
     *
     * @param reason the optional reason to supply as the exception message
     * @throws AssertionError
     * @deprecated Use throw Assert.create*FailException() instead.
     */
    @Deprecated
    public static void fail(String reason) {
        throw new AssertionError(reason);
    }

    @CheckReturnValue
    public static AssertionError createAssertionFailException(String msg) {
        return new AssertionError(msg);
    }

    @CheckReturnValue
    public static AssertionError createAssertionFailException(String msg, Throwable reason) {
        return new AssertionError(msg, reason);
    }

    @CheckReturnValue
    public static UnsupportedOperationException createUnsupportedOperationFailException() {
        return new UnsupportedOperationException();
    }

    @CheckReturnValue
    public static UnsupportedOperationException createUnsupportedOperationFailException(String msg) {
        return new UnsupportedOperationException(msg);
    }

    @CheckReturnValue
    public static IllegalStateException createIllegalStateFailException() {
        return new IllegalStateException();
    }

    @CheckReturnValue
    public static IllegalStateException createIllegalStateFailException(String msg) {
        return new IllegalStateException(msg);
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression) {
        checkArgument(expression, null);
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression a boolean expression
     * @param messageTemplate the message to log, possible with format arguments.
     * @param args optional arguments to be used in the formatted string.
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(
        boolean expression, @Nullable String messageTemplate, Object... args) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void checkState(boolean expression) {
        checkState(expression, null);
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * @param expression a boolean expression
     * @param messageTemplate the message to log, possible with format arguments.
     * @param args optional arguments to be used in the formatted string.
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void checkState(
        boolean expression, @Nullable String messageTemplate, Object... args) {
        if (!expression) {
            throw new IllegalStateException();
        }
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling method is not null.
     *
     * @param reference an object reference
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    @NonNull
    public static <T> T isNotNull(@Nullable T reference) {
        return isNotNull(reference, null);
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling method is not null.
     *
     * @param reference an object reference
     * @param messageTemplate the message to log, possible with format arguments.
     * @param args optional arguments to be used in the formatted string.
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    @NonNull
    public static <T> T isNotNull(
        @Nullable T reference, @Nullable String messageTemplate, Object... args) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }
}
