package boriskin

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._

class PutSimulation extends Simulation {

  private def getProperty(propertyName: String,
                          defaultValue: String) = {
    Option(System.getenv(propertyName))
      .orElse(Option(System.getProperty(propertyName)))
      .getOrElse(defaultValue)
  }

  def userCount: Int =
    getProperty("USERS", "100").toInt
  def rampDuration: Int =
    getProperty("RAMP_DURATION", "20").toInt
  def testDuration: Int =
    getProperty("DURATION", "180").toInt

  before {
    println(s"Running test with ${userCount} users")
    println(s"Ramping users over ${rampDuration} seconds")
    println(s"Total test duration: ${testDuration} seconds")
  }

  val httpConf: HttpProtocolBuilder =
    http.baseUrl("http://localhost:8080")

  val scn: ScenarioBuilder = scenario("putRequests")
    .forever("key") {
      exec(
        http("PUT")
          .put("/v0/entity?id=${key}")
          .body(StringBody { "${key}" })
      )
    }

  setUp(
    scn.inject(
      nothingFor(5 seconds),
      rampUsers(userCount) during (rampDuration second)
    )
  ).protocols(httpConf)
    .maxDuration(testDuration seconds)
}
