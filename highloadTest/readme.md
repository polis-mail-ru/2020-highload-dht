#V1

##CPU GET

На графике видно:
1) Старт треда селектора начинается параллельно со стартом JIT процесса, который потребляет треть всех ресурсов, потому что процесс еще не оптмимизирован.
2) Дебаг логирование потребляет 17% ресурсов.
3) Логика DAO потребляет всего 10%, но некторые все таки находят свое value. Слишком поздно запустил тетирование, инкремент ключа сильно вырос и большая часть ключей по запросам не найдена.
4) Так же видно системные вызовы.

![Alt text](./async-profiler/cpu/getCPUv1.svg)

##CPU PUT

На графике видно:
1) JIT процесс потребляет всего 1.5%, процесс успел достаточно оптимизироваться.
2) Логгер снова жрет аж 23%.
3) Логика DAO всего лишь 3%, так мало скорей всего из-за того, что на сервер было отправлено недостаточное количество данных для флеша на диск, в этом случае потребление выростит в разы.
4) Появили процессы обработки реквеста и реквест боди.
4) Так же видно системные вызовы.

![Alt text](./async-profiler/cpu/putCPUv1.svg)

##ALLOC GET
Учитывая заммечания в CPU тесте просто опишу куда уходит большая часть ресурсов. 
1) DAO 5%, видимо совсем мало обстреливал.
2) Дебагг 51%
3) Чтение из сокета 11%
4) остальное получение хедеров, парсинг реквеста, иницилизация респонсна и его отправка.

![Alt text](./async-profiler/alloc/getAllocV1.svg)

##ALLOC PUT
Учитывая заммечания в CPU тесте просто опишу куда уходит большая часть ресурсов. 
1) DAO 33%
2) Логгер 37%
3) Остальное парсинг реквеста, иницилизирование респонса, жонглирование байт буфферами.

![Alt text](./async-profiler/alloc/putAllocV1.svg)

#UPDATE!

#V2

##CPU GET

1) Процесс прогрет
2) Не тратим цпу на логгирование
3) Перед профилированием сторач наполнен

![Alt text](./async-profiler/cpu/getCPUv2.svg)

##CPU PUT

1) Появился флаш

![Alt text](./async-profiler/cpu/putCPUv2.svg)

##ALLOC GET

![Alt text](./async-profiler/alloc/getAllocV2.svg)

##ALLOC PUT

![Alt text](./async-profiler/alloc/putAllocV2.svg)

#Результаты wkr2: 

##GET

    wrk -t1 -c1 -d5m -R2000 --latency -s wrk2/getScript.lua http://127.0.0.1:8080
    Running 5m test @ http://127.0.0.1:8080
      1 threads and 1 connections
      Thread calibration: mean lat.: 7.678ms, rate sampling interval: 42ms
      Thread Stats   Avg      Stdev     Max   +/- Stdev
        Latency   133.54ms  294.69ms   1.21s    86.04%
        Req/Sec     2.01k   473.12     4.80k    74.63%
      Latency Distribution (HdrHistogram - Recorded Latency)
     50.000%    3.32ms
     75.000%   20.37ms
     90.000%  694.78ms
     99.000%    1.07s 
     99.900%    1.19s 
     99.990%    1.21s 
     99.999%    1.21s 
    100.000%    1.21s
    #[Mean    =      133.544, StdDeviation   =      294.695]
    #[Max     =     1212.416, Total count    =       322016]
    #[Buckets =           27, SubBuckets     =         2048]
    ----------------------------------------------------------
      342024 requests in 2.87m, 24.02MB read
    Requests/sec:   1988.42
    Transfer/sec:    143.01KB 


##PUT

    wrk -t1 -c1 -d5m -R2000 --latency -s wrk2/putScript.lua http://127.0.0.1:8080
    Running 5m test @ http://127.0.0.1:8080
    1 threads and 1 connections
    Thread calibration: mean lat.: 1.003ms, rate sampling interval: 10ms
    Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     8.86ms   79.34ms   1.26s    98.79%
    Req/Sec     2.11k     1.11k   61.11k    98.58%
    Latency Distribution (HdrHistogram - Recorded Latency)
    50.000%    0.98ms
    75.000%    1.34ms
     90.000%    1.68ms
     99.000%  282.11ms
     99.900%    1.11s 
     99.990%    1.24s 
     99.999%    1.26s 
    100.000%    1.26s
    #[Mean    =      133.544, StdDeviation   =      294.695]
    #[Max     =     1212.416, Total count    =       322016]
    #[Buckets =           27, SubBuckets     =         2048]
    ----------------------------------------------------------
      342024 requests in 2.87m, 24.02MB read
    Requests/sec:   1988.42
    Transfer/sec:    143.01KB
    
###TASK_3

##PUT

    wrk -t4 -c16 -d40s -R10000 --latency -s wrk2/putScript.lua http://127.0.0.1:8080
    
    Running 40s test @ http://127.0.0.1:8080
      4 threads and 16 connections
      Thread Stats   Avg      Stdev     Max   +/- Stdev
        Latency   701.20us  398.11us  11.06ms   69.61%
        Req/Sec     2.58k   146.61     4.55k    77.68%
      Latency Distribution (HdrHistogram - Recorded Latency)
     50.000%  684.00us
     75.000%    0.98ms
     90.000%    1.15ms
     99.000%    1.34ms
     99.900%    4.43ms
     99.990%    8.06ms
     99.999%   10.06ms
    100.000%   11.07ms
    #[Mean    =        0.701, StdDeviation   =        0.398]
    #[Max     =       11.064, Total count    =       299791]
    #[Buckets =           27, SubBuckets     =         2048]
    ----------------------------------------------------------
      399919 requests in 40.00s, 25.55MB read
    Requests/sec:   9998.20
    Transfer/sec:    654.18KB

![Alt text](./async-profiler/cpu/putCPUv3.svg)

![Alt text](./async-profiler/alloc/putAllocV3.svg)

###GET

    wrk -t4 -c16 -d40s -R10000 --latency -s wrk2/getScript.lua http://127.0.0.1:8080
    
    Running 40s test @ http://127.0.0.1:8080
      4 threads and 16 connections
      Thread Stats   Avg      Stdev     Max   +/- Stdev
        Latency    12.12s     4.54s   19.86s    57.85%
        Req/Sec     1.18k    19.89     1.21k    50.00%
      Latency Distribution (HdrHistogram - Recorded Latency)
     50.000%   12.22s 
     75.000%   16.06s 
     90.000%   18.35s 
     99.000%   19.69s 
     99.900%   19.84s 
     99.990%   19.86s 
     99.999%   19.87s 
    100.000%   19.87s 
    #[Mean    =    12121.006, StdDeviation   =     4537.780]
    #[Max     =    19857.408, Total count    =       142173]
    #[Buckets =           27, SubBuckets     =         2048]
    ----------------------------------------------------------
      201494 requests in 40.00s, 13.95MB read
    Requests/sec:   5037.42
    Transfer/sec:    357.05KB

![Alt text](./async-profiler/cpu/getCPUv3.svg)

![Alt text](./async-profiler/alloc/getAllocV3.svg)

#TASK_4

##PUT

    wrk -t4 -c32 -d40s -R10000 --latency -s wrk2/putScript.lua http://127.0.0.1:8080
    Running 40s test @ http://127.0.0.1:8080
      4 threads and 32 connections
      Thread calibration: mean lat.: 0.853ms, rate sampling interval: 10ms
      Thread calibration: mean lat.: 0.862ms, rate sampling interval: 10ms
      Thread calibration: mean lat.: 0.833ms, rate sampling interval: 10ms
      Thread calibration: mean lat.: 0.870ms, rate sampling interval: 10ms
      Thread Stats   Avg      Stdev     Max   +/- Stdev
        Latency   847.84us  610.21us  26.70ms   91.97%
        Req/Sec     2.58k   193.96     8.33k    89.22%
      Latency Distribution (HdrHistogram - Recorded Latency)
     50.000%  822.00us
     75.000%    1.11ms
     90.000%    1.32ms
     99.000%    1.66ms
     99.900%    7.45ms
     99.990%   20.90ms
     99.999%   24.58ms
    100.000%   26.72ms
    
    #[Mean    =        0.848, StdDeviation   =        0.610]
    #[Max     =       26.704, Total count    =       299594]
    #[Buckets =           27, SubBuckets     =         2048]
    ----------------------------------------------------------
      399829 requests in 40.00s, 29.28MB read
    Requests/sec:   9995.88
    Transfer/sec:    749.47KB

###CPU 

![Alt text](./async-profiler/cpu/putCPUv4.svg)

![Alt text](./async-profiler/cpu/putCPUwithThreadsV4.svg)

###ALLOC

![Alt text](./async-profiler/alloc/putAllocV4.svg)

![Alt text](./async-profiler/alloc/putAllocWithThreadsV4.svg)

##GET

    wrk -t4 -c32 -d40s -R10000 --latency -s wrk2/getScript.lua http://127.0.0.1:8080
    Running 40s test @ http://127.0.0.1:8080
      4 threads and 32 connections
      Thread calibration: mean lat.: 0.999ms, rate sampling interval: 10ms
      Thread calibration: mean lat.: 0.975ms, rate sampling interval: 10ms
      Thread calibration: mean lat.: 0.959ms, rate sampling interval: 10ms
      Thread calibration: mean lat.: 0.960ms, rate sampling interval: 10ms
      Thread Stats   Avg      Stdev     Max   +/- Stdev
        Latency   842.52us  657.98us  20.24ms   95.18%
        Req/Sec     2.57k   209.39     7.78k    91.13%
      Latency Distribution (HdrHistogram - Recorded Latency)
     50.000%  814.00us
     75.000%    1.10ms
     90.000%    1.30ms
     99.000%    1.59ms
     99.900%   11.84ms
     99.990%   18.00ms
     99.999%   20.01ms
    100.000%   20.25ms
    #[Mean    =        0.843, StdDeviation   =        0.658]
    #[Max     =       20.240, Total count    =       299593]
    #[Buckets =           27, SubBuckets     =         2048]
    ----------------------------------------------------------
      399822 requests in 40.00s, 31.48MB read
    Requests/sec:   9995.60
    Transfer/sec:    805.95KB
    
###CPU 

![Alt text](./async-profiler/cpu/getCPUv4.svg)

![Alt text](./async-profiler/cpu/getCPUwithThreadsV4.svg)

###ALLOC

![Alt text](./async-profiler/alloc/getAllocV4.svg)

![Alt text](./async-profiler/alloc/getAllocWithThreadsV4.svg)

###LOCK

![Alt text](./async-profiler/lock/getLockV4.svg)

![Alt text](./async-profiler/lock/putLockV4.svg)