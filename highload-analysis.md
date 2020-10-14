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
  Thread calibration: mean lat.: 1.632ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.670ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.697ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.651ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.35ms    5.16ms 132.86ms   95.34%
    Req/Sec     3.96k     0.96k   15.44k    84.96%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.28ms
 75.000%    1.93ms
 90.000%    4.13ms
 99.000%   24.62ms
 99.900%   70.85ms
 99.990%   99.14ms
 99.999%  116.10ms
100.000%  132.99ms

#[Mean    =        2.350, StdDeviation   =        5.157]
#[Max     =      132.864, Total count    =      1648771]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  1799418 requests in 2.00m, 114.98MB read
Requests/sec:  14995.36
Transfer/sec:      0.96MB
```

Итоги:
<ol>
<li>обработано 1799418 запросов</li>
<li>прочитано 114.98MB данных</li>
<li>сервер держит заданную нагрузку на уровне 14995.36 запросов в секунду</li>
</ol>

Выполним профилирование сервера при работе на одном потоке.

![CPU PUT SINGLE](/async-svg/cpu-put-single.svg)

Процессор выделяет 57.44% ресурсов на обработку Java-потока, 37.76% ресурсов - на обработку селектора потоков сервера. Остатки идут на исполнение нативных функций.

![ALLOC PUT SINGLE](/async-svg/alloc-put-single.svg)

Общая память выделяется на Java-поток (51.07%) и селектор потока сервера (48.93%).

![LOCK PUT SINGLE](/async-svg/lock-put-single.svg)

Блокировки Java-потока и селектора потока сервера делятся соответственно на 63.81% и 36.19%.

```
Running 2m test @ http://127.0.0.1:8080
  1 threads and 64 connections
  Thread calibration: mean lat.: 1.513ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.72ms    1.45ms  31.92ms   94.12%
    Req/Sec    15.83k     2.03k   42.89k    81.46%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.48ms
 75.000%    2.01ms
 90.000%    2.68ms
 99.000%    7.64ms
 99.900%   18.96ms
 99.990%   26.72ms
 99.999%   29.87ms
100.000%   31.93ms

#[Mean    =        1.723, StdDeviation   =        1.451]
#[Max     =       31.920, Total count    =      1645209]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  1797665 requests in 2.00m, 114.86MB read
Requests/sec:  14980.40
Transfer/sec:      0.96MB
```

Итоги:
<ol>
<li>обработано 1797665 запросов</li>
<li>прочитано 114.86MB данных</li>
<li>сервер держит заданную нагрузку на уровне 14980.40 запросов в секунду</li>
</ol>

### GET

Параметры запуска:
<ol>
<li>4 потока</li>
<li>64 открытых соединения</li>
<li>2 минуты работы</li>
<li>2500 запросов в секунду</li>
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
  Thread calibration: mean lat.: 2.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.642ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.636ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   395.28ms  965.20ms   3.35s    86.63%
    Req/Sec   658.97    208.97     2.56k    81.16%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    2.91ms
 75.000%    4.11ms
 90.000%    2.78s 
 99.000%    3.17s 
 99.900%    3.27s 
 99.990%    3.31s 
 99.999%    3.34s 
100.000%    3.35s 

#[Mean    =      395.280, StdDeviation   =      965.197]
#[Max     =     3346.432, Total count    =       274800]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  299936 requests in 2.00m, 19.12MB read
  Non-2xx or 3xx responses: 3
Requests/sec:   2499.47
Transfer/sec:    163.18KB
```

Итоги:
<ol>
<li>обработано 299936 запросов</li>
<li>прочитано 19.12MB данных</li>
<li>сервер выдерживает заданную нагрузку на уровне 2499.47 запросов в секунду</li>
</ol>

Выполним профилирование сервера при работе на одном потоке.

![CPU GET SINGLE](/async-svg/cpu-get-single.svg)

Процессор выделяет 95.35% ресурсов на обработку Java-потока, 3.41% ресурсов - на обработку селектора потоков сервера. Остатки идут на исполнение нативных функций.

![ALLOC GET SINGLE](/async-svg/alloc-get-single.svg)

Общая память выделяется на Java-поток (47.11%) и селектор потока сервера (52.89%).

![LOCK GET SINGLE](/async-svg/lock-get-single.svg)

Блокировки исполняются только для Java-потока.

```
Running 2m test @ http://127.0.0.1:8080
  1 threads and 64 connections
  Thread calibration: mean lat.: 2.069ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   337.95ms  974.08ms   5.04s    89.96%
    Req/Sec     2.64k     0.93k    8.80k    86.79%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    2.38ms
 75.000%    3.21ms
 90.000%    1.32s 
 99.000%    4.54s 
 99.900%    4.90s 
 99.990%    4.97s 
 99.999%    5.03s 
100.000%    5.04s 

#[Mean    =      337.946, StdDeviation   =      974.079]
#[Max     =     5038.080, Total count    =       274201]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  299634 requests in 2.00m, 19.33MB read
Requests/sec:   2496.95
Transfer/sec:    164.91KB
```

Итоги:
<ol>
<li>обработано 299634 запросов</li>
<li>прочитано 19.33MB данных</li>
<li>сервер держит заданную нагрузку на уровне 2496.95 запросов в секунду</li>
</ol>