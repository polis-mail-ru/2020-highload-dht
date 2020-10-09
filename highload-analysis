## Анализ результатов

### PUT

Параметры запуска:
<ol>
<li>4 потока</li>
<li>20 открытых соединений</li>
<li>2 минуты работы</li>
<li>15000 запросов в секунду</li>
</ol>

![CPU PUT](/async-svg/cpu-put.svg)

Каждый из 8 селекторов отрабатывают запросы и занимают около 8-12% работы процессора. Остальное пространство ядра занимают системные вызовы и работа основного потока Java (15%).

![ALLOC PUT](/async-svg/alloc-put.svg)

Каждый из 8 селекторов использует около 12% выделяемой памяти. Остальная память идёт непосредственно на пул потоков (9%).

![LOCK PUT](/async-svg/lock-put.svg)

Каждый селектор исполняет lock операций.

```
Running 2m test @ http://127.0.0.1:8080
  4 threads and 20 connections
  Thread calibration: mean lat.: 3.084ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 3.123ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.113ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 4.726ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.43ms    2.81ms  78.14ms   96.50%
    Req/Sec     3.95k   628.23    15.78k    90.05%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.01ms
 75.000%    1.41ms
 90.000%    1.98ms
 99.000%   11.38ms
 99.900%   42.40ms
 99.990%   68.67ms
 99.999%   74.62ms
100.000%   78.21ms

#[Mean    =        1.430, StdDeviation   =        2.808]
#[Max     =       78.144, Total count    =      1649613]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  1799828 requests in 2.00m, 115.00MB read
Requests/sec:  14998.79
Transfer/sec:      0.96MB
```

Итоги:
<ol>
<li>обработано 1799828 запросов</li>
<li>прочитано 115.00MB данных</li>
<li>сервер держит заданную нагрузку на уровне 14998.79 запросов в секунду</li>
</ol>

Попробуем рассчитать максимально возможную пропускную способность, которую еще будет успевать обрабатывать машина. Гарантированным значением было выбрано 20000 запросов в секунду, так как при нагрузке в 100000 запросов в секунду реальное количество обработанных запросов примерно равно этому числу.

![CPU PUT HIGH](/async-svg/cpu-put-high.svg)

В отличие от предыдущего опыта, в данном случае процессор также обрабатывает пул потоков.

![ALLOC PUT HIGH](/async-svg/alloc-put-high.svg)

В данном случае происходит выделение памяти на RMI вместе с пуллом потоков.

![LOCK PUT HIGH](/async-svg/lock-put-high.svg)

Ситуация аналогична.

```
Running 2m test @ http://127.0.0.1:8080
  4 threads and 20 connections
  Thread calibration: mean lat.: 1.669ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.603ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.637ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.632ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     6.80ms   29.82ms 444.67ms   97.01%
    Req/Sec     5.28k     1.30k   17.89k    83.52%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.36ms
 75.000%    2.18ms
 90.000%    6.26ms
 99.000%  179.20ms
 99.900%  352.00ms
 99.990%  426.49ms
 99.999%  439.81ms
100.000%  444.93ms

#[Mean    =        6.796, StdDeviation   =       29.823]
#[Max     =      444.672, Total count    =      2199478]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  2399760 requests in 2.00m, 153.34MB read
Requests/sec:  19998.06
Transfer/sec:      1.28MB
```

### GET

Параметры запуска:
<ol>
<li>4 потока</li>
<li>20 открытых соединений</li>
<li>2 минуты работы</li>
<li>15000 запросов в секунду</li>
</ol>

![CPU GET](/async-svg/cpu-get.svg)

Каждый из 8 селекторов отрабатывают запросы и занимают около 12% работы процессора.

![ALLOC GET](/async-svg/alloc-get.svg)

Каждый из 8 селекторов использует около 12% выделяемой памяти. Остальная память идёт непосредственно на RMI (8.5%).

![LOCK GET](/async-svg/lock-get.svg)

Каждый селектор исполняет lock операций.

```
Running 2m test @ http://127.0.0.1:8080
  4 threads and 20 connections
  Thread calibration: mean lat.: 4505.013ms, rate sampling interval: 15630ms
  Thread calibration: mean lat.: 4426.050ms, rate sampling interval: 15056ms
  Thread calibration: mean lat.: 4095.986ms, rate sampling interval: 14614ms
  Thread calibration: mean lat.: 4512.846ms, rate sampling interval: 15704ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     0.92m    29.07s    1.82m    57.27%
    Req/Sec   470.71     79.89   624.00     57.14%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    0.90m 
 75.000%    1.35m 
 90.000%    1.59m 
 99.000%    1.76m 
 99.900%    1.80m 
 99.990%    1.82m 
 99.999%    1.82m 
100.000%    1.82m 

#[Mean    =    55037.652, StdDeviation   =    29068.662]
#[Max     =   109248.512, Total count    =       207944]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  229786 requests in 2.00m, 14.64MB read
Requests/sec:   1914.87
Transfer/sec:    124.93KB
```

Итоги:
<ol>
<li>обработано 229786 запросов</li>
<li>прочитано 14.64MB данных</li>
<li>сервер не выдерживает заданную нагрузку и выставляет её на уровне 1914.87 запросов в секунду</li>
</ol>

Необходимо уменьшить целевую нагрузку с целью ускорить выполнение операций. Гарантированным значением было выбрано 1500 запросов в секунду.

![CPU PUT LOW](/async-svg/cpu-get-low.svg)

В отличие от предыдущего опыта, в данном случае процессор практически не обрабатывает пулл потоков.

![ALLOC PUT LOW](/async-svg/alloc-get-low.svg)

Ситуация аналогична.

![LOCK PUT LOW](/async-svg/lock-get-low.svg)

Ситуация аналогична.

```
Running 2m test @ http://127.0.0.1:8080
  4 threads and 20 connections
  Thread calibration: mean lat.: 3.490ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 3.661ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 3.017ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 3.221ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    19.60ms  126.10ms   2.29s    97.83%
    Req/Sec   395.22     80.44     0.89k    68.41%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    3.43ms
 75.000%    4.24ms
 90.000%    6.30ms
 99.000%  532.48ms
 99.900%    1.98s 
 99.990%    2.27s 
 99.999%    2.29s 
100.000%    2.29s 

#[Mean    =       19.599, StdDeviation   =      126.098]
#[Max     =     2289.664, Total count    =       164960]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  179992 requests in 2.00m, 11.46MB read
Requests/sec:   1499.92
Transfer/sec:     97.78KB
```