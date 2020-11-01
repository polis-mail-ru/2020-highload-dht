# Нагрузочное тестирование

Подробные логи wrk 

[Второй этап](https://github.com/IvanovAndrey/2020-highload-dht/blob/task3/HighloadLog2)

[Третий этап](https://github.com/IvanovAndrey/2020-highload-dht/blob/task3/HighloadLog3)
## Запрос put
### wrk
#### Task2
Параметры запуска

```
./wrk -t4 -c16 -d40s -R10000 -s put.lua --latency http://127.0.0.1:8080

```

Полученный результат

```
Running 40s test @ http://127.0.0.1:8080
  4 threads and 16 connections
  Thread calibration: mean lat.: 16.384ms, rate sampling interval: 99ms
  Thread calibration: mean lat.: 20.912ms, rate sampling interval: 148ms
  Thread calibration: mean lat.: 15.425ms, rate sampling interval: 108ms
  Thread calibration: mean lat.: 33.305ms, rate sampling interval: 243ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.65ms    1.20ms  30.94ms   87.26%
    Req/Sec     2.51k    45.74     2.90k    92.14%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.48ms
 75.000%    2.08ms
 90.000%    2.67ms
 99.000%    5.91ms
 99.900%   14.29ms
 99.990%   24.48ms
 99.999%   29.36ms
100.000%   30.96ms

#[Mean    =        1.647, StdDeviation   =        1.202]
#[Max     =       30.944, Total count    =       299798]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  399909 requests in 40.00s, 25.55MB read
Requests/sec:   9997.93
Transfer/sec:    654.16KB
```
#### Task 3
Параметры запуска

```
./wrk -t4 -c64 -d40s -R10000 -s put.lua --latency http://127.0.0.1:8080

```

Полученный результат

```
Running 40s test @ http://127.0.0.1:8080
  4 threads and 64 connections
  Thread calibration: mean lat.: 2.649ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.735ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.86ms    5.33ms  73.28ms   96.68%
    Req/Sec     2.65k   618.80     8.90k    79.63%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.96ms
 75.000%    2.74ms
 90.000%    3.85ms
 99.000%   27.22ms
 99.900%   63.94ms
 99.990%   68.99ms
 99.999%   72.25ms
100.000%   73.34ms

#[Mean    =        2.863, StdDeviation   =        5.329]
#[Max     =       73.280, Total count    =       299184]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  397218 requests in 40.00s, 25.38MB read
Requests/sec:   9930.57
Transfer/sec:    649.75KB
```
В случае с запросом put асинхронный вариант показывает более плохие цифры и по средней и по максимальной задержке.

### Async-profiler 
#### Task2
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/c11c71d7ae0d161e1b7c04455b862d7568064cc4/async/put_cpu_m.svg)

#### Task3
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/762d0e241a57c1f93160fd832d9bfef5d8242287/async/put_cpu_a.svg)

На асинхронном варианте видим два селектора и два воркера. Каждый воркер занимает по 40%, что примерно соответствует проценту, который занимает процесс ServiceImpl в 
вариате с синхронным выполнением.

### Allocation
#### Task2
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/c11c71d7ae0d161e1b7c04455b862d7568064cc4/async/put_alloc_m.svg)
#### Task3
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/762d0e241a57c1f93160fd832d9bfef5d8242287/async/put_alloc_a.svg)

Так же в асинхроннм варианте видны два воркера и два селектора, но по процентам используемой памяти изменения не сильные. Все те же функции 
используют всю ту же память, но добавились функции связи между селектором и воркером. При этом в обоих случаях мы тратили достаточно много на выделение памяти под парсинг запроса.
Но в асинхронной реализации граф показывает что мы тратили на это меньше

### Lock
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/762d0e241a57c1f93160fd832d9bfef5d8242287/async/put_lock_a.svg)

В асинхронной реализации появились локи и в этом главное отличие двух реализаций. На прошлом этапе у нас ничего нигде не блокировалось, а в асинхронной реализации уже показывает себя ArrayBlockingQueue.
Как бы то нибыло, все потоки одинаковых размеров, а значит ни один из потоков не доминирует. (Хоть это и не так наглядщно для двух потоков.)

## Запрос get
### wrk
### Task2
Параметры запуска

```
./wrk -t4 -c16 -d40s -R10000 -s get.lua --latency http://127.0.0.1:8080

```

Полученный результат

```
Running 40s test @ http://127.0.0.1:8080
  4 threads and 16 connections
  Thread calibration: mean lat.: 1.946ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.937ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.945ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.026ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.23ms    3.25ms  67.39ms   95.18%
    Req/Sec     2.67k   627.89    11.44k    86.95%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.64ms
 75.000%    2.32ms
 90.000%    3.35ms
 99.000%   15.49ms
 99.900%   48.67ms
 99.990%   57.63ms
 99.999%   66.37ms
100.000%   67.46ms
#[Mean    =        2.230, StdDeviation   =        3.255]
#[Max     =       67.392, Total count    =       299800]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  399914 requests in 40.00s, 25.51MB read
Requests/sec:   9997.35
Transfer/sec:    653.04KB
```
#### Task3
Параметры запуска

```
./wrk -t4 -c64 -d40s -R10000 -s get.lua --latency http://127.0.0.1:8080

```

Полученный результат

```
Running 40s test @ http://127.0.0.1:8080
  4 threads and 64 connections
  Thread calibration: mean lat.: 2.079ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.249ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.318ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.229ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.76ms    1.32ms  34.82ms   89.00%
    Req/Sec     2.65k   437.78    11.00k    76.13%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.54ms
 75.000%    2.19ms
 90.000%    2.89ms
 99.000%    6.42ms
 99.900%   15.44ms
 99.990%   27.58ms
 99.999%   33.31ms
100.000%   34.85ms
#[Mean    =        1.759, StdDeviation   =        1.320]
#[Max     =       34.816, Total count    =       299202]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  399632 requests in 40.00s, 25.49MB read
Requests/sec:   9990.98
Transfer/sec:    652.62KB
```
В данном случае более хорошие цифры показывает асинхронная реализация.

### Async-profiler 
#### Task2
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/762d0e241a57c1f93160fd832d9bfef5d8242287/async/get_cpu_m.svg)

#### Task3
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/762d0e241a57c1f93160fd832d9bfef5d8242287/async/get_cpu_a.svg)

Основное заметное отличие заключается в том, что в синхронной реализации на работу с базой выделялось 23 процента времени для каждого из потоков, а в асинхронной 9.
### Allocation
#### Task2
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/762d0e241a57c1f93160fd832d9bfef5d8242287/async/get_alloc_m.svg)
#### Task3
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/762d0e241a57c1f93160fd832d9bfef5d8242287/async/get_alloc_a.svg)

Ожидаемо память выделяется отдельно под воркеры и под селекторы. Проценты на запрос в базу и на ответ не сильно отличаются .
Много памяти тратится на парсинг запросов в обоих случаях. 

### Lock
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/762d0e241a57c1f93160fd832d9bfef5d8242287/async/get_lock_a.svg)

Тот же результат, что и для put и те же выводы из него.
