package com.cpt202.HerLink.unit.util;

import com.cpt202.HerLink.util.*;
import com.cpt202.HerLink.util.ResourceStatusValidator;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.exception.AppException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourceStatusValidatorTest {

    @Test
    void assertEditable_shouldPassForDraft() {
        Resource resource = new Resource();
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());

        assertDoesNotThrow(() -> ResourceStatusValidator.assertEditable(resource));
    }

    @Test
    void assertEditable_shouldPassForRejected() {
        Resource resource = new Resource();
        resource.setStatus(ResourceStatusEnum.REJECTED.getValue());

        assertDoesNotThrow(() -> ResourceStatusValidator.assertEditable(resource));
    }

    @Test
    void assertEditable_shouldThrowForApproved() {
        Resource resource = new Resource();
        resource.setStatus(ResourceStatusEnum.APPROVED.getValue());

        AppException exception = assertThrows(
                AppException.class,
                () -> ResourceStatusValidator.assertEditable(resource)
        );

        // assertion
        assertEquals(409, exception.getStatusCode());
        assertEquals("Current resource status does not allow editing.", exception.getMessage());
    }

    @Test
    void assertEditable_shouldThrowWhenResourceIsNull() {
        // call
        AppException exception = assertThrows(
                AppException.class,
                () -> ResourceStatusValidator.assertEditable(null)
        );

        // assertion
        assertEquals(404, exception.getStatusCode());
        assertEquals("Resource does not exist.", exception.getMessage());
    }

    @Test
    void assertSubmittable_shouldPassForDraft() {
        Resource resource = new Resource();
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());

        // call & assertion
        assertDoesNotThrow(() -> ResourceStatusValidator.assertSubmittable(resource));
    }

    @Test
    void assertSubmittable_shouldPassForRejected() {
        Resource resource = new Resource();
        resource.setStatus(ResourceStatusEnum.REJECTED.getValue());

        // call & assertion
        assertDoesNotThrow(() -> ResourceStatusValidator.assertSubmittable(resource));
    }

    @Test
    void assertSubmittable_shouldThrowForPendingReview() {
        Resource resource = new Resource();
        resource.setStatus(ResourceStatusEnum.PENDING_REVIEW.getValue());

        AppException exception = assertThrows(
                AppException.class,
                () -> ResourceStatusValidator.assertSubmittable(resource)
        );

        // assertion
        assertEquals(409, exception.getStatusCode());
        assertEquals("Current resource status does not allow submission.", exception.getMessage());
    }

    @Test
    void assertSubmittable_shouldThrowWhenResourceIsNull() {
        // call
        AppException exception = assertThrows(
                AppException.class,
                () -> ResourceStatusValidator.assertSubmittable(null)
        );

        // assertion
        assertEquals(404, exception.getStatusCode());
        assertEquals("Resource does not exist.", exception.getMessage());
    }
}
