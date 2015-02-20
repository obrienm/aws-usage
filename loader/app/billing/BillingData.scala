package billing

import java.util.concurrent.TimeUnit

import com.google.common.cache.{CacheBuilder, CacheLoader}
import com.google.common.util.concurrent.ListenableFuture
import log.ClassLogger
import model.{Classic, Usage}
import org.joda.time.{DateTime, DateTimeZone, LocalDate, LocalTime}

import scala.collection.GenSeq


case class BillingData(raw: GenSeq[Usage], parent: Option[BillingData] = None) {
  lazy val regions = raw.map(_.region).distinct.toList.sorted
  lazy val instanceClasses = raw.map(_.instanceType.instanceClass).distinct.toList.sorted

  lazy val firstDate = raw.map(_.endDate).minBy(_.getMillis)
  lazy val lastDate = raw.map(_.endDate).maxBy(_.getMillis)

  lazy val aggregatedUsage = Usage.aggregate(raw)
    .sortBy(u => u.instanceType.sizeNormalistionFactor -> s" ${u.reservedString} ${u.az} ${u.networkClass}")

  case class HourlyUsagePoints(hour: DateTime, reservedPoints: BigDecimal, ondemandPoints: BigDecimal)

  case class UsagePointsPerType(instType: String, reservedPoints: BigDecimal, ondemandPoints: BigDecimal)

  lazy val pointsPerHour: List[HourlyUsagePoints] = raw
    .groupBy(_.endDate)
    .map { case (hour, usages) =>
      val reservedPoints = usages.filter(_.wasReserved).map(_.reservationCriteria.points).sum
      val ondemandPoints = usages.filterNot(_.wasReserved).map(_.reservationCriteria.points).sum
      HourlyUsagePoints(hour, reservedPoints, ondemandPoints)
    }
    .toList
    .sortBy(_.hour.getMillis)

  lazy val pointsPerType: List[UsagePointsPerType] = raw
    .groupBy(_.instanceType)
    .map { case (instType, usages) =>
    val reservedPoints = usages.filter(_.wasReserved).map(_.reservationCriteria.points).sum
    val ondemandPoints = usages.filterNot(_.wasReserved).map(_.reservationCriteria.points).sum
    UsagePointsPerType(instType.toString, reservedPoints, ondemandPoints)
   }
    .toList
    .sortBy(_.instType)

  // in some cases (e.g. for nav etc) we and to be able to refer to the full set of data
  def global: BillingData = parent.map(_.global).getOrElse(this)

  def filterBy(f: Usage => Boolean) = copy(raw filter f, parent = Some(this))

  def forOneHour(filterDateTime: DateTime): BillingData  = filterBy(_.endDate == filterDateTime)

  def forOneHourYesterdayEvening: BillingData = {
    val yesterdayAtEightPm = new LocalDate().minusDays(1).toDateTime(new LocalTime(20, 0), DateTimeZone.UTC)
    filterBy(_.endDate == yesterdayAtEightPm)

  }

}

object BillingData extends ClassLogger {

  private def loadData(month: Month): BillingData = {

    BillingFileDownloader.billingFileFor(month).map { f =>
      val rawData = BillingCsvReader
        .parseZip(f)
        .filter(_.isEc2InstanceUsage)
        .filter(_.availabilityZone != null)
        // TODO: need to figure out VPS vs Classic
        .map(_.asUsage(Classic))
        .toList

      BillingData(rawData)
    } getOrElse {
      log.warn(s"Could not download billing data for $month")
      BillingData(Nil)
    }
  }

  private object Loader extends CacheLoader[Month, BillingData] {
    override def load(key: Month) = logAround(s"Loading billing data for $key") {
      loadData(key)
    }

    // TODO: implement this so we load async
    override def reload(key: Month, oldValue: BillingData): ListenableFuture[BillingData] =
      super.reload(key, oldValue)
  }

  lazy val cache = CacheBuilder.newBuilder()
    .refreshAfterWrite(60, TimeUnit.MINUTES)
    .build[Month, BillingData](Loader)

  // TODO: we should be able to get the last few months
  def get = cache(Month.thisMonth)
  //def get = cache(Month(7, 2014))

  def apply() = get

}
