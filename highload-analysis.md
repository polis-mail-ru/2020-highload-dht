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
  Thread calibration: mean lat.: 4.433ms, rate sampling interval: 15ms
  Thread calibration: mean lat.: 4.349ms, rate sampling interval: 16ms
  Thread calibration: mean lat.: 4.581ms, rate sampling interval: 15ms
  Thread calibration: mean lat.: 5.849ms, rate sampling interval: 19ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.83s     1.66s    5.45s    45.97%
    Req/Sec   762.62    223.02     2.07k    64.73%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.68s 
 75.000%    3.36s 
 90.000%    3.89s 
 99.000%    5.10s 
 99.900%    5.31s 
 99.990%    5.39s 
 99.999%    5.44s 
100.000%    5.45s 

#[Mean    =     1826.595, StdDeviation   =     1657.288]
#[Max     =     5447.680, Total count    =       324652]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  354089 requests in 2.00m, 22.58MB read
Requests/sec:   2950.69
Transfer/sec:    192.70KB
```

Итоги:
<ol>
<li>обработано 354089 запросов</li>
<li>прочитано 22.58MB данных</li>
<li>сервер выдерживает заданную нагрузку на уровне 2950.69 запросов в секунду</li>
</ol>