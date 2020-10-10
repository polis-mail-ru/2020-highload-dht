## Анализ результатов

### PUT

Параметры запуска:
<ol>
<li>4 потока</li>
<li>64 открытых соединения</li>
<li>2 минуты работы</li>
<li>15000 запросов в секунду</li>
</ol>

![CPU PUT](/async-svg/cpu-put.svg)

В отличие от предыдущих этапов, процессор обрабатывает 8 селекторов в реализации lsm (примерно по 5%) и 8 потоков работы http-сервиса (примерно по 5%). Остальные ресурсы процессора выделены на GC-потоки.

![ALLOC PUT](/async-svg/alloc-put.svg)

В отличие от предыдущих этапов, память выделяется на 8 селекторов в реализации lsm (примерно по 5%) и 8 потоков работы http-сервиса. Один из потоков исполняет работу DAOImpl (16.3%), все остальные исполняют работу ServiceImpl (примерно по 5%).

![LOCK PUT](/async-svg/lock-put.svg)

В отличие от предыдущих этапов, к блокированиям операций DAO добавляются блокировки потоков работы http-сервиса.

```
Running 2m test @ http://127.0.0.1:8080
  4 threads and 64 connections
  Thread calibration: mean lat.: 1.829ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.901ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.961ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.870ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.29ms    6.31ms 113.73ms   95.85%
    Req/Sec     1.99k     0.88k   15.67k    78.99%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    0.99ms
 75.000%    1.36ms
 90.000%    3.73ms
 99.000%   36.29ms
 99.900%   75.07ms
 99.990%   97.98ms
 99.999%  109.25ms
100.000%  113.79ms

#[Mean    =        2.294, StdDeviation   =        6.312]
#[Max     =      113.728, Total count    =       824413]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  900134 requests in 2.00m, 57.52MB read
  Socket errors: connect 0, read 0, write 0, timeout 1856
Requests/sec:   7501.00
Transfer/sec:    490.79KB
```

Итоги:
<ol>
<li>обработано 900134 запросов</li>
<li>прочитано 57.52MB данных</li>
<li>сервер держит половину заданной нагрузки на уровне 7501 запросов в секунду</li>
</ol>

### GET

Параметры запуска:
<ol>
<li>4 потока</li>
<li>64 открытых соединения</li>
<li>2 минуты работы</li>
<li>5000 запросов в секунду</li>
</ol>

![CPU GET](/async-svg/cpu-get.svg)

В отличие от предыдущих этапов, процессор обрабатывает 8 селекторов в реализации lsm (примерно по 1%) и 8 потоков работы http-сервиса (примерно по 12%). Остальные ресурсы процессора выделены на GC-потоки.

![ALLOC GET](/async-svg/alloc-get.svg)

В отличие от предыдущих этапов, память выделяется на 8 селекторов в реализации lsm (примерно по 6%) и 8 потоков работы http-сервиса (примерно по 6%).

![LOCK GET](/async-svg/lock-get.svg)

В отличие от предыдущих этапов, к блокированиям операций DAO добавляются блокировки потоков работы http-сервиса (примерно по 12%).

```
Running 2m test @ http://127.0.0.1:8080
  4 threads and 64 connections
  Thread calibration: mean lat.: 11.052ms, rate sampling interval: 56ms
  Thread calibration: mean lat.: 10.599ms, rate sampling interval: 50ms
  Thread calibration: mean lat.: 10.451ms, rate sampling interval: 51ms
  Thread calibration: mean lat.: 11.589ms, rate sampling interval: 54ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.06s     1.19s    4.89s    78.78%
    Req/Sec   611.75    262.03     1.68k    64.94%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%  595.97ms
 75.000%    2.08s 
 90.000%    2.81s 
 99.000%    3.89s 
 99.900%    4.73s 
 99.990%    4.88s 
 99.999%    4.89s 
100.000%    4.89s 

#[Mean    =     1058.210, StdDeviation   =     1189.449]
#[Max     =     4886.528, Total count    =       266550]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  294519 requests in 2.00m, 18.78MB read
  Socket errors: connect 0, read 0, write 0, timeout 1847
Requests/sec:   2454.31
Transfer/sec:    160.23KB
```

Итоги:
<ol>
<li>обработано 294519 запросов</li>
<li>прочитано 18.78MB данных</li>
<li>сервер выдерживает половину заданной нагрузки на уровне 2454.31 запросов в секунду</li>
</ol>