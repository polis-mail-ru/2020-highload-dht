##Put

Параметры запуска:
<ol>
<li>4 потока</li>
<li>64 открытых соединения</li>
<li>2 минуты работы</li>
<li>1000 запросов в секунду</li>
</ol>


```
wrk2 -t4 -c64 -d2m -R1000 -s wrk/put.lua --latency http://127.0.0.1:8080
Running 2m test @ http://127.0.0.1:8080
  4 threads and 64 connections
  Thread calibration: mean lat.: 1.788ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.747ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.783ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.923ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.32ms    5.93ms 202.11ms   98.93%
    Req/Sec   263.73    108.54     2.44k    74.81%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.87ms
 75.000%    2.27ms
 90.000%    2.73ms
 99.000%    8.63ms
 99.900%  113.92ms
 99.990%  184.83ms
 99.999%  201.21ms
100.000%  202.24ms
 
#[Mean    =        2.322, StdDeviation   =        5.926]
#[Max     =      202.112, Total count    =       109912]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  119992 requests in 2.00m, 6.75MB read
Requests/sec:    999.91
Transfer/sec:     57.61KB

```

```
/async-profiler -d 15 -f cpu-put.svg -e cpu 291344
```

![CPU PUT](/async/cpu-put.svg)

CPU стал тратить меньше ресурсов при выполнении операций в потоке SelectorThread.
Вместо накладных расходов при выполнении invoke() в one.nio.HttpClient, появились накладные расходы при выполнении асинхронной отправки и последующего получения ответа через java.net.http.HttpClient.


```
/async-profiler -d 15 -f alloc-put.svg -e alloc 291344
```
![ALLOC PUT](/async/alloc-put.svg)

Рабочие потоки используют больше памяти, нежели чем поток SelectorThread, т.к. обработка результата от реплик делегирована пулу рабочих потоков.
Узким местом с точки зрения расходования памяти при выполнении операций в рабочих потоках является выполнение метода postComplete() класса CompletableFuture, а также выполнение операции обработки запроса (put) в лямбда-выражении метода processRequest() класса AsyncServiceImpl.


```
/async-profiler -d 15 -f lock-put.svg -e lock 291344
```

![LOCK PUT](/async/lock-put.svg)

На новой диаграмме блокировки в SelectorThread значительно ниже (1.98% против 38% из старой диграммы). Это говорит о том, что поток SelectorThread тратит меньше времени на ожидания, а значит значительно раньше может обслужить нового клиента.
Если ранее мы ожидали ответа от всех реплик и только после этого возвращали ответ клиенту, то теперь мы приступаем к формированию ответа в будущем лишь тогда, когда все ответы от разных реплик уже получены.
 

##Get

Параметры запуска:
<ol>
<li>4 потока</li>
<li>64 открытых соединения</li>
<li>2 минуты работы</li>
<li>1000 запросов в секунду</li>
</ol>


```
wrk2 -t4 -c64 -d2m -R1000 -s wrk/get.lua --latency http://127.0.0.1:8080
Running 2m test @ http://127.0.0.1:8080
  4 threads and 64 connections
  Thread calibration: mean lat.: 1.923ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.928ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.905ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 4.215ms, rate sampling interval: 11ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.50ms    1.78ms  39.39ms   82.98%
    Req/Sec   263.01    277.25     1.60k    94.98%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.94ms
 75.000%    2.86ms
 90.000%    4.81ms
 99.000%    8.77ms
 99.900%   16.93ms
 99.990%   28.56ms
 99.999%   33.69ms
100.000%   39.42ms
 
#[Mean    =        2.495, StdDeviation   =        1.782]
#[Max     =       39.392, Total count    =       109923]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  119756 requests in 2.00m, 7.27MB read
Requests/sec:    997.62
Transfer/sec:     61.99KB
```
```
/async-profiler -d 15 -f cpu-get.svg -e cpu 291344 
```

![CPU GET](/async/cpu-get.svg)

Заметим, что появились накладные расходы (помимо рабочих потоков и потока-селектора) при выполнении метода run() статического класса SelectorManager, являющегося вложенным в класс HttpClientImpl из пакета jdk.internal.net.http. Эти накладные расходы связаны с тем, что мы теперь используем HttpClient из библиотеки Java.
Обработка запроса в потоке-селекторе теперь, в общем, использует меньше ресурсов CPU.
Появились накладные расходы в рабочих потоках, связанные с использованием HttpClient'а из библиотеки Java.

```
/async-profiler -d 15 -f alloc-get.svg -e alloc 291344
```

![ALLOC GET](/async/alloc-get.svg)

Видно, что использование памяти стало заметным при выполнении метода run() класса ForkJoinWorkerThread из пакета java.util.concurrent, который в конечном итоге вызывает наше лямбда-выражение, отвечающее за сбор результатов с реплик (только в момент, когда все данные уже получены от этих реплик), синтезом ответа и отправки ответа клиенту.
Ранее, данная операция выполнялась в потоке-селекторе. Можем предположить, что для реализации метода CompletableFuture.allOf() используется часть функциональности из класса ForkJoinWorkerThread.


```
/async-profiler -d 15 -f lock-get.svg -e lock 291344
```

![LOCK GET](/async/lock-get.svg) 

Блокировки в потоке-селекторе стали появляться значительно меньшие по времени, в то время, как появились продолжительные и существенные по времени блокировки при выполнении метода run класса SelectorManager, вложенного по отношению к классу HttpClientImpl из пакета jdk.internal.net.http, что можно объяснить тем, что мы задействовали HttpClient из библиотеки Java, который периодически ожидает очереденой запрос, который необходимо отправить, а также, проверяет, не случилось ли превышение таймаута.
Заметим, что на новой диаграмме не видны методы из нашего кода, что говорит о том, что наш код написан таким образом, что использование блокирующих операций сведено к минимуму.
