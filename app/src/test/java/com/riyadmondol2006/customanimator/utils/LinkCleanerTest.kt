package com.riyadmondol2006.customanimator.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkCleanerTest {

    private val providers = LinkCleaner.parseRules(
        """
        {"providers": {
          "globalRules": {"urlPattern": ".*", "rules": ["(?:%3F)?utm(?:_[a-z_]*)?", "fbclid"],
            "exceptions": ["^https?:\\/\\/[^/]+\\/login"]},
          "youtube": {"urlPattern": "^https?:\\/\\/(?:[a-z0-9-]+\\.)*?(youtube\\.com|youtu\\.be)", "rules": ["si"]},
          "amazon": {"urlPattern": "^https?:\\/\\/(?:[a-z0-9-]+\\.)*?amazon(?:\\.[a-z]{2,}){1,}",
            "rules": ["qid"], "rawRules": ["\\/ref=[^/?]*"], "referralMarketing": ["tag"]},
          "facebook": {"urlPattern": "^https?:\\/\\/(?:[a-z0-9-]+\\.)*?facebook\\.com",
            "redirections": ["^https?:\\/\\/l[a-z]?\\.facebook\\.com/l\\.php\\?.*?u=(https?%3A%2F%2F[^&]*)"]},
          "broken": {"urlPattern": "(", "rules": ["x"]}
        }}
        """
    )

    private fun clean(text: String) = LinkCleaner.clean(text, providers)

    @Test
    fun removesTrackingParamsAndKeepsOthers() {
        assertEquals(
            "https://example.com/a?id=5#top",
            clean("https://example.com/a?UTM_Source=x&id=5&fbclid=abc#top")
        )
    }

    @Test
    fun providerRulesOnlyApplyToTheirSite() {
        assertEquals("https://youtu.be/abc", clean("https://youtu.be/abc?si=xyz"))
        assertEquals("https://example.com/?si=xyz", clean("https://example.com/?si=xyz"))
    }

    @Test
    fun appliesRawRulesAndReferralParams() {
        assertEquals(
            "https://www.amazon.com.tr/dp/B01",
            clean("https://www.amazon.com.tr/dp/B01/ref=sr_1_1?qid=1&tag=aff-21")
        )
    }

    @Test
    fun unwrapsOnlyKnownRedirects() {
        assertEquals(
            "https://example.com/page",
            clean("https://l.facebook.com/l.php?u=https%3A%2F%2Fexample.com%2Fpage%3Futm_medium%3Dx&h=1")
        )
        assertEquals(
            "https://example.com/go?url=https%3A%2F%2Fother.com",
            clean("https://example.com/go?url=https%3A%2F%2Fother.com")
        )
    }

    @Test
    fun exceptionsKeepTheLink() {
        assertEquals("https://example.com/login?utm_source=x", clean("https://example.com/login?utm_source=x"))
    }

    @Test
    fun keepsTrailingPunctuationOutsideTheLink() {
        assertEquals("(see https://example.com/).", clean("(see https://example.com/?utm_source=x)."))
        assertEquals(
            "https://en.wikipedia.org/wiki/Java_(language)",
            clean("https://en.wikipedia.org/wiki/Java_(language)")
        )
    }

    @Test
    fun skipsProvidersWithInvalidPatterns() {
        assertEquals(4, providers.size)
    }

    @Test
    fun textWithoutLinkHasNoLink() {
        assertFalse(LinkCleaner.hasLink("no link here"))
    }
}
