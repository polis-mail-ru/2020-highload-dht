# Нагрузочное тестирование

## Запрос put
### wrk

Параметры запуска

```
./wrk -t4 -c64 -d40s -R8000 -s put.lua --latency http://127.0.0.1:8080

```

Полученный результат

```
Running 40s test @ http://127.0.0.1:8080
  4 threads and 64 connections
  Thread calibration: mean lat.: 2911.494ms, rate sampling interval: 9388ms
  Thread calibration: mean lat.: 2815.296ms, rate sampling interval: 9060ms
  Thread calibration: mean lat.: 2514.054ms, rate sampling interval: 8245ms
  Thread calibration: mean lat.: 2795.504ms, rate sampling interval: 9035ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    13.13s     4.68s   29.57s    58.43%
    Req/Sec     0.94k    43.09     1.03k    75.00%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%   13.34s 
 75.000%   16.97s 
 90.000%   19.15s 
 99.000%   23.48s 
 99.900%   27.97s 
 99.990%   29.38s 
 99.999%   29.57s 
100.000%   29.59s 
```

Полученный результат неудовлетворительный. Так сильно отличается он от предидущего потому что я сменил машину на еще менее мощную с последнего раза и временно не имею доступа
к старой. Поэтому снизим нагрузку. 


```
./wrk -t4 -c64 -d40s -R1000 -s put.lua --latency http://127.0.0.1:8080

```

Полученный результат

```
Running 40s test @ http://127.0.0.1:8080
  4 threads and 64 connections
  Thread calibration: mean lat.: 2.177ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.120ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.050ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.021ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.08ms    0.97ms  26.02ms   81.55%
    Req/Sec   264.49    104.06   800.00     76.02%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.95ms
 75.000%    2.48ms
 90.000%    3.07ms
 99.000%    5.00ms
 99.900%   11.75ms
 99.990%   21.74ms
 99.999%   26.03ms
100.000%   26.03ms

  39992 requests in 40.00s, 3.17MB read
Requests/sec:    999.74
Transfer/sec:     81.03KB
```

В таком варианте клиент хотя бы не умрет от старости, ожидая свою котики.jpg

Параметры запуска Async-profiler во всей лабораторной 

```
./profiler.sh -d 15 -f %имя%.svg -e cpu/alloc/lock PID
```

### Async-profiler 
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/2307422ad0fde0bbad2492a95d706e1fa444c86b/async/Task5/cpu_put_5.svg)

Первое что бросается в глаза - сильно возросший размер Selector относительно Worker. Это происходит потому, что селектор ожидает ответа от нескольких других узлов.
Так же видим большое влияние на общую картину внедрения типа Future. 

### Allocation
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/2307422ad0fde0bbad2492a95d706e1fa444c86b/async/Task5/alloc_put_5.svg)

Здесь отличие от предидущих версий в том, что добавились траты памяти на sendToReplicas (7.8%). Это по крайней мере доказывает, что концепция воплощениа в жизнь.
(Ну и тесты это тоже доказывают конечно же).
Так же опять видим больше количество памяти, выделяемое под Future.

### Lock
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/2307422ad0fde0bbad2492a95d706e1fa444c86b/async/Task5/lock_put_5.svg)

Видим блокировки в появившемся методе submit. Блокировок в потоках стало больше, потому что потоки простаивают в ожидании задач.

## Запрос get
### wrk

Параметры запуска

```
/wrk -t4 -c16 -d40s -R1000 -s get.lua --latency http://127.0.0.1:8080

```

Полученный результат

```
Running 40s test @ http://127.0.0.1:8080
  4 threads and 64 connections
  Thread calibration: mean lat.: 4.166ms, rate sampling interval: 14ms
  Thread calibration: mean lat.: 4.270ms, rate sampling interval: 15ms
  Thread calibration: mean lat.: 4.366ms, rate sampling interval: 15ms
  Thread calibration: mean lat.: 4.562ms, rate sampling interval: 16ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     4.12ms    2.81ms  35.87ms   87.68%
    Req/Sec   258.71     74.30   538.00     77.84%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    3.31ms
 75.000%    4.83ms
 90.000%    7.14ms
 99.000%   15.19ms
 99.900%   27.84ms
 99.990%   35.01ms
 99.999%   35.90ms
100.000%   35.90ms

  39989 requests in 40.00s, 3.12MB read
Requests/sec:    999.68
Transfer/sec:     79.94KB
```

### Async-profiler 
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/2307422ad0fde0bbad2492a95d706e1fa444c86b/async/Task5/get_cpu_5.svg)

Общая картина аналогична тому, что мы видели выше в другом методе.


### Allocation
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/2307422ad0fde0bbad2492a95d706e1fa444c86b/async/Task5/get_alloc_5.svg)

Основные расходы памяти связаны с пересылкаими между нодами (как и было), отправкой ответа (как и было с самого начала буквально)
и хранением ответственных за исполнение запроса реплик (как стало только с этого этапа).

### Lock
![alt text](https://raw.githubusercontent.com/IvanovAndrey/2020-highload-dht/2307422ad0fde0bbad2492a95d706e1fa444c86b/async/Task5/get_lock_5.svg)

Тут изменния тоже анологичны.

Увы, но не получится сравнить общие результаты с прошлым этапом, но паучье чутье подсказывает мне,
что у надежности есть своя цена и быстродействие должно было снизиться.

Однако на достаточно мощных машинах (мощнее чем виртуалка на два ядра на умирающем ноуте) цифры должны быть неплохими даже при солидных нагрузках.


