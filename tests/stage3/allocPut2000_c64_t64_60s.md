-t64 -c64 -d60s -R2000 -s ./wrk/put.lua --latency http://localhost:8080 put
Running 1m test @ http://localhost:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 1.001ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.941ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.982ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.966ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.957ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.969ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.925ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.976ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.966ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.928ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.946ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.947ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.960ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.949ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.985ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.961ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.964ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.927ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.017ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.972ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.956ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.939ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.958ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.934ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.957ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.928ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.947ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.890ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.885ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.903ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.934ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.985ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.940ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.918ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.908ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.910ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.908ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.905ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.945ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.890ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.910ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.906ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.953ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.940ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.916ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.013ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.998ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.985ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.961ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.012ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.956ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.948ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.929ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.962ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.919ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.935ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.931ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.942ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.962ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.947ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.940ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.954ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.962ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.957ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.07ms  443.97us  14.89ms   77.08%
    Req/Sec    32.79     48.41   111.00     68.48%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.06ms
 75.000%    1.32ms
 90.000%    1.54ms
 99.000%    1.77ms
 99.900%    5.60ms
 99.990%   10.45ms
 99.999%   13.88ms
100.000%   14.90ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.063     0.000000            1         1.00
       0.589     0.100000        10016         1.11
       0.750     0.200000        20008         1.25
       0.862     0.300000        30004         1.43
       0.964     0.400000        39997         1.67
       1.063     0.500000        49983         2.00
       1.112     0.550000        54997         2.22
       1.162     0.600000        60012         2.50
       1.212     0.650000        64958         2.86
       1.266     0.700000        70017         3.33
       1.323     0.750000        74958         4.00
       1.354     0.775000        77456         4.44
       1.387     0.800000        80010         5.00
       1.422     0.825000        82497         5.71
       1.459     0.850000        84977         6.67
       1.496     0.875000        87457         8.00
       1.516     0.887500        88730         8.89
       1.536     0.900000        89994        10.00
       1.556     0.912500        91221        11.43
       1.577     0.925000        92473        13.33
       1.599     0.937500        93694        16.00
       1.610     0.943750        94317        17.78
       1.622     0.950000        94938        20.00
       1.636     0.956250        95579        22.86
       1.652     0.962500        96205        26.67
       1.668     0.968750        96829        32.00
       1.677     0.971875        97143        35.56
       1.688     0.975000        97453        40.00
       1.699     0.978125        97751        45.71
       1.712     0.981250        98065        53.33
       1.729     0.984375        98381        64.00
       1.738     0.985938        98541        71.11
       1.749     0.987500        98689        80.00
       1.763     0.989062        98848        91.43
       1.781     0.990625        98997       106.67
       1.805     0.992188        99157       128.00
       1.825     0.992969        99230       142.22
       1.856     0.993750        99308       160.00
       1.926     0.994531        99386       182.86
       2.123     0.995313        99464       213.33
       2.503     0.996094        99542       256.00
       2.771     0.996484        99581       284.44
       3.059     0.996875        99620       320.00
       3.373     0.997266        99659       365.71
       3.793     0.997656        99698       426.67
       4.199     0.998047        99737       512.00
       4.475     0.998242        99757       568.89
       4.683     0.998437        99777       640.00
       4.991     0.998633        99796       731.43
       5.299     0.998828        99815       853.33
       5.631     0.999023        99835      1024.00
       5.879     0.999121        99845      1137.78
       6.303     0.999219        99854      1280.00
       6.651     0.999316        99864      1462.86
       6.935     0.999414        99874      1706.67
       7.223     0.999512        99884      2048.00
       7.483     0.999561        99889      2275.56
       7.727     0.999609        99893      2560.00
       8.023     0.999658        99898      2925.71
       8.391     0.999707        99903      3413.33
       8.959     0.999756        99908      4096.00
       9.191     0.999780        99911      4551.11
       9.279     0.999805        99913      5120.00
       9.607     0.999829        99915      5851.43
       9.703     0.999854        99918      6826.67
       9.807     0.999878        99920      8192.00
      10.455     0.999890        99922      9102.22
      10.927     0.999902        99923     10240.00
      11.591     0.999915        99924     11702.86
      11.791     0.999927        99925     13653.33
      11.943     0.999939        99926     16384.00
      11.951     0.999945        99927     18204.44
      12.127     0.999951        99928     20480.00
      12.127     0.999957        99928     23405.71
      13.047     0.999963        99929     27306.67
      13.047     0.999969        99929     32768.00
      13.327     0.999973        99930     36408.89
      13.327     0.999976        99930     40960.00
      13.327     0.999979        99930     46811.43
      13.879     0.999982        99931     54613.33
      13.879     0.999985        99931     65536.00
      13.879     0.999986        99931     72817.78
      13.879     0.999988        99931     81920.00
      13.879     0.999989        99931     93622.86
      14.895     0.999991        99932    109226.67
      14.895     1.000000        99932          inf
#[Mean    =        1.072, StdDeviation   =        0.444]
#[Max     =       14.888, Total count    =        99932]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  120028 requests in 1.00m, 7.67MB read
Requests/sec:   2001.99
Transfer/sec:    130.99KB
