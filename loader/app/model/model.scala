package model

import com.amazonaws.services.ec2.model.ReservedInstances
import org.joda.time.DateTime

object SmallestInClass {

  private val smallestTypeInClass = Map(
    "m1" -> "small",
    "m2" -> "xlarge",
    "m3" -> "medium",
    "c1" -> "medium",
    "c3" -> "large",
    "r3" -> "large",
    "g2" -> "2xlarge",
    "i2" -> "xlarge",
    "hs1" -> "8xlarge"
  ).withDefaultValue("large")

  def apply(instanceClass: String): String = smallestTypeInClass(instanceClass)

}

object InstanceSizeNormalisationFactor {

  // this list taken from http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ri-modification-instancemove.html
  // TODO: micro is now 0.5 becuase of t2's!
  private val instanceSizeToFactor = Map(
    "small" -> 1,
    "medium" -> 2,
    "large" -> 4,
    "xlarge" -> 8,
    "2xlarge" -> 16,
    "4xlarge" -> 32,
    "8xlarge" -> 64
  ).withDefaultValue(0)

  def apply(instanceSize: String): Int = instanceSizeToFactor(instanceSize)


  def combosFor(totalFactor: Int): Stream[List[String]] = {

    def traverse(acc: List[String], remaining: Int): Stream[List[String]] = {
      if (remaining == 0) {
        if (acc.isEmpty) Stream.empty else Stream(acc)
      }
      else if (remaining < 0) Stream.empty
      else {
        instanceSizeToFactor.toStream
          .filter { case (_, size) => size <= remaining }
          .flatMap { case (sizeName, size) =>
            traverse(sizeName :: acc, remaining - size)
          }
      }
    }

    traverse(Nil, totalFactor)
  }
}


sealed trait NetworkClass {
  def platformName: String
}
case object Classic extends NetworkClass {
  val platformName = "EC2-Classic"
}
case object VPC extends NetworkClass {
  val platformName = "EC2-VPC"
}




case class Reservation
(
  criteria: ReservationCriteria,
  endDate: DateTime,
  reservationId: String
) {

  // you can merge reservations where the hour is the same, even if the seconds differ
  // i.e. where this roundedEndDate is the same
  def roundedEndDate = endDate.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)

  def points = criteria.points

  override def toString = s"$reservationId ($criteria) expires $endDate"
}

object Reservation {

  // TODO: I think if we keep non-active reservations around too, then
  //  we may be able to ask the question "what reservations were active on
  //  date X, so we can see / graph historical changes
  def fromAws(r: ReservedInstances): List[Reservation] = {
    if (r.getState != "active") Nil
    else {
      val criteria = ReservationCriteria(
        InstanceType.fromString(r.getInstanceType),
        r.getAvailabilityZone,
        Classic,
        r.getInstanceCount.toInt
      )

      List(Reservation(
        criteria,
        new DateTime(r.getEnd),
        r.getReservedInstancesId
      )
      )
    }
  }
}


