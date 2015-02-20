import java.io.File

import billing.{BillingCsvReader, BillingCsvRow}
import log.ClassLogger
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import play.api.GlobalSettings
import play.api.libs.json.Json
import play.api.test._

import scala.util.Try

object Parser extends App {

  Helpers.running(FakeApplication(withGlobal = Some(new GlobalSettings {}))) {
    AwsUsage.createIndex

    val filename = "loader/data/362307275615-aws-billing-detailed-line-items-with-resources-and-tags-2015-01.csv.zip"
    BillingCsvReader.parseZip(new File(filename)).foreach(ElasticSearch.insert)
  }
}

object ElasticSearch extends ClassLogger {

  val client = new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost", 9300))

  def insert(billingCsvRow: BillingCsvRow) = {

    log.info(s"inserting record ${billingCsvRow.recordId}")

    def populateRow = {
      Try {
        Json.obj(
          "invoiceID" -> billingCsvRow.invoiceId,
          "payerAccountId" -> billingCsvRow.payerAccountId,
          "linkedAccountId" -> billingCsvRow.linkedAccountId,
          "recordType" -> billingCsvRow.recordType,
          "rateId" -> billingCsvRow.rateId,
          "subscriptionId" -> billingCsvRow.subscriptionId,
          "pricingPlanId" -> billingCsvRow.pricingPlanId,
          "operation" -> billingCsvRow.operation,
          "availabilityZone" -> billingCsvRow.availabilityZone,
          "reservedInstance" -> billingCsvRow.reservedInstance,
          "productName" -> billingCsvRow.productName,
          "itemDescription" -> billingCsvRow.itemDescription,
          "usage" -> Json.obj(
            "type" -> billingCsvRow.recordType,
            "from" -> billingCsvRow.usageStartDate,
            "to" -> billingCsvRow.usageEndDate,
            "value" -> billingCsvRow.usageQuantity
          ),
          "cost" -> Json.obj(
            "blended" -> billingCsvRow.blendedCost,
            "blendedRate" -> billingCsvRow.blendedRate,
            "unblended" -> billingCsvRow.unblendedCost,
            "unblendedRate" -> billingCsvRow.unblendedRate
          ),
          "resourceId" -> billingCsvRow.resourceId
        )
      }.toOption
    }

    populateRow.foreach { row =>
      client.prepareIndex(AwsUsage.index, AwsUsage.indexType, billingCsvRow.recordId)
        .setSource(Json.toJson(row).toString())
        .execute()
        .get()
    }
  }

}

object AwsUsage {

  lazy val index = "aws-usage"
  lazy val indexType = "billing"

  def create = {
    ElasticSearch.client.admin()
      .indices().prepareCreate(index)
      .addMapping(indexType, Json.stringify(mapping))
      .get()
  }

  def delete: Unit = {
    if (ElasticSearch.client.admin().indices().prepareExists(index).get().isExists)
      ElasticSearch.client.admin()
        .indices().prepareDelete(index).get()
  }

  def createIndex = {
    delete
    create
  }

  private val nonAnalysedString = Json.obj("type" -> "string", "index" -> "not_analyzed")

  private val mapping = Json.obj(
    "billing" -> Json.obj(
      "_all" -> Json.obj("enabled" -> "false"),
      "properties" -> Json.obj(
        "invoiceID" -> nonAnalysedString,
        "payerAccountId" -> nonAnalysedString,
        "linkedAccountId" -> nonAnalysedString,
        "recordType" -> nonAnalysedString,
        "productName" -> nonAnalysedString,
        "rateId" -> nonAnalysedString,
        "subscriptionId" -> nonAnalysedString,
        "pricingPlanId" -> nonAnalysedString,
        "operation" -> nonAnalysedString,
        "availabilityZone" -> nonAnalysedString,
        "reservedInstance" -> Json.obj("type" -> "boolean"),
        "itemDescription" -> nonAnalysedString,

        "usage" -> Json.obj(
          "type" -> "object",
          "properties" -> Json.obj(
            "type" -> nonAnalysedString,
            "from" -> Json.obj("type" -> "date", "index_name" -> "dt"),
            "to" -> Json.obj("type" -> "date"),
            "value" -> Json.obj("type" -> "double")
          )
        ),
        "cost" -> Json.obj(
          "type" -> "object",
          "properties" -> Json.obj(
            "blended" -> Json.obj("type" -> "double"),
            "blendedRate" -> Json.obj("type" -> "double"),
            "unblended" -> Json.obj("type" -> "double"),
            "unblendedRate" -> Json.obj("type" -> "double")
          ),
          "resourceId" -> nonAnalysedString,
          "resources" -> Json.obj(
            "type" -> "object",
            "properties" -> Json.obj(
              "aws:autoscaling:groupName" -> nonAnalysedString,
              "aws:cloudformation:stack-id" -> nonAnalysedString,
              "aws:cloudformation:stack-name" -> nonAnalysedString
            )
          )
        )
      )
    )
  )

}
