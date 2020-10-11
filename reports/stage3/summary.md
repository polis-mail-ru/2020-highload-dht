В рамках использования утилиты wrk2|profiler сервер имеет следующие показатели для put.
---------------------------------------------------------------------------------------

Скрипт PUT
----------------------------------------------------------------

rm wrkLogsPut.txt

../../wrk2/wrk -t1 -c65 -d15s -R2000 -s ../../wrk2/scripts/put.lua --u_latency http://localhost:8080 > wrkLogsPut.txt &

rm flamePutCpu.svg 

rm flamePutAlloc.svg 

rm flamePutLock.svg 

../../async-profiler-1.8.1-linux-x64/profiler.sh -d 5 -e cpu -f flamePutCpu.svg $(lsof -t -i :8080 -s TCP:LISTEN)

../../async-profiler-1.8.1-linux-x64/profiler.sh -d 5 -e alloc -f flamePutAlloc.svg $(lsof -t -i :8080 -s TCP:LISTEN)

../../async-profiler-1.8.1-linux-x64/profiler.sh -d 5 -e lock -f flamePutLock.svg $(lsof -t -i :8080 -s TCP:LISTEN)

SYNCHRONOUS 
-----------
Running 15s test @ http://localhost:8080
  
  1 threads and 65 connections
  
  Thread calibration: mean lat.: 3.774ms, rate sampling interval: 13ms
   
    Thread Stats   Avg      Stdev     Max   +/- Stdev
 
    Latency     4.67ms    2.54ms  14.96ms   66.55%
    Req/Sec     2.09k     2.01k    5.42k    42.30%
 
  Latency Distribution (HdrHistogram - Recorded Latency)
 
    50.000%    4.39ms
    75.000%    6.38ms
    90.000%    8.05ms
    99.000%    11.44ms
    99.900%    13.90ms
    99.990%    14.95ms
    99.999%    14.97ms
    100.000%   14.97ms


----------------------------------------------------------------
CPU put SYNC 
----------------------------------------------------
![alt text](sync/flamePutCpu.svg "put cpu")
------------------------------------------------------
ALLOC put SYNC
----------------------------------------------------
![alt text](sync/flamePutAlloc.svg "put alloc")

LOCK put SYNC
----------------------------------------------------
![alt text](sync/flamePutLock.svg "put lock")


ASYNCHRONOUS 
-----------    
 
Running 15s test @ http://localhost:8080
 
  1 threads and 65 connections
 
  Thread calibration: mean lat.: 1.139ms, rate sampling interval: 10ms
 
    Thread Stats   Avg      Stdev     Max   +/- Stdev

    Latency     1.15ms  563.93us   9.50ms   72.00%
    Req/Sec     2.11k   199.62     4.11k    75.11%
 
  Latency Distribution (HdrHistogram - Recorded Latency)

    50.000%     1.11ms
    75.000%     1.52ms
    90.000%     1.80ms
    99.000%     2.24ms
    99.900%     7.51ms
    99.990%     9.40ms
    99.999%     9.51ms
    100.000%    9.51ms

   

----------------------------------------------------------------
CPU put ASYNC
----------------------------------------------------
![alt text](async/flamePutCpu.svg "put cpu")
------------------------------------------------------
ALLOC put ASYNC
----------------------------------------------------
![alt text](async/flamePutAlloc.svg "put alloc")

LOCK put ASYNC
----------------------------------------------------
![alt text](async/flamePutLock.svg "put lock")







В рамках использования утилиты wrk2|profiler сервер имеет следующие показатели для get.
---------------------------------------------------------------------------------------

Скрипт GET
----------------------------------------------------------------

rm wrkLogsGet.txt

../../wrk2/wrk -t1 -c65 -d15s -R2000 -s ../../wrk2/scripts/get.lua --u_latency http://localhost:8080 > wrkLogsGet.txt &

rm flameGetCpu.svg 

rm flameGetAlloc.svg  

rm flameGetLock.svg 

../../async-profiler-1.8.1-linux-x64/profiler.sh -d 5 -e cpu -f flameGetCpu.svg $(lsof -t -i :8080 -s TCP:LISTEN)

../../async-profiler-1.8.1-linux-x64/profiler.sh -d 5 -e alloc -f flameGetAlloc.svg $(lsof -t -i :8080 -s TCP:LISTEN)

../../async-profiler-1.8.1-linux-x64/profiler.sh -d 5 -e lock -f flameGetLock.svg $(lsof -t -i :8080 -s TCP:LISTEN)

SYNCHRONOUS
----------------------------------------------------------------------

Running 15s test @ http://localhost:8080

  1 threads and 65 connections

  Thread calibration: mean lat.: 3.685ms, rate sampling interval: 13ms

      Thread Stats   Avg      Stdev     Max   +/- Stdev
      Latency     4.38ms    2.77ms  15.19ms   68.96%
      Req/Sec     2.09k     2.01k    5.42k    43.85%
 
  Latency Distribution (HdrHistogram - Recorded Latency)
    
    50.000%    3.95ms
    75.000%    5.99ms
    90.000%    8.30ms
    99.000%    12.20ms
    99.900%    14.42ms
    99.990%    15.14ms
    99.999%    15.20ms
    100.000%   15.20ms

----------------------------------------------------------------
CPU get SYNC
----------------------------------------------------
![alt text](sync/flameGetCpu.svg "get cpu")
------------------------------------------------------
ALLOC get SYNC
----------------------------------------------------
![alt text](sync/flameGetAlloc.svg "get alloc")

LOCK get SYNC
----------------------------------------------------
![alt text](sync/flameGetLock.svg "get lock")

ASYNCHRONOUS
----------------------------------------------------------------------

Running 15s test @ http://localhost:8080

  1 threads and 65 connections

  Thread calibration: mean lat.: 1.480ms, rate sampling interval: 10ms

    Thread Stats   Avg      Stdev     Max   +/- Stdev
    
    Latency     1.45ms  584.65us   3.59ms   65.22%
    Req/Sec     2.11k   174.40     2.78k    70.63%

  Latency Distribution (HdrHistogram - Recorded Latency)
 
    50.000%     1.40ms
    75.000%     1.86ms
    90.000%     2.27ms
    99.000%     2.84ms
    99.900%     3.25ms
    99.990%     3.52ms
    99.999%     3.59ms
    100.000%    3.59ms

----------------------------------------------------------------
CPU get ASYNC
----------------------------------------------------
![alt text](async/flameGetCpu.svg "get cpu")
------------------------------------------------------
ALLOC get ASYNC
----------------------------------------------------
![alt text](async/flameGetAlloc.svg "get alloc")

LOCK get ASYNC
----------------------------------------------------
![alt text](async/flameGetLock.svg "get lock")


Вывод:

В рамках нагрузочного тестирования для PUT/GET

№1 Потоков - 1 

№2 Соединений - 65

№3 Продолжительность - 15 секунд

№4 Количество запросов (Rate) - 2000

1) Put

SYNC
 Latency -> Avg. 4.67ms | Max. 14.96ms
 Req/Sec -> Avg. 2.09k  | Max. 5.42k 
ASYNC
 Latency -> Avg. 1.15ms | Max. 9.50ms
 Req/Sec -> Avg. 2.11k  | Max. 4.11k 


2) Get
 
SYNC
 Latency -> Avg. 4.38ms | Max. 15.19ms
 Req/Sec -> Avg. 2.09k  | Max. 5.42k 
ASYNC
 Latency -> Avg. 1.45ms | Max. 5.59ms
 Req/Sec -> Avg. 2.11k  | Max. 2.78k 

