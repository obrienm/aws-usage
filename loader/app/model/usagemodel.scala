package model

import org.joda.time.{DateTime, Duration}

import scala.collection.GenSeq


case class Usage
(
  instanceType: InstanceType,
  az: String,
  networkClass: NetworkClass,
  startDate: DateTime,
  endDate: DateTime,
  quantity: Int,

  // and these bits for info only
  wasReserved: Boolean,
  hourlyCost: BigDecimal

  ) {
  def durationHours = new Duration(startDate, endDate).getStandardHours
  require(durationHours == 1, s"durationHours was $durationHours, expected 1")

  def region = az dropRight 1

  def reservationCriteria = ReservationCriteria(instanceType, az, networkClass, quantity)

  def reservedString = if (wasReserved) "RESERVED" else "ON-DEMAND"
  def summary = s"$quantity * ${instanceType.name} in $az ($networkClass) $reservedString $$$hourlyCost"
}

case class UsagesByRegion(region: String, usages: List[Usage]) {
  def forInstanceClass(instanceClass: String) =
    usages.filter(_.instanceType.instanceClass == instanceClass)
}

object Usage {
  def prettyPrint(usages: List[Usage]) {
    aggregate(usages)
      .sortBy(u => s"${u.instanceType} ${u.az} ${u.networkClass} ${u.reservedString}")
      .foreach(u => println(u.summary))

  }

  def aggregate(usages: GenSeq[Usage]): List[Usage] = {
    usages.groupBy(identity)
      .values
      .map (l => l.head.copy(quantity = l.map(_.quantity).sum))
      .toList
  }
}