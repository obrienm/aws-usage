package model

// This represents a group of reserved instances that are completely mergable and splittable
// (ie. they have the same end date)
case class ReservationGroup(existingReservations: List[Reservation], proposedReservations: List[ReservationCriteria] = Nil) {
  def describeAction: String =
    s"""   Take these reservations
       |     ${existingReservations.map(_.toString).mkString("\n     ")}
       |   and apply like this:
       |     ${changes.mkString("\n     ")}
       |   command line:
       |     $commandLine
       |     """.stripMargin

  val totalPoints = existingReservations.map(_.points).sum
  val spentPoints = proposedReservations.map(_.points).sum
  val sparePoints = totalPoints - spentPoints

  override def toString =
    s"TOTAL: $totalPoints ==> SPENT: $spentPoints, left: $sparePoints"

  lazy val aggReservations = ReservationCriteria.aggregate(proposedReservations)
  def changes = aggReservations.map(_.toString)

  def commandLine =
    s"""
       |aws ec2 modify-reserved-instances \\
       | --reserved-instances-ids ${existingReservations.map(_.reservationId).mkString(" ")} \\
       | --target-configurations \\
       |  $cmdLineTargetConfigs
     """.stripMargin

  def cmdLineTargetConfigs = aggReservations.map(_.awsCliString).mkString(" \\\n  ")

  def spend(proposed: ReservationCriteria): ReservationGroup = {
    this.copy(
      proposedReservations = proposed :: proposedReservations
    )
  }

  def spendSpare(criteria: ReservationCriteria) : ReservationGroup = {

    if(sparePoints > 0){
      val smallestInClass = SmallestInClass(criteria.instanceClass)
      val pointsOfSmallest = InstanceSizeNormalisationFactor(smallestInClass)
      val spareSpend = criteria.copy(
        instanceType=InstanceType(criteria.instanceClass, smallestInClass), numInstances = sparePoints/pointsOfSmallest
      )
      this.copy(
        proposedReservations = spareSpend :: proposedReservations
      )
    }
    else this
  }
}

object ReservationGroup {
  def make(all: List[Reservation]): List[ReservationGroup] =
    all.groupBy(_.roundedEndDate)
      .values
      .toList
      .map(ReservationGroup(_))
}
