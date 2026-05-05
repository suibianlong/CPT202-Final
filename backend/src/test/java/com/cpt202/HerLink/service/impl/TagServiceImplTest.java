package com.cpt202.HerLink.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TagServiceImplTest {

    private TagServiceImpl tagService;

    @BeforeEach
    void setUp() {
        // Mapper 接口无法实例化，只能传 null
        tagService = new TagServiceImpl(null);
    }

    /**
     * 异常场景：mapper 为 null，调用数据库方法抛出空指针异常
     */
    @Test
    void listTagOptions_WithNullMapper_ShouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                tagService.listTagOptions()
        );
    }
}