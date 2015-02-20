package model

case class InstanceType(instanceClass: String, instanceSize: String) {
  val name = s"$instanceClass.$instanceSize"
  def sizeNormalistionFactor = InstanceSizeNormalisationFactor(instanceSize)
}

object InstanceType {
  def fromString(s: String) = s.split('.') match {
    case Array(cls, size) => InstanceType(cls, size)
    case _ => sys.error(s"could not parse instance type $s")
  }

  def classes = List(
    //"t1", "t2",
    "m1", "m2", "m3",
    "c1", "c3",
    "r3",
    "g2",
    "i2",
    "hs1"
  )
}
