1. PUT: \
WRK2:  ~/wrk2/wrk -c1 -t1 -R2000 -d 60s --latency -s lua/wrk2-scripts/put-request.lua http://127.0.0.1:8080 \
Running 1m test @ http://127.0.0.1:8080 \
  1 threads and 1 connections \
  Thread calibration: mean lat.: 366.477ms, rate sampling interval: 1164ms \
  Thread Stats   Avg      Stdev     Max   +/- Stdev \
    Latency     2.09s   796.25ms   3.37s    56.04% \
    Req/Sec     1.89k    31.45     1.95k    76.19% \
  Latency Distribution (HdrHistogram - Recorded Latency) \
 50.000%    2.09s \
 75.000%    2.80s \
 90.000%    3.11s \
 99.000%    3.35s \
 99.900%    3.37s \
 99.990%    3.38s \
 99.999%    3.38s \
100.000%    3.38s \
PROFILING: \
![PUT](https://github.com/s3ponia/2020-highload-dht/blob/master/test-results/profiler-put.svg)
2. DELETE: \
WRK2: ~/wrk2/wrk -c1 -t1 -R2000 -d 60s --latency -s lua/wrk2-scripts/delete-request.lua http://127.0.0.1:8080 > wrk2-delete-latency \
Running 1m test @ http://127.0.0.1:8080 \
  1 threads and 1 connections \
  Thread calibration: mean lat.: 0.894ms, rate sampling interval: 10ms \
  Thread Stats   Avg      Stdev     Max   +/- Stdev \
    Latency     0.90ms  379.11us   7.27ms   70.92% \
    Req/Sec     2.11k   135.79     3.56k    84.08% \
  Latency Distribution (HdrHistogram - Recorded Latency) \
 50.000%    0.88ms \
 75.000%    1.15ms \
 90.000%    1.31ms \
 99.000%    1.83ms \
 99.900%    4.22ms \
 99.990%    5.72ms \
 99.999%    7.11ms \
100.000%    7.27ms \
 
PROFILING: \
![PUT](https://github.com/s3ponia/2020-highload-dht/blob/master/test-results/profiler-delete.svg)

3. GET: \
WRK2: ~/wrk2/wrk -c1 -t1 -R2000 -d 60s --latency -s lua/wrk2-scripts/get-request.lua http://127.0.0.1:8080 > wrk2-get-latency \
Running 1m test @ http://127.0.0.1:8080 \
  1 threads and 1 connections \
  Thread calibration: mean lat.: 903.906ms, rate sampling interval: 3360ms \
  Thread Stats   Avg      Stdev     Max   +/- Stdev \
    Latency     6.35s     2.74s   11.60s    59.77% \
    Req/Sec     1.63k   157.59     1.94k    71.43% \
  Latency Distribution (HdrHistogram - Recorded Latency) \
 50.000%    6.15s \
 75.000%    8.45s \
 90.000%   10.25s \
 99.000%   11.49s \
 99.900%   11.59s \
 99.990%   11.60s \
 99.999%   11.60s \
100.000%   11.61s \

PROFILING: \
![PUT](https://github.com/s3ponia/2020-highload-dht/blob/master/test-results/profiler-get.svg)
