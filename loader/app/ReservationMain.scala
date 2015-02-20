import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.ec2.AmazonEC2Client
import model.Reservation
import scala.collection.JavaConverters._

object ReservationMain /*extends App*/ {
  println("Getting reservations")

  val regions = List(Regions.EU_WEST_1) map Region.getRegion

  val creds = new ProfileCredentialsProvider("billing")

  for (region <- regions) {
    val ec2 = region.createClient(classOf[AmazonEC2Client], creds, null)

    println(s"** region ${region.getName} **")

    val rawResult = ec2.describeReservedInstances()

    val reservation = rawResult.getReservedInstances.asScala
      .flatMap(Reservation.fromAws)
      .find(_.reservationId == "f127bd27-0a15-4331-906e-39bc569baaa8")
      .get

    println(reservation.toString)
    println(reservation.points)

//      .groupBy(_.roundedEndDate)
//
//    for ((dt, res) <- resGroups) {
//      println(s"  End date: $dt")
//
//      for ((instanceClass, rrr) <- res.groupBy(_.criteria.instanceType.instanceClass).toList.sortBy(_._1)) {
//        println(s"      class $instanceClass:")
//        rrr.foreach(r => println(s"          $r"))
//
//         // ${rrr.map(r => r.criteria.instanceType.sizeNormalistionFactor * r.numInstances).sum}")
//      }
//    }

  }




}
