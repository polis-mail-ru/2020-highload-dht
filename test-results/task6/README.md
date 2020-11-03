# PUT
wrk2 -t64 -c64 -d30s -R* -s test-results/task5/wrk2-scripts/put-request.lua --latency\
10k:
```Running 1m test @ http://localhost:8080
  64 threads and 64 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     0.90ms  745.27us  23.63ms   97.14%
    Req/Sec   164.53     54.85   555.00     45.71%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%  830.00us
 75.000%    1.09ms
 90.000%    1.25ms
 99.000%    3.69ms
 99.900%   10.32ms
 99.990%   13.62ms
 99.999%   19.33ms
100.000%   23.65ms
#[Mean    =        0.902, StdDeviation   =        0.745]
#[Max     =       23.632, Total count    =       499891]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  600049 requests in 1.00m, 38.34MB read
Requests/sec:  10000.90
Transfer/sec:    654.36KB
```

## CPU
![CPU](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/task6/test-results/task6/cpu_put.svg)

## ALLOC

![ALLOC](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/task6/test-results/task6/alloc_put.svg)

## LOCK:

![LOCK](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/task6/test-results/task6/lock_put.svg)

# GET
wrk2 -t64 -c64 -d30s -R* -s test-results/lua/wrk2-scripts/get-request.lua --latency\\
10k:
```
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   772.32us  420.91us  17.36ms   82.32%
    Req/Sec   163.21     52.78   444.00     48.92%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%  758.00us
 75.000%    1.01ms
 90.000%    1.16ms
 99.000%    1.46ms
 99.900%    4.33ms
 99.990%   13.32ms
 99.999%   16.82ms
100.000%   17.38ms

#[Mean    =        0.772, StdDeviation   =        0.421]
#[Max     =       17.360, Total count    =       199891]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  300048 requests in 30.00s, 19.17MB read
Requests/sec:  10002.09
Transfer/sec:    654.43KB
```

## CPU
![CPU](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/task6/test-results/task6/cpu_get.svg)

## ALLOC

![ALLOC](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/task6/test-results/task6/alloc_get.svg)

## LOCK

![LOCK](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/task6/test-results/task6/lock_get.svg)
