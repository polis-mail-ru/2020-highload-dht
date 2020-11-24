package boriskin

import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._

class GetSimulation extends Simulation {

  /********************************************************************/
    /*
    Для демонстрации работы основного функционала и отличий Gatling
    определим основные параметры, с которыми можно запустить симуляцию:
    1. USERS - заданное число симулируемых виртуальных пользователей;
    2. RAMP_DURATION - заданная продолжительность временного окна,
          по которому равномерно распределены заданное количество виртуальных пользователей;
    3. DURATION - заданная общая продолжительность обстрела.
     */

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

  /********************************************************************/

    /*
    Симуляция запросов по ключу
     */

  val httpConf: HttpProtocolBuilder =
    http.baseUrl("http://localhost:8080")

  val scn: ScenarioBuilder = scenario("putRequests")
    .forever("key") {
      exec(
        http("GET")
          .get("/v0/entity?id=${key}")
      )
    }

  /********************************************************************/

    /*
    Блок запуска установок симуляции
     */

  setUp(
    scn.inject(
      nothingFor(5 seconds),
      // Проецирует заданное число пользователей,
      // равномерно распределенных по временному окну заданной продолжительности.
      rampUsers(userCount) during (rampDuration second)
    )
  ).protocols(httpConf)
    .maxDuration(testDuration seconds)
}
