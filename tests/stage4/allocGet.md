-t1 -c64 -d60s -R2000 -s ./wrk/get.lua --latency http://localhost:8081
Running 1m test @ http://localhost:8081
  1 threads and 64 connections
  Thread calibration: mean lat.: 1.464ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.44ms  706.00us  12.92ms   68.59%
    Req/Sec     2.11k   212.65     3.44k    72.73%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.37ms
 75.000%    1.85ms
 90.000%    2.42ms
 99.000%    3.02ms
 99.900%    5.72ms
 99.990%    7.89ms
 99.999%   12.68ms
100.000%   12.93ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.067     0.000000            1         1.00
       0.608     0.100000         9940         1.11
       0.822     0.200000        19908         1.25
       1.001     0.300000        29845         1.43
       1.188     0.400000        39783         1.67
       1.371     0.500000        49680         2.00
       1.460     0.550000        54654         2.22
       1.550     0.600000        59648         2.50
       1.644     0.650000        64623         2.86
       1.743     0.700000        69586         3.33
       1.852     0.750000        74547         4.00
       1.918     0.775000        77024         4.44
       1.997     0.800000        79494         5.00
       2.091     0.825000        81971         5.71
       2.199     0.850000        84465         6.67
       2.305     0.875000        86960         8.00
       2.361     0.887500        88192         8.89
       2.419     0.900000        89457        10.00
       2.477     0.912500        90685        11.43
       2.535     0.925000        91911        13.33
       2.597     0.937500        93157        16.00
       2.629     0.943750        93786        17.78
       2.663     0.950000        94417        20.00
       2.695     0.956250        95016        22.86
       2.735     0.962500        95651        26.67
       2.779     0.968750        96257        32.00
       2.803     0.971875        96583        35.56
       2.825     0.975000        96884        40.00
       2.851     0.978125        97199        45.71
       2.881     0.981250        97508        53.33
       2.919     0.984375        97809        64.00
       2.939     0.985938        97964        71.11
       2.965     0.987500        98133        80.00
       2.997     0.989062        98273        91.43
       3.041     0.990625        98428       106.67
       3.103     0.992188        98586       128.00
       3.153     0.992969        98659       142.22
       3.249     0.993750        98737       160.00
       3.491     0.994531        98814       182.86
       3.753     0.995313        98892       213.33
       4.023     0.996094        98969       256.00
       4.239     0.996484        99009       284.44
       4.411     0.996875        99048       320.00
       4.591     0.997266        99087       365.71
       4.763     0.997656        99125       426.67
       4.995     0.998047        99163       512.00
       5.131     0.998242        99183       568.89
       5.275     0.998437        99202       640.00
       5.383     0.998633        99224       731.43
       5.571     0.998828        99241       853.33
       5.735     0.999023        99260      1024.00
       5.879     0.999121        99270      1137.78
       6.003     0.999219        99280      1280.00
       6.127     0.999316        99290      1462.86
       6.299     0.999414        99299      1706.67
       6.439     0.999512        99309      2048.00
       6.519     0.999561        99314      2275.56
       6.635     0.999609        99321      2560.00
       6.831     0.999658        99324      2925.71
       6.867     0.999707        99329      3413.33
       7.031     0.999756        99333      4096.00
       7.067     0.999780        99336      4551.11
       7.195     0.999805        99339      5120.00
       7.251     0.999829        99341      5851.43
       7.603     0.999854        99343      6826.67
       7.739     0.999878        99345      8192.00
       7.891     0.999890        99347      9102.22
       7.991     0.999902        99348     10240.00
       7.999     0.999915        99349     11702.86
       8.063     0.999927        99350     13653.33
       8.343     0.999939        99351     16384.00
       8.391     0.999945        99352     18204.44
       8.895     0.999951        99353     20480.00
       8.895     0.999957        99353     23405.71
      11.311     0.999963        99354     27306.67
      11.311     0.999969        99354     32768.00
      12.215     0.999973        99355     36408.89
      12.215     0.999976        99355     40960.00
      12.215     0.999979        99355     46811.43
      12.679     0.999982        99356     54613.33
      12.679     0.999985        99356     65536.00
      12.679     0.999986        99356     72817.78
      12.679     0.999988        99356     81920.00
      12.679     0.999989        99356     93622.86
      12.927     0.999991        99357    109226.67
      12.927     1.000000        99357          inf
#[Mean    =        1.441, StdDeviation   =        0.706]
#[Max     =       12.920, Total count    =        99357]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  119714 requests in 1.00m, 10.16MB read
Requests/sec:   1995.22
Transfer/sec:    173.39KB

-t64 -c64 -d60s -R40000 -s ./wrk/get.lua --latency http://localhost:8080
Running 1m test @ http://localhost:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.666ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.659ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.660ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.659ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.660ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.660ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.660ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.662ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.661ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.659ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.660ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.659ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.662ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   660.67us  342.45us  12.63ms   67.58%
    Req/Sec   671.54     46.89     1.33k    75.07%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%  652.00us
 75.000%    0.91ms
 90.000%    1.06ms
 99.000%    1.24ms
 99.900%    3.43ms
 99.990%    6.52ms
 99.999%    9.60ms
100.000%   12.64ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.039     0.000000            1         1.00
       0.243     0.100000       201213         1.11
       0.346     0.200000       400172         1.25
       0.449     0.300000       600236         1.43
       0.551     0.400000       800418         1.67
       0.652     0.500000      1000899         2.00
       0.703     0.550000      1101327         2.22
       0.753     0.600000      1199943         2.50
       0.804     0.650000      1300111         2.86
       0.855     0.700000      1400276         3.33
       0.906     0.750000      1500928         4.00
       0.931     0.775000      1550388         4.44
       0.956     0.800000      1599283         5.00
       0.982     0.825000      1650864         5.71
       1.007     0.850000      1700732         6.67
       1.032     0.875000      1750437         8.00
       1.044     0.887500      1774195         8.89
       1.057     0.900000      1799568        10.00
       1.070     0.912500      1825343        11.43
       1.083     0.925000      1850478        13.33
       1.096     0.937500      1875968        16.00
       1.102     0.943750      1887302        17.78
       1.109     0.950000      1899422        20.00
       1.117     0.956250      1912258        22.86
       1.126     0.962500      1925341        26.67
       1.135     0.968750      1937165        32.00
       1.140     0.971875      1943013        35.56
       1.147     0.975000      1949953        40.00
       1.154     0.978125      1955671        45.71
       1.163     0.981250      1961584        53.33
       1.177     0.984375      1968015        64.00
       1.186     0.985938      1970965        71.11
       1.199     0.987500      1974220        80.00
       1.219     0.989062      1977281        91.43
       1.262     0.990625      1980325       106.67
       1.367     0.992188      1983454       128.00
       1.440     0.992969      1984996       142.22
       1.530     0.993750      1986564       160.00
       1.636     0.994531      1988127       182.86
       1.759     0.995313      1989681       213.33
       1.903     0.996094      1991243       256.00
       1.994     0.996484      1992027       284.44
       2.091     0.996875      1992814       320.00
       2.199     0.997266      1993587       365.71
       2.341     0.997656      1994368       426.67
       2.537     0.998047      1995149       512.00
       2.661     0.998242      1995540       568.89
       2.821     0.998437      1995931       640.00
       2.993     0.998633      1996317       731.43
       3.213     0.998828      1996710       853.33
       3.465     0.999023      1997098      1024.00
       3.619     0.999121      1997295      1137.78
       3.773     0.999219      1997490      1280.00
       3.935     0.999316      1997685      1462.86
       4.115     0.999414      1997882      1706.67
       4.335     0.999512      1998074      2048.00
       4.467     0.999561      1998175      2275.56
       4.583     0.999609      1998272      2560.00
       4.731     0.999658      1998368      2925.71
       4.935     0.999707      1998465      3413.33
       5.151     0.999756      1998563      4096.00
       5.287     0.999780      1998611      4551.11
       5.463     0.999805      1998660      5120.00
       5.639     0.999829      1998710      5851.43
       5.883     0.999854      1998759      6826.67
       6.139     0.999878      1998806      8192.00
       6.355     0.999890      1998831      9102.22
       6.539     0.999902      1998855     10240.00
       6.731     0.999915      1998880     11702.86
       6.943     0.999927      1998904     13653.33
       7.359     0.999939      1998928     16384.00
       7.551     0.999945      1998941     18204.44
       7.659     0.999951      1998953     20480.00
       7.855     0.999957      1998965     23405.71
       8.167     0.999963      1998977     27306.67
       8.343     0.999969      1998991     32768.00
       8.447     0.999973      1998996     36408.89
       8.535     0.999976      1999002     40960.00
       8.735     0.999979      1999008     46811.43
       8.935     0.999982      1999014     54613.33
       9.063     0.999985      1999020     65536.00
       9.135     0.999986      1999023     72817.78
       9.255     0.999988      1999026     81920.00
       9.575     0.999989      1999029     93622.86
       9.671     0.999991      1999032    109226.67
      10.039     0.999992      1999035    131072.00
      10.327     0.999993      1999037    145635.56
      10.447     0.999994      1999038    163840.00
      10.583     0.999995      1999040    187245.71
      10.647     0.999995      1999041    218453.33
      10.991     0.999996      1999043    262144.00
      11.199     0.999997      1999044    291271.11
      11.199     0.999997      1999044    327680.00
      11.399     0.999997      1999045    374491.43
      11.559     0.999998      1999046    436906.67
      11.951     0.999998      1999047    524288.00
      11.951     0.999998      1999047    582542.22
      11.951     0.999998      1999047    655360.00
      11.959     0.999999      1999048    748982.86
      11.959     0.999999      1999048    873813.33
      12.271     0.999999      1999049   1048576.00
      12.271     0.999999      1999049   1165084.44
      12.271     0.999999      1999049   1310720.00
      12.271     0.999999      1999049   1497965.71
      12.271     0.999999      1999049   1747626.67
      12.639     1.000000      1999050   2097152.00
      12.639     1.000000      1999050          inf
#[Mean    =        0.661, StdDeviation   =        0.342]
#[Max     =       12.632, Total count    =      1999050]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  2399298 requests in 1.00m, 202.36MB read
Requests/sec:  40038.45
Transfer/sec:      3.38MB

