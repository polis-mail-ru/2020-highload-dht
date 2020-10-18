### PUT

Параметры запуска:
<ol>
<li>4 потока</li>
<li>64 открытых соединения</li>
<li>3 минуты работы</li>
<li>15000 запросов в секунду</li>
</ol>

```
./profiler.sh -e cpu -d 15 -t -f cpu-put.svg 240038
```
![CPU PUT](/async/cpu-put.svg)

По сравнению в синхронным вариантом, в асинхронном варианте процессор обрабатывает 8 селекторов RocksDB (примерно по 10%) и 8 потоков работы http-сервиса (примерно по 3%). В синхронном мы наблюдали обработку 8 селекторов. 

```
./profiler.sh -e alloc -d 15 -t -f alloc-put.svg 240038
```
![ALLOC PUT](/async/alloc-put.svg)

По сравнению в синхронным вариантом, в асинхронном варианте память выделяется на 8 селекторов RocksDB и 8 потоков работы http-сервиса по 6% каждый. В синхронном варианте каждый из 8 селекторов использует в среднем 13% выделенной памяти.

```
./profiler.sh -e cpu -d 15 -t -f cpu-put.svg 240038
```
![LOCK PUT](/async/lock-put.svg)

По сравнению в синхронным вариантом, в асинхронном варианте появились блокировки операций DAO (по 8%) и блокировки потоков работы http-сервиса (по 5%). Это главное отличие от синхронного варианта.

```
wrk2 -t4 -c64 -d3m -R10000 -s wrk/put.lua --latency http://127.0.0.1:8080
Running 3m test @ http://127.0.0.1:8080
  4 threads and 64 connections
  Thread calibration: mean lat.: 1.309ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.320ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.300ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.315ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.36ms    1.20ms  50.91ms   95.66%
    Req/Sec     2.64k   305.06    12.33k    84.20%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.20ms
 75.000%    1.65ms
 90.000%    2.08ms
 99.000%    5.50ms
 99.900%   15.34ms
 99.990%   38.08ms
 99.999%   48.90ms
100.000%   50.94ms

#[Mean    =        1.361, StdDeviation   =        1.204]
#[Max     =       50.912, Total count    =      1699198]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  1799644 requests in 3.00m, 114.99MB read
Requests/sec:   9997.98
Transfer/sec:    654.16KB
```

Итоги:
<ol>
<li>обработано 1799644 запросов</li>
<li>прочитано 114.99MB данных</li>
<li>сервер держит заданную нагрузку на уровне 9997.98 запросов в секунду</li>
</ol>

Выполним профилирование сервера для одного потока.

```
./profiler.sh -e cpu -d 15 -f cpu-put-single.svg 240038
```
![CPU PUT SINGLE](/async/cpu-put-single.svg)

Процессор выделяет 59.12% ресурсов на асинхронный сервис, 18.46% ресурсов на получение задач потоков и 22% ресурсов на обработку селектора потоков onenio. На работу DAO выделяется 51.33% ресурсов процессора.  

```
./profiler.sh -e alloc -d 15 -f alloc-put-single.svg 240038
```
![ALLOC PUT SINGLE](/async/alloc-put-single.svg)

Общая память выделяется на Java-поток (49.52%) и селектор потока сервера (50.48%).

```
./profiler.sh -e lock -d 15 -f lock-put-single.svg 240038
```
![LOCK PUT SINGLE](/async/lock-put-single.svg)

Блокировки Java-потока и селектора потока сервера делятся соответственно на 58.68% и 41.32%.

### GET

Параметры запуска:
<ol>
<li>4 потока</li>
<li>64 открытых соединения</li>
<li>3 минуты работы</li>
<li>10000 запросов в секунду</li>
</ol>


```
./profiler.sh -e cpu -d 15 -t -f cpu-get.svg 240038
```
![CPU GET](/async/cpu-get.svg)

По сравнению в синхронным вариантом, в асинхронном варианте процессор обрабатывает 8 селекторов RocksDB (примерно по 10%) и 8 потоков работы http-сервиса (примерно по 3%). В синхронном мы наблюдали обработку 8 селекторов, которые занимают примерно 12% работы процессора.


```
./profiler.sh -e alloc -d 15 -t -f alloc-get.svg 240038
```
![ALLOC GET](/async/alloc-get.svg)

По сравнению в синхронным вариантом, в асинхронном варианте память выделяется на 8 селекторов RocksDB и 8 потоков работы http-сервиса по 6% каждый. В синхронном варианте каждый из 8 селекторов использует в среднем 13% выделяемой памяти.


```
./profiler.sh -e lock -d 15 -t -f lock-get.svg 240038
```
![LOCK GET](/async/lock-get.svg)

По сравнению в синхронным вариантом, в асинхронном варианте появились блокировки операций DAO (по 8%) и блокировки потоков работы http-сервиса (по 5%). Это главное отличие от синхронного варианта.

```
wrk2 -t4 -c64 -d3m -R10000 -s wrk/get.lua --latency http://127.0.0.1:8080
Running 3m test @ http://127.0.0.1:8080
  4 threads and 64 connections
  Thread calibration: mean lat.: 1.295ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.291ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.283ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.289ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.29ms    0.96ms  38.24ms   92.68%
    Req/Sec     2.64k   301.51     6.90k    78.64%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.16ms
 75.000%    1.59ms
 90.000%    2.00ms
 99.000%    4.90ms
 99.900%   11.96ms
 99.990%   21.31ms
 99.999%   33.73ms
100.000%   38.27ms

#[Mean    =        1.292, StdDeviation   =        0.963]
#[Max     =       38.240, Total count    =      1699187]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  1799637 requests in 3.00m, 116.28MB read
Requests/sec:   9998.01
Transfer/sec:    661.52KB
```

Итоги:
<ol>
<li>обработано 179975 запросов</li>
<li>прочитано 11.46MB данных</li>
<li>сервер выдерживает заданную нагрузку на уровне 1499.77 запросов в секунду</li>
</ol>

Выполним профилирование сервера для одного потока.


```
./profiler.sh -e cpu -d 15 -f cpu-get-single.svg 240038
```
![CPU GET SINGLE](/async/cpu-get-single.svg)

Процессор выделяет 55.6% ресурсов на асинхронный сервис, 21.26% ресурсов на получение задач потоков и 22.73% ресурсов - на обработку селектора потоков onenio. На работу DAO выделяется 51.33% ресурсов процессора.

```
./profiler.sh -e alloc -d 15 -f alloc-get-single.svg 240038
```
![ALLOC GET SINGLE](/async/alloc-get-single.svg)

Общая память выделяется на Java-поток (50.17%) и селектор потока сервера (49.83%).

```
./profiler.sh -e lock -d 15 -f lock-get-single.svg 240038
```
![LOCK GET SINGLE](/async/lock-get-single.svg)

Блокировки Java-потока и селектора потока сервера делятся соответственно на 63.48% и 36.52%.