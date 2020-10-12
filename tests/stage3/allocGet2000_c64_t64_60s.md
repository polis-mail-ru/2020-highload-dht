-t64 -c64 -d60s -R2000 -s ./wrk/get.lua --latency http://localhost:8080 get
Running 1m test @ http://localhost:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 0.913ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.900ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.919ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.900ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.876ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.886ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.874ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.879ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.896ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.840ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.860ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.853ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.837ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.848ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.870ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.839ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.859ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.863ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.837ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.863ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.840ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.853ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.851ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.845ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.844ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.858ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.868ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.855ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.867ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.874ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.055ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.012ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.989ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.986ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.999ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.985ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.007ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.985ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.979ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.002ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.030ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.014ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.996ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.965ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.989ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.998ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.979ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.982ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.976ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.968ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.980ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.945ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.962ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.939ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.975ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.939ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.940ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.910ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.922ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.918ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.920ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.920ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.917ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.908ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.07ms  366.46us  10.02ms   67.89%
    Req/Sec    32.82     48.41   111.00     68.44%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.07ms
 75.000%    1.32ms
 90.000%    1.53ms
 99.000%    1.77ms
 99.900%    2.87ms
 99.990%    6.24ms
 99.999%    9.21ms
100.000%   10.03ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.083     0.000000            1         1.00
       0.592     0.100000        10044         1.11
       0.757     0.200000        20094         1.25
       0.869     0.300000        30030         1.43
       0.971     0.400000        40059         1.67
       1.066     0.500000        50007         2.00
       1.113     0.550000        54983         2.22
       1.162     0.600000        59989         2.50
       1.213     0.650000        65045         2.86
       1.264     0.700000        70056         3.33
       1.320     0.750000        75022         4.00
       1.349     0.775000        77488         4.44
       1.381     0.800000        79982         5.00
       1.416     0.825000        82521         5.71
       1.452     0.850000        85023         6.67
       1.490     0.875000        87490         8.00
       1.510     0.887500        88770         8.89
       1.530     0.900000        89991        10.00
       1.552     0.912500        91227        11.43
       1.575     0.925000        92485        13.33
       1.599     0.937500        93741        16.00
       1.612     0.943750        94376        17.78
       1.625     0.950000        94972        20.00
       1.640     0.956250        95607        22.86
       1.656     0.962500        96237        26.67
       1.674     0.968750        96871        32.00
       1.683     0.971875        97154        35.56
       1.694     0.975000        97483        40.00
       1.705     0.978125        97793        45.71
       1.717     0.981250        98095        53.33
       1.733     0.984375        98410        64.00
       1.740     0.985938        98555        71.11
       1.751     0.987500        98720        80.00
       1.761     0.989062        98871        91.43
       1.773     0.990625        99032       106.67
       1.787     0.992188        99185       128.00
       1.793     0.992969        99258       142.22
       1.802     0.993750        99339       160.00
       1.814     0.994531        99415       182.86
       1.827     0.995313        99496       213.33
       1.837     0.996094        99568       256.00
       1.848     0.996484        99606       284.44
       1.858     0.996875        99645       320.00
       1.876     0.997266        99685       365.71
       1.895     0.997656        99724       426.67
       1.935     0.998047        99763       512.00
       1.969     0.998242        99782       568.89
       2.057     0.998437        99801       640.00
       2.215     0.998633        99821       731.43
       2.551     0.998828        99840       853.33
       2.941     0.999023        99861      1024.00
       3.035     0.999121        99870      1137.78
       3.279     0.999219        99879      1280.00
       3.433     0.999316        99889      1462.86
       3.807     0.999414        99899      1706.67
       3.939     0.999512        99909      2048.00
       4.061     0.999561        99914      2275.56
       4.419     0.999609        99918      2560.00
       4.583     0.999658        99923      2925.71
       4.787     0.999707        99928      3413.33
       4.943     0.999756        99933      4096.00
       5.159     0.999780        99936      4551.11
       5.227     0.999805        99938      5120.00
       5.239     0.999829        99940      5851.43
       5.543     0.999854        99943      6826.67
       5.851     0.999878        99945      8192.00
       6.235     0.999890        99947      9102.22
       6.431     0.999902        99948     10240.00
       6.567     0.999915        99949     11702.86
       6.639     0.999927        99950     13653.33
       6.695     0.999939        99951     16384.00
       7.399     0.999945        99952     18204.44
       8.319     0.999951        99953     20480.00
       8.319     0.999957        99953     23405.71
       8.383     0.999963        99954     27306.67
       8.383     0.999969        99954     32768.00
       8.919     0.999973        99955     36408.89
       8.919     0.999976        99955     40960.00
       8.919     0.999979        99955     46811.43
       9.207     0.999982        99956     54613.33
       9.207     0.999985        99956     65536.00
       9.207     0.999986        99956     72817.78
       9.207     0.999988        99956     81920.00
       9.207     0.999989        99956     93622.86
      10.031     0.999991        99957    109226.67
      10.031     1.000000        99957          inf
#[Mean    =        1.066, StdDeviation   =        0.366]
#[Max     =       10.024, Total count    =        99957]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  120053 requests in 1.00m, 8.06MB read
Requests/sec:   2002.07
Transfer/sec:    137.66KB


