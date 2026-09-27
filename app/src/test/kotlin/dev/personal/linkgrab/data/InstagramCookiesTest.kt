package dev.personal.linkgrab.data

import kotlin.test.Test
import kotlin.test.assertEquals

class InstagramCookiesTest {
    @Test
    fun convertsCookieHeaderToNetscapeFormat() {
        val expected = "# Netscape HTTP Cookie File\n" +
            "#HttpOnly_.instagram.com\tTRUE\t/\tTRUE\t1800000000\tsessionid\tABC123\n" +
            ".instagram.com\tTRUE\t/\tTRUE\t1800000000\tcsrftoken\txyz987\n" +
            ".instagram.com\tTRUE\t/\tTRUE\t1800000000\tig_did\tp7FG_7F"
        assertEquals(expected, InstagramCookies.toNetscape("sessionid=ABC123; csrftoken=xyz987; ig_did=p7FG_7F", 1800000000L))
    }

    @Test
    fun blankInputIsEmpty() {
        assertEquals("", InstagramCookies.toNetscape("", 1800000000L))
        assertEquals("", InstagramCookies.toNetscape("   ", 1800000000L))
    }

    @Test
    fun optionalCookiePrefixIsStripped() {
        assertEquals(
            "# Netscape HTTP Cookie File\n#HttpOnly_.instagram.com\tTRUE\t/\tTRUE\t1800000000\tsessionid\tABC123",
            InstagramCookies.toNetscape("Cookie: sessionid=ABC123", 1800000000L)
        )
    }

    @Test
    fun tokensWithoutEqualsAreSkipped() {
        assertEquals(
            "# Netscape HTTP Cookie File\n" +
                "#HttpOnly_.instagram.com\tTRUE\t/\tTRUE\t1800000000\tsessionid\tABC\n" +
                ".instagram.com\tTRUE\t/\tTRUE\t1800000000\tfoo\tbar",
            InstagramCookies.toNetscape("sessionid=ABC; dpr; ; foo=bar", 1800000000L)
        )
    }

    @Test
    fun valueMayContainEquals() {
        assertEquals(
            "# Netscape HTTP Cookie File\n.instagram.com\tTRUE\t/\tTRUE\t1800000000\tcsrftoken\t8PQ=okX",
            InstagramCookies.toNetscape("csrftoken=8PQ=okX", 1800000000L)
        )
    }

    @Test
    fun emptyWhenNoValidCookieTokens() {
        assertEquals("", InstagramCookies.toNetscape("garbage; ;", 1800000000L))
    }

    @Test
    fun newlinesAndTabsStrippedFromValues() {
        assertEquals(
            "# Netscape HTTP Cookie File\n#HttpOnly_.instagram.com\tTRUE\t/\tTRUE\t1800000000\tsessionid\tABCDE",
            InstagramCookies.toNetscape("sessionid=AB\nCD\tE", 1800000000L)
        )
    }
}