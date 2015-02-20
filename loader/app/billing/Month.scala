package billing

import org.joda.time.DateTime

case class Month(month: Int, year: Int) {
  override def toString = "%02d-%04d".format(month, year)
}

object Month {
  def thisMonth = {
    val now = DateTime.now
    Month(now.getMonthOfYear, now.getYear)
  }
}
