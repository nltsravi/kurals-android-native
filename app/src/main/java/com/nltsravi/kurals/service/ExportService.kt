package com.nltsravi.kurals.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.nltsravi.kurals.constants.AppConstants
import com.nltsravi.kurals.model.ExportFormat
import com.nltsravi.kurals.model.Kural
import com.nltsravi.kurals.util.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExportService {

    fun escapeHtml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#039;")
    }

    fun escapeXml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Generates the full printable HTML document (matching `generatePrintableHtml` in `exportService.ts`)
     */
    fun generatePrintableHtml(
        title: String,
        subtitle: String,
        kurals: List<Kural>
    ): String {
        val kuralItemsHtml = kurals.joinToString("\n") { k ->
            val (line1, line2) = TextUtils.formatKuralCouplet(k.line1, k.line2)
            """
            <div class="kural-card">
              <div class="kural-header">
                <span class="kural-badge">குறள் #${k.number} • ${escapeHtml(k.chapterNameTamil)} (${escapeHtml(k.chapterNameEnglish)})</span>
              </div>
              <div class="couplet-tamil">
                <div>${escapeHtml(line1)}</div>
                <div>${escapeHtml(line2)}</div>
              </div>
              <div class="couplet-translit">
                <div>${escapeHtml(k.transliteration1)}</div>
                <div>${escapeHtml(k.transliteration2)}</div>
              </div>
            </div>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html lang="ta">
        <head>
          <meta charset="UTF-8" />
          <title>${escapeHtml(title)}</title>
        </head>
        <body>
          <div class="doc-header">
            <div class="doc-header-sub">திருக்குறள்கள்</div>
            <h1 class="doc-header-title">${escapeHtml(title)}</h1>
            <p>${escapeHtml(subtitle)}</p>
          </div>
          <div class="kural-list">
            $kuralItemsHtml
          </div>
          <div class="doc-footer">
            <span>${escapeHtml(AppConstants.COPYRIGHT_TEXT)}</span>
          </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * Generates a valid OpenXML `.docx` ZIP package as a ByteArray
     */
    fun generateDocxBytes(
        title: String,
        subtitle: String,
        kurals: List<Kural>
    ): ByteArray {
        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/header1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"/>
  <Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""

        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        val wordRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/header" Target="header1.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer1.xml"/>
</Relationships>"""

        val headerXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:hdr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:p>
    <w:pPr>
      <w:pBdr>
        <w:bottom w:val="single" w:sz="12" w:space="6" w:color="FF7C0A"/>
      </w:pBdr>
      <w:spacing w:after="160"/>
    </w:pPr>
    <w:r>
      <w:rPr>
        <w:b/>
        <w:color w:val="D97706"/>
        <w:sz w:val="20"/>
      </w:rPr>
      <w:t>திருக்குறள்கள்  |  </w:t>
    </w:r>
    <w:r>
      <w:rPr>
        <w:b/>
        <w:color w:val="1F2937"/>
        <w:sz w:val="22"/>
      </w:rPr>
      <w:t>${escapeXml(title)}</w:t>
    </w:r>
  </w:p>
</w:hdr>"""

        val footerXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:ftr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:p>
    <w:pPr>
      <w:pBdr>
        <w:top w:val="single" w:sz="6" w:space="4" w:color="E5E7EB"/>
      </w:pBdr>
      <w:tabs>
        <w:tab w:val="right" w:pos="9026"/>
      </w:tabs>
    </w:pPr>
    <w:r>
      <w:rPr>
        <w:color w:val="6B7280"/>
        <w:sz w:val="18"/>
      </w:rPr>
      <w:t>${escapeXml(AppConstants.COPYRIGHT_TEXT)}</w:t>
    </w:r>
    <w:r>
      <w:tab/>
    </w:r>
    <w:r>
      <w:rPr>
        <w:b/>
        <w:color w:val="6B7280"/>
        <w:sz w:val="18"/>
      </w:rPr>
      <w:t xml:space="preserve">Page </w:t>
    </w:r>
    <w:fldSimple w:instr="PAGE">
      <w:r>
        <w:rPr>
          <w:b/>
          <w:color w:val="6B7280"/>
          <w:sz w:val="18"/>
        </w:rPr>
        <w:t>1</w:t>
      </w:r>
    </w:fldSimple>
  </w:p>
</w:ftr>"""

        val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
    <w:rPr>
      <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Latha"/>
      <w:sz w:val="22"/>
    </w:rPr>
  </w:style>
</w:styles>"""

        val kuralParagraphs = kurals.joinToString("\n") { k ->
            val (line1, line2) = TextUtils.formatKuralCouplet(k.line1, k.line2)
            """
            <w:p>
              <w:pPr><w:spacing w:before="200" w:after="60"/></w:pPr>
              <w:r>
                <w:rPr><w:b/><w:color w:val="B45309"/><w:sz w:val="20"/></w:rPr>
                <w:t>குறள் #${k.number} • ${escapeXml(k.chapterNameTamil)} (${escapeXml(k.chapterNameEnglish)})</w:t>
              </w:r>
            </w:p>
            <w:p>
              <w:pPr><w:spacing w:before="40" w:after="20"/></w:pPr>
              <w:r>
                <w:rPr><w:b/><w:color w:val="111827"/><w:sz w:val="24"/></w:rPr>
                <w:t>${escapeXml(line1)}</w:t>
              </w:r>
            </w:p>
            <w:p>
              <w:pPr><w:spacing w:before="20" w:after="60"/></w:pPr>
              <w:r>
                <w:rPr><w:b/><w:color w:val="111827"/><w:sz w:val="24"/></w:rPr>
                <w:t>${escapeXml(line2)}</w:t>
              </w:r>
            </w:p>
            <w:p>
              <w:pPr><w:spacing w:before="20" w:after="20"/></w:pPr>
              <w:r>
                <w:rPr><w:i/><w:color w:val="4B5563"/><w:sz w:val="19"/></w:rPr>
                <w:t>${escapeXml(k.transliteration1)}</w:t>
              </w:r>
            </w:p>
            <w:p>
              <w:pPr>
                <w:pBdr>
                  <w:bottom w:val="single" w:sz="4" w:space="6" w:color="FDE68A"/>
                </w:pBdr>
                <w:spacing w:before="20" w:after="140"/>
              </w:pPr>
              <w:r>
                <w:rPr><w:i/><w:color w:val="4B5563"/><w:sz w:val="19"/></w:rPr>
                <w:t>${escapeXml(k.transliteration2)}</w:t>
              </w:r>
            </w:p>
            """.trimIndent()
        }

        val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <w:body>
    $kuralParagraphs
    <w:sectPr>
      <w:headerReference w:type="default" r:id="rId2"/>
      <w:footerReference w:type="default" r:id="rId3"/>
      <w:pgSz w:w="11906" w:h="16838"/>
      <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="720" w:footer="720" w:gutter="0"/>
    </w:sectPr>
  </w:body>
</w:document>"""

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun addEntry(path: String, content: String) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            addEntry("[Content_Types].xml", contentTypesXml)
            addEntry("_rels/.rels", relsXml)
            addEntry("word/_rels/document.xml.rels", wordRelsXml)
            addEntry("word/header1.xml", headerXml)
            addEntry("word/footer1.xml", footerXml)
            addEntry("word/styles.xml", stylesXml)
            addEntry("word/document.xml", documentXml)
        }
        return baos.toByteArray()
    }

    /**
     * Generates a styled A4 multi-page PDF document with header icon, Thiruvalluvar watermark,
     * Kural cards, and copyright + page number footer.
     */
    fun generatePdfFile(
        context: Context,
        title: String,
        subtitle: String,
        kurals: List<Kural>,
        outputFile: File
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 width in PostScript points (72 dpi)
        val pageHeight = 842 // A4 height in PostScript points
        val marginH = 38f
        val kuralsPerPage = 5
        val chunks = if (kurals.isEmpty()) listOf(emptyList()) else kurals.chunked(kuralsPerPage)

        val iconBitmap: Bitmap? = try {
            context.assets.open("images/icon.png").use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
        val watermarkBitmap: Bitmap? = try {
            context.assets.open("images/thiruvalluvar-outline.png").use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }

        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D97706")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF7C0A")
            strokeWidth = 2f
        }
        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFDF9")
            style = Paint.Style.FILL
        }
        val cardStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FDE68A")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val badgeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FEF3C7")
            style = Paint.Style.FILL
        }
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B45309")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tamilCoupletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val translitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        val footerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1f
        }
        val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B7280")
            textSize = 9f
        }
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = 16 // ~6% opacity
        }

        try {
            chunks.forEachIndexed { pageIdx, pageKurals ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIdx + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                // 1. Watermark centered
                if (watermarkBitmap != null) {
                    val wmWidth = 240
                    val wmHeight = (wmWidth.toFloat() * watermarkBitmap.height / watermarkBitmap.width).toInt()
                    val left = (pageWidth - wmWidth) / 2
                    val top = (pageHeight - wmHeight) / 2
                    canvas.drawBitmap(
                        watermarkBitmap,
                        null,
                        Rect(left, top, left + wmWidth, top + wmHeight),
                        watermarkPaint
                    )
                }

                // 2. Top Header
                var headerTextX = marginH
                if (iconBitmap != null) {
                    val iconRect = RectF(marginH, 28f, marginH + 30f, 58f)
                    canvas.drawBitmap(iconBitmap, null, iconRect, null)
                    headerTextX = marginH + 38f
                }
                canvas.drawText("திருக்குறள்கள்  •  $subtitle", headerTextX, 39f, headerSubPaint)
                canvas.drawText(title, headerTextX, 55f, headerTitlePaint)
                canvas.drawLine(marginH, 66f, pageWidth - marginH, 66f, headerDividerPaint)

                // 3. Kural Cards
                var currentY = 82f
                val cardWidth = pageWidth - marginH * 2
                val cardHeight = 124f

                for (kural in pageKurals) {
                    val (line1, line2) = TextUtils.formatKuralCouplet(kural.line1, kural.line2)
                    val cardRect = RectF(marginH, currentY, marginH + cardWidth, currentY + cardHeight)
                    canvas.drawRoundRect(cardRect, 8f, 8f, cardFillPaint)
                    canvas.drawRoundRect(cardRect, 8f, 8f, cardStrokePaint)

                    val badgeText = "குறள் #${kural.number} • ${kural.chapterNameTamil} (${kural.chapterNameEnglish})"
                    val badgeWidth = badgeTextPaint.measureText(badgeText) + 18f
                    val badgeRect = RectF(marginH + 12f, currentY + 10f, marginH + 12f + badgeWidth, currentY + 28f)
                    canvas.drawRoundRect(badgeRect, 10f, 10f, badgeFillPaint)
                    canvas.drawText(badgeText, marginH + 21f, currentY + 22.5f, badgeTextPaint)

                    // Tamil Couplet (2 lines)
                    canvas.drawText(line1, marginH + 14f, currentY + 48f, tamilCoupletPaint)
                    canvas.drawText(line2, marginH + 14f, currentY + 66f, tamilCoupletPaint)

                    // Divider inside card
                    canvas.drawLine(
                        marginH + 14f,
                        currentY + 76f,
                        marginH + cardWidth - 14f,
                        currentY + 76f,
                        footerLinePaint
                    )

                    // Transliteration (2 lines)
                    canvas.drawText(kural.transliteration1, marginH + 14f, currentY + 93f, translitPaint)
                    canvas.drawText(kural.transliteration2, marginH + 14f, currentY + 109f, translitPaint)

                    currentY += cardHeight + 12f
                }

                // 4. Footer
                val footerY = pageHeight - 34f
                canvas.drawLine(marginH, footerY - 12f, pageWidth - marginH, footerY - 12f, footerLinePaint)
                canvas.drawText(AppConstants.COPYRIGHT_TEXT, marginH, footerY, footerTextPaint)
                val pageLabel = "Page ${pageIdx + 1} of ${chunks.size}"
                val pageLabelWidth = footerTextPaint.measureText(pageLabel)
                canvas.drawText(pageLabel, pageWidth - marginH - pageLabelWidth, footerY, footerTextPaint)

                pdfDocument.finishPage(page)
            }

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
        } finally {
            pdfDocument.close()
        }
        return outputFile
    }

    /**
     * Exports a list of Kurals to either PDF or DOCX in `cacheDir/exports` and launches native share sheet.
     */
    suspend fun exportKuralsDocument(
        context: Context,
        title: String,
        subtitle: String,
        kurals: List<Kural>,
        format: ExportFormat
    ): File = withContext(Dispatchers.IO) {
        val safeName = TextUtils.sanitizeFilename(title)
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }

        val outFile = if (format == ExportFormat.PDF) {
            val pdfFile = File(exportsDir, "$safeName.pdf")
            generatePdfFile(context, title, subtitle, kurals, pdfFile)
        } else {
            val docxFile = File(exportsDir, "$safeName.docx")
            val bytes = generateDocxBytes(title, subtitle, kurals)
            FileOutputStream(docxFile).use { it.write(bytes) }
            docxFile
        }

        withContext(Dispatchers.Main) {
            val mimeType = if (format == ExportFormat.PDF) {
                "application/pdf"
            } else {
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            }
            ShareService.shareExportedFile(
                context = context,
                file = outFile,
                mimeType = mimeType,
                chooserTitle = "Export $title (${format.name})"
            )
        }

        outFile
    }
}

