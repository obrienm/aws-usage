package billing

import java.io.{File, InputStreamReader, Reader}
import java.util.zip.ZipFile

import log.ClassLogger
import org.supercsv.comment.CommentMatcher
import org.supercsv.io.CsvMapReader
import org.supercsv.prefs.CsvPreference

import scala.collection.convert.decorateAll._

object BillingCsvReader extends ClassLogger {

  def parseZip(zipFile: File) = {
    val zip = new ZipFile(zipFile)
    val first = zip.entries().asScala.next()
    parse(new InputStreamReader(zip.getInputStream(first), "US-ASCII"))
  }

  private class CommentEverythingNotEc2Related extends CommentMatcher {
    var isFirstLine = true

    override def isComment(line: String): Boolean = {

      val isInterestingDataLine =
        (line contains "Amazon Elastic Compute Cloud") &&
        (line contains "Usage") &&
        (line contains "RunInstances")

      val result = !isFirstLine && !isInterestingDataLine

      isFirstLine = false

      result
    }

  }

  def parse(reader: Reader): Stream[BillingCsvRow] = {
    val csvPrefs = new CsvPreference.Builder(CsvPreference.STANDARD_PREFERENCE).build()
    val csvReader = new CsvMapReader(reader, csvPrefs)
    val headers = csvReader.getHeader(true)

    def streamUntilEof(): Stream[BillingCsvRow] = {
      val line = csvReader.read(headers: _*)
      if (line == null) Stream.empty
      else new BillingCsvRow(line.asScala.toMap, csvReader.getLineNumber) #:: streamUntilEof()
    }
    streamUntilEof()
  }
}
