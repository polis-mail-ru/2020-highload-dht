### PUT

![CPU PUT](async/cpu-put.svg)

![ALLOC PUT](async/alloc-put.svg)

```
wrk2 -t4 -c20 -d60s -R10000 -s wrk/put.lua --latency http://127.0.0.1:8080
Running 1m test @ http://127.0.0.1:8080
  4 threads and 20 connections
  Thread calibration: mean lat.: 1.233ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.226ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.230ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.266ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.25ms    1.23ms  47.78ms   98.62%
    Req/Sec     2.64k   229.67     7.70k    81.97%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.14ms
 75.000%    1.57ms
 90.000%    1.90ms
 99.000%    2.64ms
 99.900%   22.80ms
 99.990%   34.65ms
 99.999%   45.06ms
100.000%   47.81ms

#[Mean    =        1.251, StdDeviation   =        1.227]
#[Max     =       47.776, Total count    =       499752]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  599901 requests in 1.00m, 38.33MB read
Requests/sec:   9998.21
Transfer/sec:    654.18KB
```

### GET 

![CPU GET](async/cpu-get.svg)

![ALLOC GET](async/alloc-get.svg)

```
wrk2 -t4 -c20 -d60s -R10000 -s wrk/get.lua --latency http://127.0.0.1:8080
Running 1m test @ http://127.0.0.1:8080
  4 threads and 20 connections
  Thread calibration: mean lat.: 1.029ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.045ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.121ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.063ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.08ms  616.23us  27.23ms   77.36%
    Req/Sec     2.63k   209.46     5.30k    81.35%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.03ms
 75.000%    1.42ms
 90.000%    1.74ms
 99.000%    2.24ms
 99.900%    7.54ms
 99.990%   13.85ms
 99.999%   24.14ms
100.000%   27.25ms

#[Mean    =        1.083, StdDeviation   =        0.616]
#[Max     =       27.232, Total count    =       499728]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  599880 requests in 1.00m, 38.48MB read
Requests/sec:   9997.05
Transfer/sec:    656.63KB
```
 