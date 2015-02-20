package billing

import model.{Usage, NetworkClass, InstanceType}
import org.joda.time.format.DateTimeFormat

// represents the standard data provided by a row within the
// aws-billing-detailed-line-items-with-resources-and-tags csv file provided by amazon
class BillingCsvRow(val values: Map[String, String], lineNumber: Long) {
  // always "Estimated" for this months invoice
  def invoiceId = values("InvoiceID")

  // PayerAccountId: the payer account (always where I downloaded it from)
  def payerAccountId = values("PayerAccountId")

  // LinkedAccountId: the account the cost was incurred
  def linkedAccountId = values("LinkedAccountId")

  // RecordType: LineItem for raw data, AccountTotal for account totals, StatementTotal, Rounding, InvoiceTotal
  def recordType = values("RecordType")

  // RecordId: Unique id of this record
  def recordId = values("RecordId")

  // ProductName: "Amazon Elastic Compute Cloud" for EC2
  def productName = values("ProductName")

  // RateId: ????  - looks like it's something to do with the pricing
  def rateId = values("RateId")

  // SubscriptionId: ????
  def subscriptionId = values("SubscriptionId")

  // PricingPlanId: ??? - like like something to do with the kind of thing, 474748 look like box usage
  def pricingPlanId = values("PricingPlanId")

  // UsageType: [EU-]BoxUsage[:box-size] = on demand; Same but with HeavyUsage = reservation
  //  sometimes the box-size is in here, sometimes you have to parse it out of "ItemDescription"
  //  nope, according to ice (and by my inspection) if not present it's m1.small
  def usageType = values("UsageType")

  // Operation: "RunInstances" == running an ec2 instance
  def operation = values("Operation")

  // AvailabilityZone: the az
  def availabilityZone = values("AvailabilityZone")

  // ReservedInstance: "Y" == it was reserved, "N" == it was not reserved
  def reservedInstance = values("ReservedInstance") == "Y"

  // ItemDescription: seems pretty formulaic
  def itemDescription = values("ItemDescription")

  // UsageStartDate: date time of start,
  def usageStartDate = BillingCsvRow.awsDateTimeFormat.parseDateTime(values("UsageStartDate"))

  // UsageEndDate: date time of end,
  def usageEndDate = BillingCsvRow.awsDateTimeFormat.parseDateTime(values("UsageEndDate"))

  // UsageQuantity: looks like always 1.0 for instances; and start & end always looks like one hour,
  //   you appear to get summary entries for the overall reservations too, but they appear to duplicate tha
  //   actual instance usages.
  def usageQuantity = BigDecimal(values("UsageQuantity"))

  // BlendedRate,
  def blendedRate = BigDecimal(values("BlendedRate"))

  // BlendedCost,
  def blendedCost = BigDecimal(values("BlendedCost"))

  // UnBlendedRate,
  def unblendedRate = BigDecimal(values("UnBlendedRate"))

  // UnBlendedCost,
  def unblendedCost = BigDecimal(values("UnBlendedCost"))

  // ResourceId: for 1.0 entries, the actual instance id,
  def resourceId = values("ResourceId")


  def instanceTypeAsString = usageType.split(":") match {
    case Array(_, size) => size
    case other => "m1.small"
  }
  
  def instanceType = InstanceType.fromString(instanceTypeAsString)

  def isEc2InstanceUsage =
    // just ec2
    productName == "Amazon Elastic Compute Cloud" &&
    // just instance usage, not bandwidth
    usageType.contains("Usage") &&
    // running instances only
    operation == "RunInstances"

  def isActuallyAnInstanceUsage =
    isEc2InstanceUsage &&
    // for heavy reserved instances, you get both the actual usage (for one hour)
    // and a overall monthly "unused" cost. The unused cost doesn't have an availability zone.
    availabilityZone != null

  def asUsage(networkClass: NetworkClass) = Usage(
    instanceType = instanceType,
    az = availabilityZone,
    networkClass = networkClass,
    startDate = usageStartDate,
    endDate = usageEndDate,
    quantity = usageQuantity.toInt,

    wasReserved = reservedInstance,
    hourlyCost = unblendedRate
  )

  override def toString = values.toString()
}

object BillingCsvRow {
  // "2014-07-01 00:00:00"
  val awsDateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withZoneUTC()
}

