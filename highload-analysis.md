##Put

Параметры запуска:
<ol>
<li>4 потока</li>
<li>64 открытых соединения</li>
<li>2 минуты работы</li>
<li>5000 запросов в секунду</li>
</ol>


```
wrk2 -t4 -c64 -d2m -R5000 -s wrk/put.lua --latency http://127.0.0.1:8080
Running 2m test @ http://127.0.0.1:8080
  4 threads and 64 connections
  Thread calibration: mean lat.: 1.708ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.635ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.762ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.679ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     3.45ms   16.29ms 476.16ms   97.80%
    Req/Sec     1.32k   238.99     4.33k    88.95%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.37ms
 75.000%    1.90ms
 90.000%    3.04ms
 99.000%   56.64ms
 99.900%  272.89ms
 99.990%  443.39ms
 99.999%  468.99ms
100.000%  476.42ms
 
#[Mean    =        3.445, StdDeviation   =       16.292]
#[Max     =      476.160, Total count    =       549600]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  599839 requests in 2.00m, 52.06MB read
Requests/sec:   4998.66
Transfer/sec:    444.22KB

```

```
/async-profiler -d 15 -f cpu-put.svg -e cpu 291344
```

![CPU PUT](/async/cpu-put.svg)

Ресурсов CPU теперь больше тратится на поток-селектор, т.к. прежде чем ответить, он ожидает ответов от нескольких других узлов. Вызов get() для Future<> является блокирующим, поэтому пока все узлы не отправят ответ, поток-селектор всё ещё занят ожиданием. Сбор результатов для синтеза единого результата можно было бы выполнить в отдельном рабочем потоке, вместо задействования потока-селектора.

```
/async-profiler -d 15 -f alloc-put.svg -e alloc 291344
```
![ALLOC PUT](/async/alloc-put.svg)

При сравнении диаграмм видно, что в новой версии добавились траты памяти при выполнении метода sendToReplicas. Это может быть связано с тем, что мы получаем ответы от разных узлов, прежде чем сформировать окончательный ответ для клиента.

```
/async-profiler -d 15 -f lock-put.svg -e lock 291344
```

![LOCK PUT](/async/lock-put.svg)
Вместо блокировок на методе execute, появились блокировки на submit. При этом видно, что основное использование блокировок приходится во время исполнения метода submit при инициировании отправок запроса репликам, а также при делегировании выполнения операции текущим узлом в ином потоке. Нагрузки по блокировкам на рабочие потоки стала выше, в основном из-за того, что рабочие потоки простаивают в ожидании задач. Хотя в коде отправка запросов на реплики происходит конкуррентно в рабочих потоках, но сбор результатов выполняется в потоке-селектора по мере получения ответов от реплик.

##Get

Параметры запуска:
<ol>
<li>4 потока</li>
<li>64 открытых соединения</li>
<li>2 минуты работы</li>
<li>5000 запросов в секунду</li>
</ol>


```
wrk2 -t4 -c64 -d2m -R5000 -s wrk/get.lua --latency http://127.0.0.1:8080
Running 2m test @ http://127.0.0.1:8080
  4 threads and 64 connections
  Thread calibration: mean lat.: 1.355ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.383ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.374ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.372ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    11.26ms   54.79ms   1.16s    96.78%
    Req/Sec     1.32k   319.20     4.60k    83.48%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.53ms
 75.000%    2.73ms
 90.000%   13.72ms
 99.000%  210.43ms
 99.900%  800.77ms
 99.990%    1.04s 
 99.999%    1.14s 
100.000%    1.16s 
 
#[Mean    =       11.262, StdDeviation   =       54.794]
#[Max     =     1158.144, Total count    =       549567]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  599807 requests in 2.00m, 52.20MB read
Requests/sec:   4998.34
Transfer/sec:    445.45KB
```
```
/async-profiler -d 15 -f cpu-get.svg -e cpu 291344 
```

![CPU GET](/async/cpu-get.svg)

По аналогии с cpu put.

```
/async-profiler -d 15 -f alloc-get.svg -e alloc 291344
```

![ALLOC GET](/async/alloc-get.svg)
В сравнении с диаграммой, полученной на предыдущем этапе, видно, что появились накладные расходы по памяти при выполнении метода sendToReplicas класса AsyncServiceImpl. Основные расходы при вызове этого метода сопряжены в передачей на реплики того же запроса (метод passOn), отправкой ответа (метод trySendResponse) и хранением ответственных за исполнение запроса реплик (getResponsibleNodes).

```
/async-profiler -d 15 -f lock-get.svg -e lock 291344
```

![LOCK GET](/async/lock-get.svg)
Изменения, которые мы видим на диаграммах, очень близки к изменениям, описанным для lock put.