-t1 -c64 -d60s -R2000 -s ./wrk/put.lua --latency http://localhost:8081

Running 1m test @ http://localhost:8081
  1 threads and 64 connections
  Thread calibration: mean lat.: 1.421ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.40ms  661.70us   9.49ms   68.12%
    Req/Sec     2.10k   252.82     3.44k    69.54%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.32ms
 75.000%    1.84ms
 90.000%    2.24ms
 99.000%    2.78ms
 99.900%    6.08ms
 99.990%    7.77ms
 99.999%    8.57ms
100.000%    9.49ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.053     0.000000            1         1.00
       0.626     0.100000         9956         1.11
       0.820     0.200000        19912         1.25
       0.993     0.300000        29827         1.43
       1.156     0.400000        39762         1.67
       1.315     0.500000        49701         2.00
       1.400     0.550000        54663         2.22
       1.493     0.600000        59660         2.50
       1.595     0.650000        64593         2.86
       1.713     0.700000        69579         3.33
       1.842     0.750000        74527         4.00
       1.907     0.775000        77043         4.44
       1.973     0.800000        79489         5.00
       2.040     0.825000        82009         5.71
       2.105     0.850000        84466         6.67
       2.173     0.875000        86960         8.00
       2.209     0.887500        88196         8.89
       2.245     0.900000        89431        10.00
       2.285     0.912500        90686        11.43
       2.325     0.925000        91909        13.33
       2.371     0.937500        93176        16.00
       2.397     0.943750        93810        17.78
       2.423     0.950000        94408        20.00
       2.455     0.956250        95046        22.86
       2.487     0.962500        95647        26.67
       2.525     0.968750        96274        32.00
       2.545     0.971875        96580        35.56
       2.569     0.975000        96903        40.00
       2.597     0.978125        97197        45.71
       2.627     0.981250        97505        53.33
       2.665     0.984375        97806        64.00
       2.689     0.985938        97960        71.11
       2.719     0.987500        98122        80.00
       2.751     0.989062        98273        91.43
       2.809     0.990625        98428       106.67
       2.915     0.992188        98581       128.00
       3.099     0.992969        98661       142.22
       3.359     0.993750        98737       160.00
       3.699     0.994531        98815       182.86
       4.001     0.995313        98892       213.33
       4.283     0.996094        98971       256.00
       4.495     0.996484        99008       284.44
       4.663     0.996875        99048       320.00
       4.867     0.997266        99087       365.71
       5.095     0.997656        99126       426.67
       5.303     0.998047        99163       512.00
       5.439     0.998242        99184       568.89
       5.555     0.998437        99202       640.00
       5.727     0.998633        99222       731.43
       5.899     0.998828        99241       853.33
       6.119     0.999023        99260      1024.00
       6.251     0.999121        99270      1137.78
       6.367     0.999219        99280      1280.00
       6.543     0.999316        99290      1462.86
       6.707     0.999414        99299      1706.67
       6.851     0.999512        99309      2048.00
       6.931     0.999561        99314      2275.56
       7.019     0.999609        99319      2560.00
       7.143     0.999658        99324      2925.71
       7.207     0.999707        99328      3413.33
       7.343     0.999756        99333      4096.00
       7.479     0.999780        99336      4551.11
       7.499     0.999805        99338      5120.00
       7.619     0.999829        99341      5851.43
       7.643     0.999854        99343      6826.67
       7.751     0.999878        99346      8192.00
       7.767     0.999890        99347      9102.22
       7.799     0.999902        99348     10240.00
       7.831     0.999915        99349     11702.86
       7.843     0.999927        99350     13653.33
       7.867     0.999939        99351     16384.00
       7.999     0.999945        99352     18204.44
       8.083     0.999951        99353     20480.00
       8.083     0.999957        99353     23405.71
       8.231     0.999963        99354     27306.67
       8.231     0.999969        99354     32768.00
       8.423     0.999973        99355     36408.89
       8.423     0.999976        99355     40960.00
       8.423     0.999979        99355     46811.43
       8.575     0.999982        99356     54613.33
       8.575     0.999985        99356     65536.00
       8.575     0.999986        99356     72817.78
       8.575     0.999988        99356     81920.00
       8.575     0.999989        99356     93622.86
       9.495     0.999991        99357    109226.67
       9.495     1.000000        99357          inf
#[Mean    =        1.396, StdDeviation   =        0.662]
#[Max     =        9.488, Total count    =        99357]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  119714 requests in 1.00m, 9.48MB read
Requests/sec:   1995.19
Transfer/sec:    161.72KB

-t64 -c64 -d60s -R40000 -s ./wrk/put.lua --latency http://localhost:8080
Running 1m test @ http://localhost:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 0.645ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.646ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.646ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.646ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.642ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.645ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.646ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.648ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.648ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.644ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.645ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.644ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.639ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.645ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.644ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.645ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.645ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.643ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.645ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.646ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.645ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.646ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.649ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.646ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.649ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.642ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.649ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.645ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.646ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.645ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.643ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.644ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.648ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.646ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.644ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.646ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   652.15us  344.20us  12.52ms   68.10%
    Req/Sec   675.36     45.63     1.33k    77.86%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%  643.00us
 75.000%    0.90ms
 90.000%    1.05ms
 99.000%    1.21ms
 99.900%    3.36ms
 99.990%    7.35ms
 99.999%   10.50ms
100.000%   12.53ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.039     0.000000            1         1.00
       0.236     0.100000       200975         1.11
       0.339     0.200000       401307         1.25
       0.441     0.300000       600926         1.43
       0.542     0.400000       799817         1.67
       0.643     0.500000      1000161         2.00
       0.694     0.550000      1100379         2.22
       0.745     0.600000      1200718         2.50
       0.796     0.650000      1301065         2.86
       0.846     0.700000      1399794         3.33
       0.897     0.750000      1500210         4.00
       0.922     0.775000      1549734         4.44
       0.948     0.800000      1601144         5.00
       0.973     0.825000      1650515         5.71
       0.998     0.850000      1700005         6.67
       1.023     0.875000      1749361         8.00
       1.036     0.887500      1775030         8.89
       1.049     0.900000      1800840        10.00
       1.061     0.912500      1824732        11.43
       1.074     0.925000      1850280        13.33
       1.087     0.937500      1875463        16.00
       1.093     0.943750      1886802        17.78
       1.100     0.950000      1899698        20.00
       1.108     0.956250      1913024        22.86
       1.116     0.962500      1925060        26.67
       1.125     0.968750      1937863        32.00
       1.129     0.971875      1943076        35.56
       1.135     0.975000      1950085        40.00
       1.141     0.978125      1955698        45.71
       1.149     0.981250      1961610        53.33
       1.161     0.984375      1967907        64.00
       1.169     0.985938      1971060        71.11
       1.180     0.987500      1974184        80.00
       1.196     0.989062      1977327        91.43
       1.226     0.990625      1980362       106.67
       1.316     0.992188      1983465       128.00
       1.389     0.992969      1985030       142.22
       1.479     0.993750      1986590       160.00
       1.586     0.994531      1988149       182.86
       1.706     0.995313      1989718       213.33
       1.846     0.996094      1991269       256.00
       1.927     0.996484      1992049       284.44
       2.014     0.996875      1992828       320.00
       2.123     0.997266      1993607       365.71
       2.253     0.997656      1994393       426.67
       2.409     0.998047      1995168       512.00
       2.527     0.998242      1995563       568.89
       2.669     0.998437      1995953       640.00
       2.867     0.998633      1996339       731.43
       3.111     0.998828      1996730       853.33
       3.381     0.999023      1997120      1024.00
       3.563     0.999121      1997316      1137.78
       3.731     0.999219      1997512      1280.00
       3.941     0.999316      1997708      1462.86
       4.175     0.999414      1997902      1706.67
       4.507     0.999512      1998098      2048.00
       4.727     0.999561      1998195      2275.56
       4.915     0.999609      1998297      2560.00
       5.151     0.999658      1998390      2925.71
       5.415     0.999707      1998488      3413.33
       5.711     0.999756      1998584      4096.00
       5.887     0.999780      1998635      4551.11
       6.095     0.999805      1998682      5120.00
       6.375     0.999829      1998731      5851.43
       6.627     0.999854      1998781      6826.67
       6.979     0.999878      1998828      8192.00
       7.167     0.999890      1998853      9102.22
       7.363     0.999902      1998877     10240.00
       7.603     0.999915      1998902     11702.86
       7.827     0.999927      1998927     13653.33
       8.051     0.999939      1998950     16384.00
       8.175     0.999945      1998964     18204.44
       8.335     0.999951      1998976     20480.00
       8.479     0.999957      1998987     23405.71
       8.591     0.999963      1999000     27306.67
       8.839     0.999969      1999011     32768.00
       9.039     0.999973      1999019     36408.89
       9.167     0.999976      1999024     40960.00
       9.503     0.999979      1999030     46811.43
       9.615     0.999982      1999036     54613.33
       9.951     0.999985      1999043     65536.00
      10.039     0.999986      1999045     72817.78
      10.255     0.999988      1999048     81920.00
      10.431     0.999989      1999051     93622.86
      10.663     0.999991      1999054    109226.67
      10.839     0.999992      1999057    131072.00
      11.031     0.999993      1999059    145635.56
      11.135     0.999994      1999060    163840.00
      11.199     0.999995      1999062    187245.71
      11.231     0.999995      1999063    218453.33
      11.247     0.999996      1999065    262144.00
      11.471     0.999997      1999066    291271.11
      11.471     0.999997      1999066    327680.00
      11.543     0.999997      1999067    374491.43
      11.911     0.999998      1999068    436906.67
      11.959     0.999998      1999069    524288.00
      11.959     0.999998      1999069    582542.22
      11.959     0.999998      1999069    655360.00
      12.047     0.999999      1999070    748982.86
      12.047     0.999999      1999070    873813.33
      12.479     0.999999      1999071   1048576.00
      12.479     0.999999      1999071   1165084.44
      12.479     0.999999      1999071   1310720.00
      12.479     0.999999      1999071   1497965.71
      12.479     0.999999      1999071   1747626.67
      12.527     1.000000      1999072   2097152.00
      12.527     1.000000      1999072          inf
#[Mean    =        0.652, StdDeviation   =        0.344]
#[Max     =       12.520, Total count    =      1999072]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  2399325 requests in 1.00m, 189.92MB read
Requests/sec:  40036.03
Transfer/sec:      3.17MB


