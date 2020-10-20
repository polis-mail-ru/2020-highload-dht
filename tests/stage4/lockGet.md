-t1 -c64 -d60s -R2000 -s ./wrk/get.lua --latency http://localhost:8081
Running 1m test @ http://localhost:8081
  1 threads and 64 connections
  Thread calibration: mean lat.: 1.330ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.33ms  661.62us  10.17ms   67.56%
    Req/Sec     2.10k   269.42     3.40k    65.57%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.27ms
 75.000%    1.72ms
 90.000%    2.26ms
 99.000%    2.78ms
 99.900%    5.44ms
 99.990%    8.02ms
 99.999%   10.11ms
100.000%   10.18ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.051     0.000000            1         1.00
       0.551     0.100000         9971         1.11
       0.740     0.200000        19924         1.25
       0.911     0.300000        29839         1.43
       1.094     0.400000        39756         1.67
       1.267     0.500000        49719         2.00
       1.347     0.550000        54671         2.22
       1.427     0.600000        59628         2.50
       1.512     0.650000        64596         2.86
       1.606     0.700000        69594         3.33
       1.717     0.750000        74527         4.00
       1.784     0.775000        76999         4.44
       1.860     0.800000        79497         5.00
       1.956     0.825000        81981         5.71
       2.057     0.850000        84476         6.67
       2.159     0.875000        86953         8.00
       2.209     0.887500        88184         8.89
       2.263     0.900000        89434        10.00
       2.315     0.912500        90674        11.43
       2.369     0.925000        91903        13.33
       2.423     0.937500        93172        16.00
       2.449     0.943750        93772        17.78
       2.477     0.950000        94398        20.00
       2.509     0.956250        95047        22.86
       2.537     0.962500        95643        26.67
       2.577     0.968750        96264        32.00
       2.597     0.971875        96572        35.56
       2.617     0.975000        96884        40.00
       2.641     0.978125        97208        45.71
       2.669     0.981250        97509        53.33
       2.703     0.984375        97826        64.00
       2.721     0.985938        97960        71.11
       2.743     0.987500        98117        80.00
       2.767     0.989062        98267        91.43
       2.799     0.990625        98427       106.67
       2.845     0.992188        98577       128.00
       2.879     0.992969        98655       142.22
       2.947     0.993750        98735       160.00
       3.073     0.994531        98810       182.86
       3.311     0.995313        98888       213.33
       3.603     0.996094        98965       256.00
       3.761     0.996484        99005       284.44
       3.941     0.996875        99043       320.00
       4.143     0.997266        99082       365.71
       4.343     0.997656        99121       426.67
       4.603     0.998047        99159       512.00
       4.715     0.998242        99179       568.89
       4.823     0.998437        99198       640.00
       4.959     0.998633        99218       731.43
       5.251     0.998828        99237       853.33
       5.451     0.999023        99256      1024.00
       5.563     0.999121        99266      1137.78
       5.679     0.999219        99276      1280.00
       5.835     0.999316        99286      1462.86
       5.967     0.999414        99295      1706.67
       6.163     0.999512        99305      2048.00
       6.339     0.999561        99311      2275.56
       6.447     0.999609        99315      2560.00
       6.595     0.999658        99320      2925.71
       6.779     0.999707        99324      3413.33
       6.851     0.999756        99329      4096.00
       6.887     0.999780        99332      4551.11
       7.087     0.999805        99334      5120.00
       7.175     0.999829        99337      5851.43
       7.195     0.999854        99339      6826.67
       7.611     0.999878        99341      8192.00
       8.015     0.999890        99343      9102.22
       8.039     0.999902        99344     10240.00
       8.095     0.999915        99345     11702.86
       8.303     0.999927        99346     13653.33
       8.431     0.999939        99347     16384.00
       8.615     0.999945        99348     18204.44
       8.671     0.999951        99349     20480.00
       8.671     0.999957        99349     23405.71
       8.959     0.999963        99350     27306.67
       8.959     0.999969        99350     32768.00
       9.415     0.999973        99351     36408.89
       9.415     0.999976        99351     40960.00
       9.415     0.999979        99351     46811.43
      10.111     0.999982        99352     54613.33
      10.111     0.999985        99352     65536.00
      10.111     0.999986        99352     72817.78
      10.111     0.999988        99352     81920.00
      10.111     0.999989        99352     93622.86
      10.175     0.999991        99353    109226.67
      10.175     1.000000        99353          inf
#[Mean    =        1.330, StdDeviation   =        0.662]
#[Max     =       10.168, Total count    =        99353]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  119708 requests in 1.00m, 10.16MB read
Requests/sec:   1995.14
Transfer/sec:    173.38KB

-t64 -c64 -d60s -R40000 -s ./wrk/get.lua --latency http://localhost:8080
Running 1m test @ http://localhost:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 0.970ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.988ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.940ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.208ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.980ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.972ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.882ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.921ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.953ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.938ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.178ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.853ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.298ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.895ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.859ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.875ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.824ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.401ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.005ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.828ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.141ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.829ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.824ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.941ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.822ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.866ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.819ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.209ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.859ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.456ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.878ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.780ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.939ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.921ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.828ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.884ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.885ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.726ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.728ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.857ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.781ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.806ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.854ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.857ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.751ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.775ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.807ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.782ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.758ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.791ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.751ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.735ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.757ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.743ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.748ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.771ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.818ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.774ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.750ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.013ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.029ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.728ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.005ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.750ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   655.20us  329.65us  10.66ms   65.21%
    Req/Sec   673.46     45.94     1.22k    76.44%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%  649.00us
 75.000%    0.90ms
 90.000%    1.05ms
 99.000%    1.21ms
 99.900%    2.91ms
 99.990%    6.11ms
 99.999%    7.98ms
100.000%   10.67ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.041     0.000000            1         1.00
       0.241     0.100000       201206         1.11
       0.344     0.200000       401190         1.25
       0.445     0.300000       599505         1.43
       0.547     0.400000       799872         1.67
       0.649     0.500000      1000721         2.00
       0.699     0.550000      1099227         2.22
       0.750     0.600000      1199838         2.50
       0.801     0.650000      1299707         2.86
       0.852     0.700000      1400512         3.33
       0.902     0.750000      1500002         4.00
       0.927     0.775000      1550082         4.44
       0.952     0.800000      1599748         5.00
       0.977     0.825000      1649291         5.71
       1.002     0.850000      1698509         6.67
       1.028     0.875000      1749917         8.00
       1.041     0.887500      1775284         8.89
       1.053     0.900000      1799007        10.00
       1.066     0.912500      1824732        11.43
       1.079     0.925000      1850235        13.33
       1.091     0.937500      1873512        16.00
       1.098     0.943750      1886834        17.78
       1.105     0.950000      1899480        20.00
       1.112     0.956250      1910967        22.86
       1.120     0.962500      1923348        26.67
       1.129     0.968750      1936050        32.00
       1.134     0.971875      1942381        35.56
       1.140     0.975000      1949030        40.00
       1.146     0.978125      1954528        45.71
       1.155     0.981250      1961268        53.33
       1.166     0.984375      1967254        64.00
       1.173     0.985938      1970102        71.11
       1.183     0.987500      1973288        80.00
       1.197     0.989062      1976488        91.43
       1.219     0.990625      1979467       106.67
       1.276     0.992188      1982600       128.00
       1.331     0.992969      1984145       142.22
       1.402     0.993750      1985699       160.00
       1.493     0.994531      1987264       182.86
       1.597     0.995313      1988825       213.33
       1.716     0.996094      1990387       256.00
       1.785     0.996484      1991173       284.44
       1.865     0.996875      1991940       320.00
       1.959     0.997266      1992725       365.71
       2.065     0.997656      1993515       426.67
       2.201     0.998047      1994290       512.00
       2.285     0.998242      1994677       568.89
       2.389     0.998437      1995062       640.00
       2.535     0.998633      1995456       731.43
       2.711     0.998828      1995845       853.33
       2.947     0.999023      1996234      1024.00
       3.071     0.999121      1996428      1137.78
       3.233     0.999219      1996623      1280.00
       3.435     0.999316      1996819      1462.86
       3.661     0.999414      1997015      1706.67
       3.911     0.999512      1997209      2048.00
       4.035     0.999561      1997306      2275.56
       4.175     0.999609      1997405      2560.00
       4.387     0.999658      1997502      2925.71
       4.587     0.999707      1997601      3413.33
       4.855     0.999756      1997698      4096.00
       5.007     0.999780      1997745      4551.11
       5.175     0.999805      1997794      5120.00
       5.367     0.999829      1997843      5851.43
       5.623     0.999854      1997893      6826.67
       5.927     0.999878      1997941      8192.00
       6.047     0.999890      1997965      9102.22
       6.151     0.999902      1997990     10240.00
       6.271     0.999915      1998015     11702.86
       6.491     0.999927      1998038     13653.33
       6.643     0.999939      1998064     16384.00
       6.687     0.999945      1998076     18204.44
       6.799     0.999951      1998087     20480.00
       6.911     0.999957      1998100     23405.71
       7.027     0.999963      1998112     27306.67
       7.239     0.999969      1998125     32768.00
       7.323     0.999973      1998130     36408.89
       7.435     0.999976      1998136     40960.00
       7.503     0.999979      1998142     46811.43
       7.563     0.999982      1998148     54613.33
       7.751     0.999985      1998154     65536.00
       7.859     0.999986      1998157     72817.78
       7.883     0.999988      1998160     81920.00
       7.915     0.999989      1998163     93622.86
       8.019     0.999991      1998166    109226.67
       8.223     0.999992      1998169    131072.00
       8.343     0.999993      1998171    145635.56
       8.391     0.999994      1998172    163840.00
       8.447     0.999995      1998175    187245.71
       8.447     0.999995      1998175    218453.33
       8.719     0.999996      1998177    262144.00
       8.999     0.999997      1998178    291271.11
       8.999     0.999997      1998178    327680.00
       9.103     0.999997      1998179    374491.43
       9.175     0.999998      1998180    436906.67
       9.383     0.999998      1998181    524288.00
       9.383     0.999998      1998181    582542.22
       9.383     0.999998      1998181    655360.00
       9.719     0.999999      1998182    748982.86
       9.719     0.999999      1998182    873813.33
       9.823     0.999999      1998183   1048576.00
       9.823     0.999999      1998183   1165084.44
       9.823     0.999999      1998183   1310720.00
       9.823     0.999999      1998183   1497965.71
       9.823     0.999999      1998183   1747626.67
      10.671     1.000000      1998184   2097152.00
      10.671     1.000000      1998184          inf
#[Mean    =        0.655, StdDeviation   =        0.330]
#[Max     =       10.664, Total count    =      1998184]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  2398436 requests in 1.00m, 202.28MB read
Requests/sec:  40020.46
Transfer/sec:      3.38MB


