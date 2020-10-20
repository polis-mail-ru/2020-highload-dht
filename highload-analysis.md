### PUT

Параметры запуска:
<ol>
<li>4 потока</li>
<li>64 открытых соединения</li>
<li>2 минуты работы</li>
<li>5000 запросов в секунду</li>
</ol>

```
/async-profiler -d 15 -f cpu-put.svg -e cpu 291344
```

![CPU PUT](/async/cpu-put.svg)
Деление ресурсов между потоком работы и селектором делится в соответствии 63.55% и 30.30%. На пересылку запроса(функция passOn) тратится примерно 20.69%, на саму функцию put 19.21%.

```
/async-profiler -d 15 -f alloc-put.svg -e alloc 291344
```

![ALLOC PUT](/async/alloc-put.svg)
В данном случае поток работы занимает 70.25% ресурсов, из которых на пересылку тратится 43.15% а на выполнение put-а 25.16%. На селектор тратится 29.75%.

```
/async-profiler -d 15 -f lock-put.svg -e lock 291344
```

![LOCK PUT](/async/lock-put.svg)
Блокировки потоков работы отнимают практически 100%.

```
wrk2 -t4 -c64 -d2m -R5000 -s wrk/put.lua —latency http://127.0.0.1:8080
Running 2m test @ http://127.0.0.1:8080
4 threads and 64 connections
Thread calibration: mean lat.: 2.857ms, rate sampling interval: 10ms
Thread calibration: mean lat.: 2.903ms, rate sampling interval: 10ms
Thread calibration: mean lat.: 2.962ms, rate sampling interval: 10ms
Thread calibration: mean lat.: 2.822ms, rate sampling interval: 10ms
Thread Stats Avg Stdev Max +/- Stdev
Latency 3.92ms 14.27ms 193.41ms 95.26%
Req/Sec 1.32k 245.42 5.33k 94.05%
Latency Distribution (HdrHistogram - Recorded Latency)
50.000% 1.06ms
75.000% 1.45ms
90.000% 2.08ms
99.000% 83.07ms
99.900% 154.24ms
99.990% 178.94ms
99.999% 189.95ms
100.000% 193.54ms
 
#[Mean = 3.925, StdDeviation = 14.267]
#[Max = 193.408, Total count = 549592]
#[Buckets = 27, SubBuckets = 2048]
----------------------------------------------------------
 
599834 requests in 2.00m, 52.06MB read
Requests/sec: 4998.64
Transfer/sec: 444.22KB 
Transfer/sec: 445.48KB
```
Итоги:
<ol>
<li>обработано 599834 запроса за 2 минуты</li>
<li>прочитано 52.06MB данных</li>
<li>сервер держит заданную нагрузку на уровне 4998.64 запросов в секунду</li>
</ol>

### GET 

Параметры запуска:
<ol>
<li>4 потока</li>
<li>64 открытых соединения</li>
<li>2 минуты работы</li>
<li>5000 запросов в секунду</li>
</ol>

```
/async-profiler -d 15 -f cpu-get.svg -e cpu 291344 
```

![CPU GET](/async/cpu-get.svg)
Деление ресурсов между потоком работы и селектором делится в соответствии 64.51% и 29.01%. На пересылки тратится примерно 23.669%, на саму функцию get 18.87%.

```
/async-profiler -d 15 -f alloc-get.svg -e alloc 291344
```

![ALLOC GET](/async/alloc-get.svg)
В данном случае поток работы занимает 74.16% ресурсов, из которых на пересылку тратится 50.76% а на выполнение get-а 20.34%. На селектор тратится 25.84%.

```
/async-profiler -d 15 -f lock-get.svg -e lock 291344
```

![LOCK GET](/async/lock-get.svg)
Блокировки потоков работы отнимают практически 100%.

```
wrk2 -t4 -c64 -d2m -R5000 -s wrk/get.lua —latency http://127.0.0.1:8080
Running 2m test @ http://127.0.0.1:8080
4 threads and 64 connections
Thread calibration: mean lat.: 1.314ms, rate sampling interval: 10ms
Thread calibration: mean lat.: 1.325ms, rate sampling interval: 10ms
Thread calibration: mean lat.: 1.313ms, rate sampling interval: 10ms
Thread calibration: mean lat.: 1.305ms, rate sampling interval: 10ms
Thread Stats Avg Stdev Max +/- Stdev
Latency 3.85ms 12.85ms 173.06ms 94.86%
Req/Sec 1.32k 237.91 6.00k 91.34%
Latency Distribution (HdrHistogram - Recorded Latency)
50.000% 1.07ms
75.000% 1.47ms
90.000% 3.50ms
99.000% 68.42ms
99.900% 140.29ms
99.990% 158.98ms
99.999% 169.98ms
100.000% 173.18ms
 
#[Mean = 3.846, StdDeviation = 12.850]
#[Max = 173.056, Total count = 549595]
#[Buckets = 27, SubBuckets = 2048]
----------------------------------------------------------
599836 requests in 2.00m, 52.20MB read
Requests/sec: 4998.63
Transfer/sec: 445.48KB
```
Итоги:
<ol>
<li>обработано 599836 запроса за 2 минуты</li>
<li>прочитано 52.20MB данных</li>
<li>сервер держит заданную нагрузку на уровне 4998.63 запросов в секунду</li>
</ol>
