package com.arslan.customanimator.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppCleanerManagerTest {

    private val receivedImages = WhatsAppMediaTarget(WhatsAppMediaCategory.IMAGES, WhatsAppMediaPart.RECEIVED)
    private val sentImages = WhatsAppMediaTarget(WhatsAppMediaCategory.IMAGES, WhatsAppMediaPart.SENT)
    private val voiceNotes = WhatsAppMediaTarget(WhatsAppMediaCategory.VOICE_NOTES, WhatsAppMediaPart.RECEIVED)

    @Test
    fun onlySplitCategoriesHaveSentAndPrivateParts() {
        assertEquals(WhatsAppMediaPart.entries, WhatsAppMediaCategory.IMAGES.parts)
        assertEquals(listOf(WhatsAppMediaPart.RECEIVED), WhatsAppMediaCategory.VOICE_NOTES.parts)
        assertEquals(WhatsAppCleanerManager.allTargets.distinct(), WhatsAppCleanerManager.allTargets)
    }

    @Test
    fun scanOutputIsParsedPerTargetAndAge() {
        val scans = WhatsAppCleanerManager.parseScan(
            "ROOT\nIMAGES/RECEIVED 12 4 96 5 2 0 1 0 300 0 0 0\nIMAGES/SENT 0 0 0 0 0 0 0 0 0 0 0 0\n" +
                "VOICE_NOTES/RECEIVED 3 0 900 3 0 900 0 0 0 0 0 0"
        )
        val anyAge = scans.getValue(0)

        assertEquals(WhatsAppCleanerManager.AGE_OPTIONS_DAYS, scans.keys.toList())
        assertTrue(anyAge.isMediaFolderFound)
        assertEquals(WhatsAppMediaUsage(12, 4096L), anyAge.usage[receivedImages])
        assertEquals(listOf(receivedImages, voiceNotes), anyAge.targetsWithFiles())
        assertEquals(WhatsAppMediaUsage(15, 4996L), anyAge.usageOf(listOf(receivedImages, sentImages, voiceNotes)))
        assertEquals(WhatsAppMediaUsage(8, 2900L), scans.getValue(30).total)
        assertEquals(listOf(receivedImages), scans.getValue(180).targetsWithFiles())
        assertTrue(scans.getValue(365).targetsWithFiles().isEmpty())
    }

    @Test
    fun foldersLargerThanTheShellIntegerRangeKeepTheirSize() {
        val scans = WhatsAppCleanerManager.parseScan(
            "ROOT\nIMAGES/PRIVATE 6000 3500000 123 0 0 0 0 0 0 0 0 0"
        )
        val privateImages = WhatsAppMediaTarget(WhatsAppMediaCategory.IMAGES, WhatsAppMediaPart.PRIVATE)
        val script = WhatsAppCleanerManager.scanScript()

        assertEquals(WhatsAppMediaUsage(6000, 3_500_000_123L), scans.getValue(0).usage[privateImages])
        assertFalse(script.contains("+s))"))
        assertTrue(script.contains("p=000\$s; h=\${p%???}; l=\${p#\"\$h\"};"))
    }

    @Test
    fun unreadableScanOutputYieldsEmptyScans() {
        val scans = WhatsAppCleanerManager.parseScan(
            "Shizuku permission not granted\nIMAGES/RECEIVED 1 1\nIMAGES/SENT x 1 0 0 0 0 0 0 0 0 0 0\n" +
                "UNKNOWN/SENT 1 1 0 0 0 0 0 0 0 0 0 0"
        )

        assertEquals(WhatsAppCleanerManager.AGE_OPTIONS_DAYS, scans.keys.toList())
        scans.values.forEach { scan ->
            assertFalse(scan.isMediaFolderFound)
            assertTrue(scan.usage.isEmpty())
            assertEquals(WhatsAppMediaUsage.NONE, scan.total)
        }
    }

    @Test
    fun receivedFilesOfSplitCategoriesAreNotSearchedInSubfolders() {
        val script = WhatsAppCleanerManager.deleteScript(listOf(receivedImages, sentImages, voiceNotes), 0)

        assertTrue(script.contains("'/sdcard/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images' '/sdcard/WhatsApp/Media/WhatsApp Images'; do if [ -d \"\$d\" ]; then find \"\$d\" -maxdepth 1 -type f"))
        assertTrue(script.contains("/WhatsApp Images/Sent'; do if [ -d \"\$d\" ]; then find \"\$d\" -mindepth 1 -type f"))
        assertTrue(script.contains("/WhatsApp Voice Notes'; do if [ -d \"\$d\" ]; then find \"\$d\" -mindepth 1 -type f"))
        assertFalse(script.contains("-mtime"))
    }

    @Test
    fun deleteAppliesTheAgeFilterAndScanBucketsEveryAgeInOnePass() {
        val delete = WhatsAppCleanerManager.deleteScript(listOf(receivedImages), 30)
        val scan = WhatsAppCleanerManager.scanScript()

        assertTrue(delete.contains("-type f ! -name .nomedia -mtime +30 -exec rm"))
        assertTrue(scan.contains("-type f ! -name .nomedia -exec stat"))
        assertFalse(scan.contains("-mtime"))
        WhatsAppCleanerManager.AGE_OPTIONS_DAYS.filter { it > 0 }.forEach { days ->
            assertTrue(scan.contains("if [ \$a -gt $days ]"))
        }
    }

    @Test
    fun statusesAndDocumentsAreSplitByFileKind() {
        val statusPhotos = WhatsAppMediaTarget(WhatsAppMediaCategory.STATUSES, WhatsAppMediaPart.RECEIVED, WhatsAppMediaKind.PHOTOS)
        val statusOther = WhatsAppMediaTarget(WhatsAppMediaCategory.STATUSES, WhatsAppMediaPart.RECEIVED, WhatsAppMediaKind.OTHER)
        val sentPdf = WhatsAppMediaTarget(WhatsAppMediaCategory.DOCUMENTS, WhatsAppMediaPart.SENT, WhatsAppMediaKind.PDF)
        val scans = WhatsAppCleanerManager.parseScan(
            "ROOT\nSTATUSES/RECEIVED/PHOTOS 4 0 800 0 0 0 0 0 0 0 0 0\nDOCUMENTS/SENT/PDF 1 2 0 0 0 0 0 0 0 0 0 0"
        )

        assertEquals(3, WhatsAppMediaCategory.STATUSES.targets.size)
        assertEquals(12, WhatsAppMediaCategory.DOCUMENTS.targets.size)
        assertEquals(listOf(sentPdf, statusPhotos), scans.getValue(0).targetsWithFiles())
        assertTrue(
            WhatsAppCleanerManager.deleteScript(listOf(statusPhotos), 0).contains(
                "/Media/.Statuses'; do if [ -d \"\$d\" ]; then find \"\$d\" -mindepth 1 -type f ! -name .nomedia " +
                    "\\( -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.png' -o -iname '*.webp' \\) -exec rm"
            )
        )
        assertTrue(
            WhatsAppCleanerManager.deleteScript(listOf(statusOther), 0)
                .contains("! -name .nomedia ! \\( -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.png' -o -iname '*.webp' -o -iname '*.mp4'")
        )
        assertTrue(
            WhatsAppCleanerManager.deleteScript(listOf(sentPdf), 30)
                .contains("/WhatsApp Documents/Sent'; do if [ -d \"\$d\" ]; then find \"\$d\" -mindepth 1 -type f ! -name .nomedia \\( -iname '*.pdf' \\) -mtime +30 -exec rm")
        )
    }

    @Test
    fun onlyDatedChatBackupsAreEverTouchedInDatabases() {
        val backups = WhatsAppMediaCategory.OLD_BACKUPS.targets
        val scripts = listOf(WhatsAppCleanerManager.deleteScript(backups, 0), WhatsAppCleanerManager.scanScript())

        assertEquals(1, backups.size)
        assertFalse(WhatsAppMediaCategory.OLD_BACKUPS.isMedia)
        scripts.forEach { script ->
            script.lines().filter { it.contains("/Databases'") }.forEach { line ->
                assertTrue(line.contains("! -name .nomedia \\( -iname 'msgstore-*' \\) -"))
            }
        }
        assertTrue(scripts.all { it.contains("'/sdcard/WhatsApp/Databases'") })
        assertTrue(scripts.last().contains("'/sdcard/Android/media/com.whatsapp/WhatsApp/.Shared'"))
    }

    @Test
    fun scanAndDeleteFailSafelyWithoutShizuku() {
        assertTrue(WhatsAppCleanerManager.scan().values.none { it.isMediaFolderFound })
        assertTrue(WhatsAppCleanerManager.scan().values.all { it.usage.isEmpty() })
        assertFalse(WhatsAppCleanerManager.delete(listOf(receivedImages), 0))
        assertFalse(WhatsAppCleanerManager.delete(emptyList(), 0))
    }
}
