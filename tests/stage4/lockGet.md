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

Практически нет изменений в процентном соотношении селекторов и воркеров: 49.6% занимают селекоры в этой реализации, 50.57% в прошлой.

-t64 -c64 -d60s -R80000 -s ./wrk/get.lua --latency http://localhost:8080
Running 1m test @ http://localhost:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 17.283ms, rate sampling interval: 161ms
  Thread calibration: mean lat.: 15.701ms, rate sampling interval: 152ms
  Thread calibration: mean lat.: 18.963ms, rate sampling interval: 179ms
  Thread calibration: mean lat.: 18.038ms, rate sampling interval: 173ms
  Thread calibration: mean lat.: 18.945ms, rate sampling interval: 180ms
  Thread calibration: mean lat.: 15.763ms, rate sampling interval: 142ms
  Thread calibration: mean lat.: 18.020ms, rate sampling interval: 172ms
  Thread calibration: mean lat.: 19.538ms, rate sampling interval: 185ms
  Thread calibration: mean lat.: 13.943ms, rate sampling interval: 130ms
  Thread calibration: mean lat.: 16.474ms, rate sampling interval: 157ms
  Thread calibration: mean lat.: 14.634ms, rate sampling interval: 139ms
  Thread calibration: mean lat.: 14.879ms, rate sampling interval: 143ms
  Thread calibration: mean lat.: 18.474ms, rate sampling interval: 177ms
  Thread calibration: mean lat.: 18.582ms, rate sampling interval: 182ms
  Thread calibration: mean lat.: 13.791ms, rate sampling interval: 131ms
  Thread calibration: mean lat.: 18.601ms, rate sampling interval: 172ms
  Thread calibration: mean lat.: 15.815ms, rate sampling interval: 153ms
  Thread calibration: mean lat.: 15.228ms, rate sampling interval: 147ms
  Thread calibration: mean lat.: 17.109ms, rate sampling interval: 163ms
  Thread calibration: mean lat.: 12.080ms, rate sampling interval: 112ms
  Thread calibration: mean lat.: 17.919ms, rate sampling interval: 170ms
  Thread calibration: mean lat.: 15.357ms, rate sampling interval: 146ms
  Thread calibration: mean lat.: 19.778ms, rate sampling interval: 184ms
  Thread calibration: mean lat.: 15.795ms, rate sampling interval: 155ms
  Thread calibration: mean lat.: 14.248ms, rate sampling interval: 133ms
  Thread calibration: mean lat.: 15.587ms, rate sampling interval: 145ms
  Thread calibration: mean lat.: 17.500ms, rate sampling interval: 171ms
  Thread calibration: mean lat.: 18.219ms, rate sampling interval: 173ms
  Thread calibration: mean lat.: 16.591ms, rate sampling interval: 163ms
  Thread calibration: mean lat.: 18.436ms, rate sampling interval: 177ms
  Thread calibration: mean lat.: 17.408ms, rate sampling interval: 165ms
  Thread calibration: mean lat.: 15.762ms, rate sampling interval: 151ms
  Thread calibration: mean lat.: 18.060ms, rate sampling interval: 175ms
  Thread calibration: mean lat.: 12.981ms, rate sampling interval: 122ms
  Thread calibration: mean lat.: 14.765ms, rate sampling interval: 134ms
  Thread calibration: mean lat.: 17.528ms, rate sampling interval: 163ms
  Thread calibration: mean lat.: 17.505ms, rate sampling interval: 172ms
  Thread calibration: mean lat.: 18.472ms, rate sampling interval: 168ms
  Thread calibration: mean lat.: 19.612ms, rate sampling interval: 181ms
  Thread calibration: mean lat.: 15.071ms, rate sampling interval: 147ms
  Thread calibration: mean lat.: 14.104ms, rate sampling interval: 129ms
  Thread calibration: mean lat.: 18.941ms, rate sampling interval: 182ms
  Thread calibration: mean lat.: 18.583ms, rate sampling interval: 175ms
  Thread calibration: mean lat.: 20.160ms, rate sampling interval: 188ms
  Thread calibration: mean lat.: 15.954ms, rate sampling interval: 147ms
  Thread calibration: mean lat.: 22.950ms, rate sampling interval: 208ms
  Thread calibration: mean lat.: 17.445ms, rate sampling interval: 166ms
  Thread calibration: mean lat.: 19.607ms, rate sampling interval: 184ms
  Thread calibration: mean lat.: 18.680ms, rate sampling interval: 175ms
  Thread calibration: mean lat.: 18.141ms, rate sampling interval: 174ms
  Thread calibration: mean lat.: 19.057ms, rate sampling interval: 178ms
  Thread calibration: mean lat.: 15.309ms, rate sampling interval: 143ms
  Thread calibration: mean lat.: 19.381ms, rate sampling interval: 180ms
  Thread calibration: mean lat.: 16.756ms, rate sampling interval: 154ms
  Thread calibration: mean lat.: 14.635ms, rate sampling interval: 138ms
  Thread calibration: mean lat.: 18.343ms, rate sampling interval: 171ms
  Thread calibration: mean lat.: 14.089ms, rate sampling interval: 131ms
  Thread calibration: mean lat.: 16.936ms, rate sampling interval: 160ms
  Thread calibration: mean lat.: 16.317ms, rate sampling interval: 151ms
  Thread calibration: mean lat.: 14.596ms, rate sampling interval: 133ms
  Thread calibration: mean lat.: 16.636ms, rate sampling interval: 153ms
  Thread calibration: mean lat.: 17.827ms, rate sampling interval: 169ms
  Thread calibration: mean lat.: 16.384ms, rate sampling interval: 147ms
  Thread calibration: mean lat.: 18.086ms, rate sampling interval: 171ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.03s     1.16s    3.47s    76.81%
    Req/Sec     1.17k    81.35     1.45k    64.65%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%  440.83ms
 75.000%    2.11s 
 90.000%    2.87s 
 99.000%    3.29s 
 99.900%    3.42s 
 99.990%    3.45s 
 99.999%    3.46s 
100.000%    3.47s 

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.037     0.000000            1         1.00
       0.838     0.100000       373635         1.11
       1.177     0.200000       746445         1.25
       1.632     0.300000      1120042         1.43
       5.771     0.400000      1492770         1.67
     440.831     0.500000      1865998         2.00
     807.423     0.550000      2052516         2.22
    1179.647     0.600000      2239216         2.50
    1594.367     0.650000      2425892         2.86
    1862.655     0.700000      2612436         3.33
    2109.439     0.750000      2799238         4.00
    2230.271     0.775000      2892120         4.44
    2357.247     0.800000      2986189         5.00
    2502.655     0.825000      3079881         5.71
    2631.679     0.850000      3173447         6.67
    2748.415     0.875000      3266386         8.00
    2807.807     0.887500      3313031         8.89
    2867.199     0.900000      3359086        10.00
    2924.543     0.912500      3405785        11.43
    2981.887     0.925000      3453020        13.33
    3037.183     0.937500      3500123        16.00
    3065.855     0.943750      3523022        17.78
    3094.527     0.950000      3546466        20.00
    3123.199     0.956250      3570269        22.86
    3149.823     0.962500      3591917        26.67
    3180.543     0.968750      3616300        32.00
    3194.879     0.971875      3626953        35.56
    3211.263     0.975000      3639004        40.00
    3227.647     0.978125      3651005        45.71
    3244.031     0.981250      3662980        53.33
    3258.367     0.984375      3673589        64.00
    3268.607     0.985938      3680421        71.11
    3276.799     0.987500      3685797        80.00
    3287.039     0.989062      3691640        91.43
    3297.279     0.990625      3697085       106.67
    3311.615     0.992188      3703189       128.00
    3319.807     0.992969      3706083       142.22
    3327.999     0.993750      3708579       160.00
    3340.287     0.994531      3711693       182.86
    3352.575     0.995313      3714580       213.33
    3364.863     0.996094      3717444       256.00
    3371.007     0.996484      3718852       284.44
    3377.151     0.996875      3720390       320.00
    3383.295     0.997266      3721774       365.71
    3389.439     0.997656      3723293       426.67
    3395.583     0.998047      3724537       512.00
    3399.679     0.998242      3725370       568.89
    3403.775     0.998437      3726197       640.00
    3407.871     0.998633      3726923       731.43
    3411.967     0.998828      3727607       853.33
    3416.063     0.999023      3728334      1024.00
    3418.111     0.999121      3728659      1137.78
    3420.159     0.999219      3728999      1280.00
    3422.207     0.999316      3729253      1462.86
    3426.303     0.999414      3729724      1706.67
    3428.351     0.999512      3729969      2048.00
    3430.399     0.999561      3730193      2275.56
    3432.447     0.999609      3730382      2560.00
    3434.495     0.999658      3730502      2925.71
    3438.591     0.999707      3730814      3413.33
    3440.639     0.999756      3730970      4096.00
    3440.639     0.999780      3730970      4551.11
    3442.687     0.999805      3731118      5120.00
    3444.735     0.999829      3731219      5851.43
    3444.735     0.999854      3731219      6826.67
    3446.783     0.999878      3731310      8192.00
    3448.831     0.999890      3731404      9102.22
    3448.831     0.999902      3731404     10240.00
    3450.879     0.999915      3731506     11702.86
    3450.879     0.999927      3731506     13653.33
    3452.927     0.999939      3731576     16384.00
    3452.927     0.999945      3731576     18204.44
    3454.975     0.999951      3731612     20480.00
    3454.975     0.999957      3731612     23405.71
    3457.023     0.999963      3731648     27306.67
    3457.023     0.999969      3731648     32768.00
    3459.071     0.999973      3731673     36408.89
    3459.071     0.999976      3731673     40960.00
    3461.119     0.999979      3731700     46811.43
    3461.119     0.999982      3731700     54613.33
    3463.167     0.999985      3731727     65536.00
    3463.167     0.999986      3731727     72817.78
    3463.167     0.999988      3731727     81920.00
    3463.167     0.999989      3731727     93622.86
    3463.167     0.999991      3731727    109226.67
    3465.215     0.999992      3731744    131072.00
    3465.215     0.999993      3731744    145635.56
    3465.215     0.999994      3731744    163840.00
    3465.215     0.999995      3731744    187245.71
    3465.215     0.999995      3731744    218453.33
    3467.263     0.999996      3731761    262144.00
    3467.263     1.000000      3731761          inf
#[Mean    =     1034.261, StdDeviation   =     1162.380]
#[Max     =     3465.216, Total count    =      3731761]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  4530971 requests in 1.00m, 383.29MB read
Requests/sec:  75585.72
Transfer/sec:      6.39MB
