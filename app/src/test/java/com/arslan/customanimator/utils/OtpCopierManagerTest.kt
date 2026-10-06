package com.arslan.customanimator.utils

import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class OtpCopierManagerTest {

    private lateinit var context: Context

    private fun extract(text: String) = OtpCopierManager.extractCode(OtpCopierManager.DEFAULT_KEYWORDS, emptyList(), text)

    private fun clipboardText(): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        OtpCopierManager.setEnabled(context, false)
        OtpCopierManager.resetKeywords(context)
        OtpCopierManager.getPatterns(context).forEach { OtpCopierManager.removePattern(context, it) }
    }

    @Test
    fun codeAfterKeywordIsExtracted() {
        assertEquals("482913", extract("Your verification code is 482913. Do not share it."))
        assertEquals("7731", extract("OTP: 7731"))
    }

    @Test
    fun codeBeforeKeywordIsExtracted() {
        assertEquals("482913", extract("482913 is your login code"))
    }

    @Test
    fun turkishMessagesAreExtracted() {
        assertEquals("905512", extract("Doğrulama kodunuz: 905512"))
        assertEquals("1234", extract("ŞİFRENİZ 1234, kimseyle paylaşmayın"))
    }

    @Test
    fun messagesWithoutAKeywordOrCodeAreIgnored() {
        assertNull(extract("Your order 482913 has shipped"))
        assertNull(extract("Use the code we sent to your email"))
        assertNull(extract("Call 05321234567 about your password"))
    }

    @Test
    fun customPatternCopiesFirstGroupOrWholeMatch() {
        val text = "token AB12 expires soon"
        assertEquals("AB12", OtpCopierManager.extractCode(emptyList(), listOf("token ([A-Z0-9]{4})"), text))
        assertEquals("991", OtpCopierManager.extractCode(emptyList(), listOf("(", "\\d{3}"), "pick 991"))
        assertNull(OtpCopierManager.extractCode(emptyList(), emptyList(), "code 123456"))
    }

    @Test
    fun addedKeywordIsUsedAndRemovedKeywordIsNot() {
        assertNull(extract("Jeton: 482913"))
        assertTrue(OtpCopierManager.addKeyword(context, " jeton "))
        assertFalse(OtpCopierManager.addKeyword(context, "JETON"))
        assertFalse(OtpCopierManager.addKeyword(context, " "))
        val keywords = OtpCopierManager.getKeywords(context)
        assertEquals("482913", OtpCopierManager.extractCode(keywords, emptyList(), "Jeton: 482913"))
        OtpCopierManager.removeKeyword(context, "jeton")
        assertFalse("jeton" in OtpCopierManager.getKeywords(context))
        OtpCopierManager.removeKeyword(context, "code")
        OtpCopierManager.resetKeywords(context)
        assertEquals(OtpCopierManager.DEFAULT_KEYWORDS, OtpCopierManager.getKeywords(context))
    }

    @Test
    fun keywordsAreMatchedLiterally() {
        assertEquals("4821", OtpCopierManager.extractCode(listOf("a.b"), emptyList(), "a.b 4821"))
        assertNull(OtpCopierManager.extractCode(listOf("a.b"), emptyList(), "axb 4821"))
    }

    @Test
    fun invalidPatternIsRejectedAndNeverStored() {
        assertFalse(OtpCopierManager.isValidPattern("("))
        assertFalse(OtpCopierManager.addPattern(context, "("))
        assertFalse(OtpCopierManager.addPattern(context, " "))
        assertTrue(OtpCopierManager.getPatterns(context).isEmpty())
    }

    @Test
    fun patternsRoundTripAndAreRemovable() {
        assertTrue(OtpCopierManager.addPattern(context, "\\d{6}"))
        assertFalse(OtpCopierManager.addPattern(context, "\\d{6}"))
        assertEquals(listOf("\\d{6}"), OtpCopierManager.getPatterns(context))
        OtpCopierManager.removePattern(context, "\\d{6}")
        assertTrue(OtpCopierManager.getPatterns(context).isEmpty())
    }

    @Test
    fun nothingIsCopiedWhileDisabled() {
        OtpCopierManager.onNotification(context, "disabled", "Your code is 111222")
        assertNull(clipboardText())
    }

    @Test
    fun matchingNotificationIsCopiedWhenEnabled() {
        OtpCopierManager.setEnabled(context, true)
        OtpCopierManager.onNotification(context, "enabled", "Bank\nYour code is 333444")
        assertEquals("333444", clipboardText())
    }

    @Test
    fun toastIsOnByDefaultAndCanBeTurnedOff() {
        OtpCopierManager.setEnabled(context, true)
        assertTrue(OtpCopierManager.isToastEnabled(context))
        OtpCopierManager.onNotification(context, "toast-on", "Your code is 555666")
        assertEquals("Code copied: 555666", ShadowToast.getTextOfLatestToast())
        ShadowToast.reset()
        OtpCopierManager.setToastEnabled(context, false)
        OtpCopierManager.onNotification(context, "toast-off", "Your code is 777888")
        assertEquals("777888", clipboardText())
        assertNull(ShadowToast.getLatestToast())
        OtpCopierManager.setToastEnabled(context, true)
    }

    @Test
    fun sensitiveAccessFailsSafelyWithoutShizuku() {
        assertFalse(OtpCopierManager.hasSensitiveAccess(context))
        assertFalse(OtpCopierManager.grantSensitiveAccess(context))
    }
}
