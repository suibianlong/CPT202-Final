package com.cpt202.HerLink.unit.enums;

import com.cpt202.HerLink.enums.ResourceTypeEnum;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ResourceTypeEnum Enum Tests")
class ResourceTypeEnumTest {

    @Nested
    @DisplayName("getLookupValues method: correctly combines value and aliases")
    class GetLookupValuesTests {

        @Test
        @DisplayName("IMAGE should return complete lookup array")
        void getLookupValues_Image_ReturnsAllValues() {
            String[] values = ResourceTypeEnum.IMAGE.getLookupValues();
            assertEquals(7, values.length);
            assertArrayEquals(
                new String[]{"photo", "Picture", "IMAGE", "PICTURE", "PHOTO", "PHOTO/IMAGE", "PHOTO_IMAGE"},
                values
            );
        }

        @Test
        @DisplayName("VIDEO should return correct lookup array")
        void getLookupValues_Video_ReturnsAllValues() {
            String[] values = ResourceTypeEnum.VIDEO.getLookupValues();
            assertEquals(3, values.length);
            assertArrayEquals(new String[]{"video", "Video", "VIDEO"}, values);
        }

        @Test
        @DisplayName("AUDIO should return correct lookup array")
        void getLookupValues_Audio_ReturnsAllValues() {
            String[] values = ResourceTypeEnum.AUDIO.getLookupValues();
            assertEquals(3, values.length);
            assertArrayEquals(new String[]{"audio", "Audio", "AUDIO"}, values);
        }

        @Test
        @DisplayName("DOCUMENT should return correct lookup array")
        void getLookupValues_Document_ReturnsAllValues() {
            String[] values = ResourceTypeEnum.DOCUMENT.getLookupValues();
            assertEquals(5, values.length);
        }

        @Test
        @DisplayName("EXTRA_LINK should return correct lookup array")
        void getLookupValues_ExtraLink_ReturnsAllValues() {
            String[] values = ResourceTypeEnum.EXTRA_LINK.getLookupValues();
            assertEquals(4, values.length);
        }

        @Test
        @DisplayName("OTHER should return correct lookup array")
        void getLookupValues_Other_ReturnsAllValues() {
            String[] values = ResourceTypeEnum.OTHER.getLookupValues();
            assertEquals(3, values.length);
        }
    }

    @Nested
    @DisplayName("fromValue method: normal matching cases")
    class FromValueNormalTests {

        @ParameterizedTest
        @ValueSource(strings = {"photo", "Picture", "IMAGE", "PICTURE", "PHOTO", "PHOTO/IMAGE", "PHOTO_IMAGE"})
        @DisplayName("Input image aliases → return IMAGE")
        void fromValue_ImageAliases_ReturnsImage(String input) {
            assertSame(ResourceTypeEnum.IMAGE, ResourceTypeEnum.fromValue(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"video", "Video", "VIDEO"})
        @DisplayName("Input video aliases → return VIDEO")
        void fromValue_VideoAliases_ReturnsVideo(String input) {
            assertSame(ResourceTypeEnum.VIDEO, ResourceTypeEnum.fromValue(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"audio", "Audio", "AUDIO"})
        @DisplayName("Input audio aliases → return AUDIO")
        void fromValue_AudioAliases_ReturnsAudio(String input) {
            assertSame(ResourceTypeEnum.AUDIO, ResourceTypeEnum.fromValue(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"document", "Document", "DOCUMENT", "FILE/DOCUMENT", "FILE_DOCUMENT"})
        @DisplayName("Input document aliases → return DOCUMENT")
        void fromValue_DocumentAliases_ReturnsDocument(String input) {
            assertSame(ResourceTypeEnum.DOCUMENT, ResourceTypeEnum.fromValue(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"extra link", "Extra Link", "EXTRA LINK", "EXTRA_LINK"})
        @DisplayName("Input extra link aliases → return EXTRA_LINK")
        void fromValue_ExtraLinkAliases_ReturnsExtraLink(String input) {
            assertSame(ResourceTypeEnum.EXTRA_LINK, ResourceTypeEnum.fromValue(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"other", "Other", "OTHER"})
        @DisplayName("Input other aliases → return OTHER")
        void fromValue_OtherAliases_ReturnsOther(String input) {
            assertSame(ResourceTypeEnum.OTHER, ResourceTypeEnum.fromValue(input));
        }
    }

    @Nested
    @DisplayName("fromValue method: boundary formatting cases")
    class FromValueBoundaryTests {

        @Test
        @DisplayName("Input with leading and trailing whitespace → match successfully")
        void fromValue_Whitespace_Success() {
            assertSame(ResourceTypeEnum.IMAGE, ResourceTypeEnum.fromValue("   PHOTO/IMAGE   "));
        }

        @Test
        @DisplayName("Input with mixed case → match successfully")
        void fromValue_MixedCase_Success() {
            assertSame(ResourceTypeEnum.VIDEO, ResourceTypeEnum.fromValue("ViDeO"));
        }

        @Test
        @DisplayName("Input containing _ / - → normalize and match successfully")
        void fromValue_SpecialCharacters_Success() {
            assertSame(ResourceTypeEnum.DOCUMENT, ResourceTypeEnum.fromValue("FILE-DOCUMENT"));
            assertSame(ResourceTypeEnum.EXTRA_LINK, ResourceTypeEnum.fromValue("EXTRA__LINK"));
        }
    }

    @Nested
    @DisplayName("fromValue method: exception cases")
    class FromValueExceptionTests {

        @Test
        @DisplayName("Input null → throw exception")
        void fromValue_Null_ThrowsException() {
            var ex = assertThrows(IllegalArgumentException.class, () -> ResourceTypeEnum.fromValue(null));
            assertEquals("Resource type cannot be null", ex.getMessage());
        }

        @Test
        @DisplayName("Input unknown type → throw exception")
        void fromValue_UnknownValue_ThrowsException() {
            var ex = assertThrows(IllegalArgumentException.class, () -> ResourceTypeEnum.fromValue("invalid-type"));
            assertEquals("Unknown resource type: invalid-type", ex.getMessage());
        }

        @Test
        @DisplayName("Input only spaces → throw exception")
        void fromValue_OnlySpaces_ThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> ResourceTypeEnum.fromValue("     "));
        }
    }
}
