package com.cpt202.HerLink.testutil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mockito.invocation.Invocation;
import static org.mockito.Mockito.mockingDetails;

public final class MockInvocationAssertions {

    private MockInvocationAssertions() {
    }

    public static void assertNoInteractions(Object... mocks) {
        for (Object mock : mocks) {
            List<Invocation> invocations = new ArrayList<>(mockingDetails(mock).getInvocations());
            assertTrue(
                    invocations.isEmpty(),
                    () -> "Expected no interactions, but found: " + describeInvocations(invocations)
            );
        }
    }

    public static void assertInvokedOnce(Object mock, String methodName) {
        assertEquals(
                1,
                findInvocations(mock, methodName).size(),
                () -> "Expected method " + methodName + " to be called once, but found: "
                        + describeInvocations(findInvocations(mock, methodName))
        );
    }

    public static void assertInvokedOnceWithArgs(Object mock, String methodName, Object... expectedArgs) {
        Invocation invocation = findSingleInvocation(mock, methodName);
        assertEquals(
                expectedArgs.length,
                invocation.getArguments().length,
                () -> "Expected " + expectedArgs.length + " arguments for " + methodName
                        + " but found " + invocation.getArguments().length
        );
        for (int i = 0; i < expectedArgs.length; i++) {
            final int argumentIndex = i;
            final Object expectedArgument = expectedArgs[i];
            assertEquals(
                    expectedArgument,
                    invocation.getArgument(argumentIndex),
                    () -> "Expected argument " + argumentIndex + " for " + methodName + " to be "
                            + expectedArgument + " but was " + invocation.getArgument(argumentIndex)
            );
        }
    }

    public static void assertInvokedOnceWithArgsPrefix(Object mock, String methodName, Object... expectedArgs) {
        Invocation invocation = findSingleInvocation(mock, methodName);
        assertTrue(
                invocation.getArguments().length >= expectedArgs.length,
                () -> "Expected at least " + expectedArgs.length + " arguments for " + methodName
                        + " but found " + invocation.getArguments().length
        );
        for (int i = 0; i < expectedArgs.length; i++) {
            final int argumentIndex = i;
            final Object expectedArgument = expectedArgs[i];
            assertEquals(
                    expectedArgument,
                    invocation.getArgument(argumentIndex),
                    () -> "Expected argument " + argumentIndex + " for " + methodName + " to be "
                            + expectedArgument + " but was " + invocation.getArgument(argumentIndex)
            );
        }
    }

    public static void assertInvokedOnceWithArgAt(Object mock, String methodName, int index, Object expectedArg) {
        Invocation invocation = findSingleInvocation(mock, methodName);
        assertTrue(
                index >= 0 && index < invocation.getArguments().length,
                () -> "Expected argument index " + index + " to exist for " + methodName
                        + " but only found " + invocation.getArguments().length + " arguments."
        );
        assertEquals(
                expectedArg,
                invocation.getArgument(index),
                () -> "Expected argument " + index + " for " + methodName + " to be " + expectedArg
                        + " but was " + invocation.getArgument(index)
        );
    }

    private static Invocation findSingleInvocation(Object mock, String methodName) {
        List<Invocation> invocations = findInvocations(mock, methodName);
        assertEquals(
                1,
                invocations.size(),
                () -> "Expected method " + methodName + " to be called once, but found: "
                        + describeInvocations(invocations)
        );
        return invocations.get(0);
    }

    private static List<Invocation> findInvocations(Object mock, String methodName) {
        return mockingDetails(mock).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals(methodName))
                .collect(Collectors.toList());
    }

    private static String describeInvocations(List<Invocation> invocations) {
        if (invocations.isEmpty()) {
            return "[]";
        }
        return invocations.stream()
                .map(invocation -> invocation.getMethod().getName()
                        + Arrays.toString(invocation.getArguments()))
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
