# Нагрузочное тестирование при помощи [Gatling](https://gatling.io/).

Содержание отчета:

1. [Gatling - инструмент нагрузочного тестирования](#gatling---инструмент-нагрузочного-тестирования).
2. [Scala-скрипт для симуляции нагрузки](#scala-скрипты-для-симуляции-нагрузки).
3. [Характеристики системы](#характеристики-системы)
4. [Обстрел _PUT_'ами](#обстрел-putами).
5. [Обстрел _GET_'ами](#обстрел-getами).
6. [Обстрел _PUT_+_GET_](#обстрел-put-+-get).
7. [Выводы по работе](#выводы-по-работе).

## Gatling - инструмент нагрузочного тестирования.

_Gatling_ - [популярный][1] инструмент нагрузочного тестирования. Как с [открытым исходным кодом][2], так и с коммерческими [предложениями][3]. В дальнейшем речь пойдет о версии open-source.

Позволяет симулировать [поведение пользователей][4] - абстракции типа: контекст пользователя.

Предоставляет _красивые_ информативные [отчеты][5] по результатам симуляций нагрузки.

## Scala-скрипты для симуляции нагрузки.

_Gatling_ предоставляет возможность записи действий пользователя с помощью инструмента [Recorder][6] для дальнейшего автоматического формирования Scala-скриптов для симуляции нагрузки. Подходит для тестирования E-Commerce веб-сайтов.

В данном случае напишем Scala-скрипты для обстрела _PUT_ и _GET_ запросами (подобно lua-скриптам, использовавшимся ранее для обстрела с помощью утилиты [_wrk2_][7]):

```scala
interp.load.ivy("io.gatling" % "gatling-core" % "3.3.1")
interp.load.ivy("io.gatling" % "gatling-http" % "3.3.1")

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._

class MySimulation extends Simulation {

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
    getProperty("USERS", "5").toInt
  def rampDuration: Int =
    getProperty("RAMP_DURATION", "10").toInt
  def testDuration: Int =
    getProperty("DURATION", "60").toInt

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

  val scn: ScenarioBuilder = scenario("putRequests") // "getRequests"
    .forever("key") {
      exec(
        http("PUT")
          .put("/v0/entity?id=${key}")
          .body(StringBody { "${key}" })
      )
//      exec(
//        http("GET")
//          .get("/v0/entity?id=${key}")
//      )
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
```

В случае _wrk2_ мы открывали 64 соединения и производили обстрел со стабильной нагрузкой в течение заданного промежутка времени.
В данном же случае мы симулируем заданное количество вирутальных пользователей, каждый из которых пытается открыть свое собственное соединение, и стреляем так быстро, как только возможно.
В данном случае используется [открытая модель симуляции нагрузки][8]: мы определяем скорость прибытия новых виртуальных пользователей, 
а количество одновременных пользователей внутри системы является следствием времени ответа и общей длительности, и мы не контролируем этот параметр.
Закрытая же модель - это когда система работает на полную мощность, и прибывающие пользователи отправляются в очередь, когда мы не контролируем скорость прибытия новых виртуальных пользователей, а только текущее количество активных на данный момент.
В случае закрытой модели возможна проблема "_Coordinated Omission Problem_", которую [рассматривал][9] Gil Tene.
И _Gatling_, и _wrk2_ : генератор запросов - основан на [открытой системе][10], то есть это когда продолжает выдавать запросы независимо от скорости отклика, независимо от пропускной способности системы.

## Характеристики системы:

Наименование | Модель
--- | ---
Процессор | Intel Core i7 6700HQ (Skylake) 2.6 ГГц
Количество ядер | 8
Оперативная память | 8 ГБ DDR4 DDR4-2133 МГц
Накопитель | 128 ГБ HDD (5400 об/мин.)
ОС | Ubuntu Budgie 20.04
Версия курсового проекта | Этап 6. Асинхронный клиент

## Обстрел PUT'ами:

Обстрел был произведен со следующими настройками симуляции:

```
Running test with 5 users
Ramping users over 10 seconds
Total test duration: 60 seconds
Simulation boriskin.PutSimulation started...
```

_Gatling_ выводит в консоль каждые 5 сек. следующую информацию:

```
...

================================================================================
2020-11-24 12:49:24                                          40s elapsed
---- Requests ------------------------------------------------------------------
> Global                                                   (OK=267993 KO=0     )
> PUT                                                      (OK=267993 KO=0     )

---- putRequests ---------------------------------------------------------------
[--------------------------------------------------------------------------]  0%
          waiting: 0      / active: 5      / done: 0     
================================================================================

...
```

В конечном итоге по завершению обстрела _Gatling_ формирует следующий общий консольный вывод по результатам работы:

```
Simulation boriskin.PutSimulation completed in 60 seconds
Parsing log file(s)...
Parsing log file(s) done
Generating reports...

================================================================================
---- Global Information --------------------------------------------------------
> request count                                     447977 (OK=447977 KO=0     )
> min response time                                      0 (OK=0      KO=-     )
> max response time                                    893 (OK=893    KO=-     )
> mean response time                                     1 (OK=1      KO=-     )
> std deviation                                          2 (OK=2      KO=-     )
> response time 50th percentile                          1 (OK=1      KO=-     )
> response time 75th percentile                          1 (OK=1      KO=-     )
> response time 95th percentile                          1 (OK=1      KO=-     )
> response time 99th percentile                          2 (OK=2      KO=-     )
> mean requests/sec                                8145.036 (OK=8145.036 KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                        447974 (100%)
> 800 ms < t < 1200 ms                                   3 (  0%)
> t > 1200 ms                                            0 (  0%)
> failed                                                 0 (  0%)
================================================================================
```

Можно видеть, что обеспечивалась нагрузка со средним Rate 8145,036 зап./сек. .

_Gatling_ предоставляет информативный .html-отчет о проведенной симуляции нагрузки. В данном отчете можно видеть:

В начале в более красочном виде выводится то же, что в консоль в конце симуляции:

![image](https://user-images.githubusercontent.com/36387962/100097995-6c1c3400-2e6e-11eb-8b30-2fe516848169.png)

Распределение запросов-ответов за 60 сек. симуляции:

![image](https://user-images.githubusercontent.com/36387962/100096402-05e2e180-2e6d-11eb-9216-3a3d6f6a3a4f.png)

Также можно видеть персентили времени отклика:

![image](https://user-images.githubusercontent.com/36387962/100097830-28c1c580-2e6e-11eb-8a2a-27e3fd91864d.png)

Видно, что есть выбросы по времени ответа.

Распределение времени ответа:

![image](https://user-images.githubusercontent.com/36387962/100097676-eef0bf00-2e6d-11eb-8dd1-ac0952bd24c1.png)


Теперь проводем тестирование с бОльшим заданным количеством пользователей и большей длительностью симуляции:

```
Running test with 200 users
Ramping users over 20 seconds
Total test duration: 180 seconds
Simulation boriskin.PutSimulation started...
```

В итоге получим следующий консольный вывод:

```
Simulation boriskin.PutSimulation completed in 180 seconds
Parsing log file(s)...
Parsing log file(s) done
Generating reports...

================================================================================
---- Global Information --------------------------------------------------------
> request count                                    2192740 (OK=2192740 KO=0     )
> min response time                                      1 (OK=1      KO=-     )
> max response time                                    307 (OK=307    KO=-     )
> mean response time                                    15 (OK=15     KO=-     )
> std deviation                                          4 (OK=4      KO=-     )
> response time 50th percentile                         14 (OK=14     KO=-     )
> response time 75th percentile                         16 (OK=16     KO=-     )
> response time 95th percentile                         21 (OK=21     KO=-     )
> response time 99th percentile                         27 (OK=27     KO=-     )
> mean requests/sec                                12529.943 (OK=12529.943 KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                       2192740 (100%)
> 800 ms < t < 1200 ms                                   0 (  0%)
> t > 1200 ms                                            0 (  0%)
> failed                                                 0 (  0%)
================================================================================
```

Видно, что количество обеспечиваемых запросов в секунду увеличилось до 12529,943.

Посмотрим графики, предоставленные в отчете:

Распределение времени ответа:

![image](https://user-images.githubusercontent.com/36387962/100099072-f913bd00-2e6f-11eb-97d9-0442d970163a.png)

Персентили времени отклика:

![image](https://user-images.githubusercontent.com/36387962/100099128-0e88e700-2e70-11eb-9341-8c58e2b223a0.png)

Посекундное распределение запросов по Rate:

![image](https://user-images.githubusercontent.com/36387962/100099240-38daa480-2e70-11eb-8157-d62fccd8564e.png)


В случае 15k виртуальных пользователей сервер не справился с нагрузкой:

```
Running test with 15000 users
Ramping users over 20 seconds
Total test duration: 180 seconds
Simulation boriskin.PutSimulation started...
```

```
Simulation boriskin.PutSimulation completed in 183 seconds
Parsing log file(s)...
Parsing log file(s) done
Generating reports...

================================================================================
---- Global Information --------------------------------------------------------
> request count                                     194794 (OK=602    KO=194192)
> min response time                                    580 (OK=580    KO=1101  )
> max response time                                  60176 (OK=2390   KO=60176 )
> mean response time                                 12513 (OK=1207   KO=12548 )
> std deviation                                      11386 (OK=413    KO=11386 )
> response time 50th percentile                      10001 (OK=1059   KO=10001 )
> response time 75th percentile                      10009 (OK=1398   KO=10009 )
> response time 95th percentile                      60000 (OK=2158   KO=60000 )
> response time 99th percentile                      60013 (OK=2323   KO=60013 )
> mean requests/sec                                1094.348 (OK=3.382  KO=1090.966)
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                            52 (  0%)
> 800 ms < t < 1200 ms                                 315 (  0%)
> t > 1200 ms                                          235 (  0%)
> failed                                            194192 (100%)
---- Errors --------------------------------------------------------------------
> i.n.c.ConnectTimeoutException: connection timed out: localhost 177757 (91.54%)
/127.0.0.1:8080
> i.g.h.c.i.RequestTimeoutException: Request timeout to localhos  10488 ( 5.40%)
t/127.0.0.1:8080 after 60000 ms
> status.find.in(200,201,202,203,204,205,206,207,208,209,304), f   5947 ( 3.06%)
ound 504
================================================================================
```

![image](https://user-images.githubusercontent.com/36387962/100101352-3168ca80-2e73-11eb-9233-0bedce95c7bd.png)

![image](https://user-images.githubusercontent.com/36387962/100101413-40e81380-2e73-11eb-8f72-83fe9354c8b9.png)


Попробуем изменить установки запуска, что симулировать [закртую модель (closed model)][11] - - _Gatling_ 3.x.x [это позволяет][12]. То есть когда мы регулируем параметр количества виртуальных пользователей, находящихся сейчас в системе.

```scala
setUp(
    scn.inject(
      // Количество одновременных пользователей в системе постоянно
      constantConcurrentUsers(10) during (10 seconds),
      // Количество одновременных пользователей в системе линейно переходит 
      // из одного числа в другое за определенное время
      rampConcurrentUsers(10) to (15000) during (180 seconds) /
    )
  ).protocols(httpConf)
    .maxDuration(testDuration seconds)
```

Тогда результаты работы будут следующими:

![image](https://user-images.githubusercontent.com/36387962/100108996-71807b00-2e7c-11eb-9034-faca75c00004.png)

![image](https://user-images.githubusercontent.com/36387962/100109051-8230f100-2e7c-11eb-9ce3-912bb6c06786.png)

![image](https://user-images.githubusercontent.com/36387962/100109085-8bba5900-2e7c-11eb-80cd-7ed8f397bee9.png)

Видно, что система ведет себя по-другому в данном случае, но также на определенном момент больше не выдерживает нагрузку.

Таким образом, можно видеть, что _Gatling_ позволяет выполнять совершенно различные симуляции с различными настройками.

Профилирование под нагрзукой показало те же флейм-графы, что были получены при профилировании под нагрузкой _wrk2_:

![put-cpu](https://user-images.githubusercontent.com/36387962/100113016-eeadef00-2e80-11eb-9c74-dffcc6d793e2.png)

![put-cpu-alloc](https://user-images.githubusercontent.com/36387962/100113035-f2da0c80-2e80-11eb-9da4-dc596febb6fe.png)

![put-cpu-lock](https://user-images.githubusercontent.com/36387962/100113040-f4a3d000-2e80-11eb-8e11-46c8d36fe49b.png)


## Обстрел GET'ами.

По заранее наполненной БД со следующими установками:

```
Running test with 100 users
Ramping users over 20 seconds
Total test duration: 180 seconds
Simulation boriskin.GetSimulation started...
```

Каждые 5 сек. консольный вывод:

```
================================================================================
2020-11-24 18:04:46                                          85s elapsed
---- Requests ------------------------------------------------------------------
> Global                                                   (OK=256175 KO=0     )
> GET                                                      (OK=256175 KO=0     )

---- putRequests ---------------------------------------------------------------
[--------------------------------------------------------------------------]  0%
          waiting: 0      / active: 100    / done: 0     
================================================================================
```

Общий консольный вывод по результатам обстрела:

```
Simulation boriskin.GetSimulation completed in 180 seconds
Parsing log file(s)...
Parsing log file(s) done
Generating reports...

================================================================================
---- Global Information --------------------------------------------------------
> request count                                     548383 (OK=548383 KO=0     )
> min response time                                      2 (OK=2      KO=-     )
> max response time                                    442 (OK=442    KO=-     )
> mean response time                                    30 (OK=30     KO=-     )
> std deviation                                         16 (OK=16     KO=-     )
> response time 50th percentile                         28 (OK=28     KO=-     )
> response time 75th percentile                         39 (OK=39     KO=-     )
> response time 95th percentile                         59 (OK=59     KO=-     )
> response time 99th percentile                         77 (OK=77     KO=-     )
> mean requests/sec                                3133.617 (OK=3133.617 KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                        548383 (100%)
> 800 ms < t < 1200 ms                                   0 (  0%)
> t > 1200 ms                                            0 (  0%)
> failed                                                 0 (  0%)
================================================================================
```

Как видно, выдерживает нагрузку 3133,617 зап./сек. в среднем.

Посмотрим результаты работы в информативном отчете _Gatling_:

![image](https://user-images.githubusercontent.com/36387962/100112399-3e3feb00-2e80-11eb-94bc-8693c5a4320b.png)

![image](https://user-images.githubusercontent.com/36387962/100112453-4861e980-2e80-11eb-9431-eb86c1cf49de.png)

![image](https://user-images.githubusercontent.com/36387962/100112473-5152bb00-2e80-11eb-8b2f-7c0d5f443833.png)

![image](https://user-images.githubusercontent.com/36387962/100112509-5ca5e680-2e80-11eb-81a2-1b4335bcdede.png)

Система успешно справилась с симулированной нагрузкой, однако видно, что время ответа пострадало при таких настройках. Видно, что в основном время ответа колебалось в пределах 50-60 мс:

![image](https://user-images.githubusercontent.com/36387962/100112811-b7d7d900-2e80-11eb-8c90-d0800d68aff0.png)

Профилирование показало те же флейм графы, что и при обсреле с помощью _wrk2_:

![get-cpu](https://user-images.githubusercontent.com/36387962/100113333-4187a680-2e81-11eb-82d5-19cee8bd21a2.png)

![get-cpu-alloc](https://user-images.githubusercontent.com/36387962/100113339-43516a00-2e81-11eb-8f6e-f32e7d3651d9.png)

## Обстрел PUT + GET:

Произведем обстрел одновременно запросами на _GET_ и _PUT_:


```scala
val scn: ScenarioBuilder = scenario("putRequests")
    .forever("key") {
      exec(
        http("PUT")
          .put("/v0/entity?id=${key}")
          .body(StringBody { "${key}" })
      )
      .exec(
        http("GET")
          .get("/v0/entity?id=${key}")
      )
    }
```

_Gatling_ позволяет сделать это в одном сценарии симуляции, определенном в Scala-скрипте:

```
Running test with 100 users
Ramping users over 20 seconds
Total test duration: 180 seconds
Simulation boriskin.GetPutSimulation started...
```

Видно, что в ходе работы кол-во запросов распределеяется примерно поровну:

```
================================================================================
2020-11-24 18:23:42                                          45s elapsed
---- Requests ------------------------------------------------------------------
> Global                                                   (OK=191172 KO=0     )
> PUT                                                      (OK=95614  KO=0     )
> GET                                                      (OK=95558  KO=0     )

---- putRequests ---------------------------------------------------------------
[--------------------------------------------------------------------------]  0%
          waiting: 0      / active: 100    / done: 0     
================================================================================
```

Финальный консольный выхлоп:

```
Simulation boriskin.GetPutSimulation completed in 180 seconds
Parsing log file(s)...
Parsing log file(s) done
Generating reports...

================================================================================
---- Global Information --------------------------------------------------------
> request count                                     821610 (OK=821610 KO=0     )
> min response time                                      0 (OK=0      KO=-     )
> max response time                                    150 (OK=150    KO=-     )
> mean response time                                    20 (OK=20     KO=-     )
> std deviation                                          9 (OK=9      KO=-     )
> response time 50th percentile                         19 (OK=19     KO=-     )
> response time 75th percentile                         25 (OK=25     KO=-     )
> response time 95th percentile                         36 (OK=36     KO=-     )
> response time 99th percentile                         46 (OK=46     KO=-     )
> mean requests/sec                                4694.914 (OK=4694.914 KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                        821610 (100%)
> 800 ms < t < 1200 ms                                   0 (  0%)
> t > 1200 ms                                            0 (  0%)
> failed                                                 0 (  0%)
================================================================================
```

Наглядный информативный отчет _Gatling_:

Распределение времени ответа получилось нормальным:

![image](https://user-images.githubusercontent.com/36387962/100115240-536a4900-2e83-11eb-9c4b-40b3aa8c5c14.png)

Как и у PUT:

![image](https://user-images.githubusercontent.com/36387962/100115442-93c9c700-2e83-11eb-9384-d55a5b5a72f9.png)

У GET время ответа в среднем выше:

![image](https://user-images.githubusercontent.com/36387962/100115566-b956d080-2e83-11eb-9d4a-bd28ea6d067f.png)

нежели у PUT:

![image](https://user-images.githubusercontent.com/36387962/100115680-d9868f80-2e83-11eb-9f05-293c692d216f.png)

## Выводы по работе:

_Gatling_ удобный инструмент, который предоставляет рабочий функционал для нагрузочного тестирования. В том числе полностью удовлетворяет для задач нагрузочного тестирования для текущего курсового проекта. Однако настолько серьезный и масштабный инструментарий лучше использовать для мультизадачного нагрузочного тестирования, так как он явно разработан для этого. То есть для нагрузочного тестирования E-Commerce веб-сервисов. 

В ходе работы были составлены сценарии симуляции обстрела разработанной системы PUT и GET запросами. Было проведено тестирование стрессоустойчивости системы с использованием концепции виртуальных пользователей, концепций открытой и закрытой моделей. Были разобраны наглядные информативные .html-отчеты, формируемые _Gatling'ом_ по результатам нагрузки. 

[1]: https://github.com/gatling/gatling/stargazers                                               	"GitHub stars"
[2]: https://github.com/gatling/gatling                                                          	"Gatling GitHub"
[3]: https://gatling.io/gatling-frontline/on-premises/#price-comparison              		"Garling FrontLine"
[4]: https://gatling.io/docs/current/general/concepts/#virtual-user                     		"Virtual User"
[5]: https://gatling.io/docs/current/general/reports/                                            	"Reports"
[6]: https://gatling.io/docs/current/http/recorder/                                                	"Gatling Recorder"
[7]: https://github.com/giltene/wrk2                                                          	"wrk2 GitHub"
[8]: https://gatling.io/docs/current/general/simulation_setup/#open-model        			"Gatling: OpenModel setUp"
[9]: https://www.youtube.com/watch?v=lJ8ydIuPFeU                                        		"How NOT to Measure Latency"
[10]: https://news.ycombinator.com/item?id=10486215                                     		"Четвертый ответ в топике"
[11]: https://gatling.io/docs/current/general/simulation_setup/#closed-model    			"Gatling: ClosedModel setUp"
[12]: https://gatling.io/docs/current/whats_new/3.0/                                           	"Gatling 3: new exciting features"

