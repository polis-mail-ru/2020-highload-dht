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

Выполним профилирование сервера для одного потока.

![CPU PUT SINGLE](/async-svg/cpu-put-single.svg)

Процессор выделяет 57.44% ресурсов на обработку Java-потока, 37.76% ресурсов - на обработку селектора потоков сервера. Остатки идут на исполнение нативных функций.

![ALLOC PUT SINGLE](/async-svg/alloc-put-single.svg)

Общая память выделяется на Java-поток (51.07%) и селектор потока сервера (48.93%).

![LOCK PUT SINGLE](/async-svg/lock-put-single.svg)

Блокировки Java-потока и селектора потока сервера делятся соответственно на 63.81% и 36.19%.

### GET

Параметры запуска:
<ol>
<li>4 потока</li>
<li>64 открытых соединения</li>
<li>2 минуты работы</li>
<li>1500 запросов в секунду</li>
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
  Thread calibration: mean lat.: 1.858ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.868ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.877ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.867ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     5.04ms   27.95ms 432.90ms   98.26%
    Req/Sec   394.19     81.49     1.78k    69.76%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.69ms
 75.000%    2.07ms
 90.000%    2.45ms
 99.000%  162.82ms
 99.900%  348.42ms
 99.990%  380.16ms
 99.999%  401.92ms
100.000%  433.15ms

#[Mean    =        5.042, StdDeviation   =       27.955]
#[Max     =      432.896, Total count    =       164879]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  179975 requests in 2.00m, 11.46MB read
Requests/sec:   1499.77
Transfer/sec:     97.77KB

```

Итоги:
<ol>
<li>обработано 179975 запросов</li>
<li>прочитано 11.46MB данных</li>
<li>сервер выдерживает заданную нагрузку на уровне 1499.77 запросов в секунду</li>
</ol>

Выполним профилирование сервера для одного потока.

![CPU GET SINGLE](/async-svg/cpu-get-single.svg)

Процессор выделяет 95.35% ресурсов на обработку Java-потока, 3.41% ресурсов - на обработку селектора потоков сервера. Остатки идут на исполнение нативных функций.

![ALLOC GET SINGLE](/async-svg/alloc-get-single.svg)

Общая память выделяется на Java-поток (47.11%) и селектор потока сервера (52.89%).

![LOCK GET SINGLE](/async-svg/lock-get-single.svg)

Блокировки исполняются только для Java-потока.