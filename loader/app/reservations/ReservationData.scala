package reservations

import java.util.concurrent.TimeUnit

import aws.AWS
import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.ec2.AmazonEC2Client
import com.google.common.cache.{CacheBuilder, CacheLoader}
import com.google.common.util.concurrent.ListenableFuture
import log.ClassLogger
import model.Reservation
import scala.collection.JavaConverters._
import scala.collection.GenSeq

case class ReservationData(region: String, all: GenSeq[Reservation]) {

  def forClass(instanceClass: String) = copy(
    all = all.filter(_.criteria.instanceType.instanceClass == instanceClass)
  )

  def forType(instanceClass: String, instanceSize: String) = copy(
    all = all.filter(_.criteria.instanceType.instanceClass == instanceClass)
      .filter(_.criteria.instanceType.instanceSize == instanceSize)
  )


  def totalPoints = all.map(_.points).sum

}


object ReservationData extends ClassLogger {

  private def loadData(regionName: String): ReservationData = {
    val region = Region.getRegion(Regions.fromName(regionName))

    val ec2 = region.createClient(classOf[AmazonEC2Client], AWS.credentials, null)

    val all = ec2.describeReservedInstances()
      .getReservedInstances.asScala
      .flatMap(Reservation.fromAws)
      .toList

    ReservationData(regionName, all)
  }

  private object Loader extends CacheLoader[String, ReservationData] {
    override def load(region: String) = logAround(s"Loading reservation data for $region") {
      loadData(region)
    }

    // TODO: implement this so we load async
    override def reload(key: String, oldValue: ReservationData): ListenableFuture[ReservationData] =
      super.reload(key, oldValue)
  }


  private lazy val cache = CacheBuilder.newBuilder()
    .refreshAfterWrite(1, TimeUnit.HOURS)
    .build(Loader)

  def apply(regionName: String) = cache(regionName)
}