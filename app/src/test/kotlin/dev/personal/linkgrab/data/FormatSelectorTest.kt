package dev.personal.linkgrab.data

import dev.personal.linkgrab.model.AudioFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormatSelectorTest {
    @Test fun instagramFormatIsBaPlusBv() {
        assertEquals("ba+bv", FormatSelector.instagramVideoFormat())
    }

    @Test fun videoFormatRespectsQualityCeiling() {
        assertEquals(
            "bv*[vcodec^=avc1][height<=720]+ba[ext=m4a]/b[ext=mp4][height<=720]",
            FormatSelector.videoFormat(720)
        )
    }

    @Test fun audioFormatPrefersM4a() {
        assertEquals("ba[ext=m4a]/ba", FormatSelector.audioFormat(AudioFormat.MP3))
    }

    @Test fun instagramUrlsAreDetected() {
        assertTrue(FormatSelector.isInstagramUrl("https://www.instagram.com/reel/ABC123/"))
        assertTrue(FormatSelector.isInstagramUrl("https://instagram.com/p/XYZ"))
        assertTrue(FormatSelector.isInstagramUrl("https://instagr.am/p/XYZ/"))
    }

    @Test fun nonInstagramUrlsAreNotDetected() {
        assertFalse(FormatSelector.isInstagramUrl("https://www.youtube.com/watch?v=abc"))
        assertFalse(FormatSelector.isInstagramUrl("https://example.com"))
        assertFalse(FormatSelector.isInstagramUrl("http://youtube.com"))
        assertFalse(FormatSelector.isInstagramUrl(""))
        assertFalse(FormatSelector.isInstagramUrl(null))
    }
}