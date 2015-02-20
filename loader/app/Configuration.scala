package conf

import java.io.File

import com.typesafe.config.{ConfigException, ConfigFactory}

object Configuration {
  private lazy val configFile = new File(sys.props("user.home"), ".ribot")

  private lazy val configDataFromFile = ConfigFactory.parseFile(configFile)

  private lazy val configDataFromEnv = ConfigFactory.systemEnvironment()

  private lazy val configData = configDataFromEnv withFallback configDataFromFile

  lazy val billingS3Bucket = getMandatory("S3_BUCKET")

  private def getMandatory(key: String): String = try {
    configData.getString(key)
  } catch {
    case e: ConfigException =>
      sys.error(s"expecting an entry $key=???? in environment variables or in ${configFile.getCanonicalPath}")
  }
}
