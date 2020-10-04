В рамках использования утилиты wrk2|profiler сервер имеет следующие показатели для put.
---------------------------------------------------------------------------------------
1) Команды 

rm wrkLogsPut.txt

./wrk2/wrk -t1 -c1 -d15s -R2000 -s ./wrk2/scripts/put.lua --u_latency http://localhost:8080/v0/entity?id=100 > wrkLogsPut.txt &

rm flamePutCpu.svg 

rm flamePutAlloc.svg 

./async-profiler-1.8.1-linux-x64/profiler.sh -d 5 -e cpu -f flamePutCpu.svg $(lsof -t -i :8080 -s TCP:LISTEN)

./async-profiler-1.8.1-linux-x64/profiler.sh -d 5 -e alloc -f flamePutAlloc.svg $(lsof -t -i :8080 -s TCP:LISTEN)

----------------------------------------------------------------------------------------

Running 15s test @ http://localhost:8080
  1 threads and 1 connections
  Thread calibration: mean lat.: 1.139ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.11ms  501.07us   2.35ms   61.82%
    Req/Sec     2.10k   168.83     2.56k    65.73%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.11ms
 75.000%    1.48ms
 90.000%    1.82ms
 99.000%    2.11ms
 99.900%    2.20ms
 99.990%    2.28ms
 99.999%    2.35ms
100.000%    2.35ms

----------------------------------------------------------------
CPU put
----------------------------------------------------
![alt text](flamePutCpu.svg "put cpu")
------------------------------------------------------
ALLOC put
----------------------------------------------------
![alt text](flamePutAlloc.svg "put alloc")


----------------------------------------------------------------------------------------
В рамках использования утилиты wrk2|profiler сервер имеет следующие показатели для get.
---------------------------------------------------------------------------------------
1) Команды 

rm wrkLogsGet.txt

./wrk2/wrk -t1 -c1 -d15s -R2000 -s ./wrk2/scripts/get.lua --u_latency http://localhost:8080/v0/entity?id=100 > wrkLogsGet.txt &

rm flameGetCpu.svg 

rm flameGetAlloc.svg  

./async-profiler-1.8.1-linux-x64/profiler.sh -d 5 -e cpu -f flameGetCpu.svg $(lsof -t -i :8080 -s TCP:LISTEN)

./async-profiler-1.8.1-linux-x64/profiler.sh -d 5 -e alloc -f flameGetAlloc.svg $(lsof -t -i :8080 -s TCP:LISTEN)


----------------------------------------------------------------------------------------------
Running 15s test @ http://localhost:8080
  1 threads and 1 connections
  Thread calibration: mean lat.: 1.106ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.12ms  777.72us  16.53ms   87.02%
    Req/Sec     2.11k   195.62     4.00k    72.53%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.08ms
 75.000%    1.43ms
 90.000%    1.80ms
 99.000%    2.14ms
 99.900%   13.17ms
 99.990%   16.25ms
 99.999%   16.54ms
100.000%   16.54ms


CPU get
-----------------------------------------------------------------------------------------------
![alt text](flameGetCpu.svg "get cpu")

ALLOC get
------------------------------------------------------------------------------------------------
![alt text](flameGetAlloc.svg "get alloc")
------------------------------------------------------------------------------------------------

Выводы:

Из данных профилирования видно, что разброс во времени задержки 
и запросов в секунду больше у PUT чем у GET. 
Это скорее всего связано с вызовом дополнительной функциональности 
для сохранения тела запроса в RocksDB, которую можно заметить на flamegraph 
для PUT cpu. Так же из flamegraph PUT alloc можно заметить, 
что он гораздо более дробленный по сравнению с GET alloc. 
Это означает, что аллокации памяти происходят более решулярно и 
так же скорее всего замедляет в некоторых запросах скорость работы т.к.
процессор не может эффективно предсказать какие команды надо загружать 
в кэш-память процессора.


PUT
1) Задрежка средняя - 1.1 мс. (Макс. - 2.35 мс.)
2) Запросов в секунду в среднем - 2100 (Макс. - 2670)
3) 90% запросов укладываются в 1.80ms.

GET
1) Задрежка средняя - 1.12 мс. (Макс. - 16.5 мс.)
2) Запросов в секунду в среднем - 2110 (Макс. - 4000)
3) 90% запросов укладываются в 1.81ms.