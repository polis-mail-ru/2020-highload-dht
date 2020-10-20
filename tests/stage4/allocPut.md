-t1 -c64 -d60s -R2000 -s ./wrk/put.lua --latency http://localhost:8081
Running 1m test @ http://localhost:8081
  1 threads and 64 connections
  Thread calibration: mean lat.: 1.520ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.47ms  705.08us  10.63ms   68.84%
    Req/Sec     2.11k   260.26     3.56k    67.12%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.40ms
 75.000%    1.87ms
 90.000%    2.41ms
 99.000%    3.00ms
 99.900%    6.43ms
 99.990%    8.04ms
 99.999%    8.70ms
100.000%   10.64ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.066     0.000000            1         1.00
       0.654     0.100000         9959         1.11
       0.867     0.200000        19894         1.25
       1.062     0.300000        29819         1.43
       1.243     0.400000        39757         1.67
       1.405     0.500000        49721         2.00
       1.483     0.550000        54691         2.22
       1.564     0.600000        59654         2.50
       1.651     0.650000        64628         2.86
       1.746     0.700000        69590         3.33
       1.867     0.750000        74521         4.00
       1.947     0.775000        77017         4.44
       2.037     0.800000        79511         5.00
       2.135     0.825000        81986         5.71
       2.229     0.850000        84503         6.67
       2.319     0.875000        86953         8.00
       2.363     0.887500        88182         8.89
       2.409     0.900000        89481        10.00
       2.453     0.912500        90690        11.43
       2.503     0.925000        91945        13.33
       2.555     0.937500        93162        16.00
       2.583     0.943750        93794        17.78
       2.613     0.950000        94406        20.00
       2.643     0.956250        95021        22.86
       2.679     0.962500        95646        26.67
       2.717     0.968750        96278        32.00
       2.739     0.971875        96574        35.56
       2.765     0.975000        96886        40.00
       2.791     0.978125        97193        45.71
       2.825     0.981250        97518        53.33
       2.859     0.984375        97811        64.00
       2.881     0.985938        97966        71.11
       2.911     0.987500        98119        80.00
       2.953     0.989062        98277        91.43
       3.041     0.990625        98428       106.67
       3.245     0.992188        98583       128.00
       3.449     0.992969        98661       142.22
       3.721     0.993750        98739       160.00
       4.069     0.994531        98816       182.86
       4.459     0.995313        98895       213.33
       4.723     0.996094        98971       256.00
       4.851     0.996484        99010       284.44
       5.071     0.996875        99049       320.00
       5.275     0.997266        99088       365.71
       5.527     0.997656        99127       426.67
       5.771     0.998047        99166       512.00
       5.867     0.998242        99185       568.89
       5.991     0.998437        99205       640.00
       6.115     0.998633        99224       731.43
       6.283     0.998828        99244       853.33
       6.443     0.999023        99262      1024.00
       6.515     0.999121        99272      1137.78
       6.727     0.999219        99282      1280.00
       6.915     0.999316        99293      1462.86
       7.019     0.999414        99301      1706.67
       7.195     0.999512        99312      2048.00
       7.223     0.999561        99316      2275.56
       7.271     0.999609        99321      2560.00
       7.339     0.999658        99326      2925.71
       7.427     0.999707        99330      3413.33
       7.607     0.999756        99335      4096.00
       7.687     0.999780        99338      4551.11
       7.707     0.999805        99340      5120.00
       7.803     0.999829        99343      5851.43
       7.915     0.999854        99345      6826.67
       7.939     0.999878        99347      8192.00
       8.039     0.999890        99349      9102.22
       8.043     0.999902        99350     10240.00
       8.063     0.999915        99351     11702.86
       8.263     0.999927        99352     13653.33
       8.287     0.999939        99353     16384.00
       8.295     0.999945        99354     18204.44
       8.351     0.999951        99355     20480.00
       8.351     0.999957        99355     23405.71
       8.415     0.999963        99356     27306.67
       8.415     0.999969        99356     32768.00
       8.439     0.999973        99357     36408.89
       8.439     0.999976        99357     40960.00
       8.439     0.999979        99357     46811.43
       8.703     0.999982        99358     54613.33
       8.703     0.999985        99358     65536.00
       8.703     0.999986        99358     72817.78
       8.703     0.999988        99358     81920.00
       8.703     0.999989        99358     93622.86
      10.639     0.999991        99359    109226.67
      10.639     1.000000        99359          inf
#[Mean    =        1.473, StdDeviation   =        0.705]
#[Max     =       10.632, Total count    =        99359]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  119714 requests in 1.00m, 9.48MB read
Requests/sec:   1995.19
Transfer/sec:    161.72KB


-t64 -c64 -d60s -R40000 -s ./wrk/put.lua --latency http://localhost:8080
Running 1m test @ http://localhost:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.648ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.645ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.648ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.648ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.649ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.649ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.644ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.646ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.648ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.648ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.649ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.646ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.649ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.648ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.649ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.648ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   665.96us  368.07us  17.34ms   72.26%
    Req/Sec   672.65     47.64     1.78k    75.35%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%  653.00us
 75.000%    0.91ms
 90.000%    1.06ms
 99.000%    1.50ms
 99.900%    3.64ms
 99.990%    8.57ms
 99.999%   13.05ms
100.000%   17.36ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.041     0.000000            1         1.00
       0.241     0.100000       201046         1.11
       0.346     0.200000       401366         1.25
       0.449     0.300000       600044         1.43
       0.552     0.400000       801411         1.67
       0.653     0.500000      1000341         2.00
       0.704     0.550000      1100363         2.22
       0.755     0.600000      1200052         2.50
       0.807     0.650000      1301087         2.86
       0.858     0.700000      1401123         3.33
       0.908     0.750000      1499873         4.00
       0.934     0.775000      1551183         4.44
       0.959     0.800000      1600043         5.00
       0.984     0.825000      1649268         5.71
       1.010     0.850000      1700577         6.67
       1.035     0.875000      1750043         8.00
       1.048     0.887500      1775815         8.89
       1.060     0.900000      1799354        10.00
       1.073     0.912500      1825027        11.43
       1.086     0.925000      1850350        13.33
       1.099     0.937500      1875160        16.00
       1.106     0.943750      1886991        17.78
       1.114     0.950000      1899562        20.00
       1.123     0.956250      1912564        22.86
       1.132     0.962500      1924243        26.67
       1.144     0.968750      1936776        32.00
       1.152     0.971875      1943089        35.56
       1.162     0.975000      1949245        40.00
       1.176     0.978125      1955392        45.71
       1.198     0.981250      1961604        53.33
       1.247     0.984375      1967838        64.00
       1.295     0.985938      1970966        71.11
       1.362     0.987500      1974083        80.00
       1.445     0.989062      1977183        91.43
       1.544     0.990625      1980301       106.67
       1.665     0.992188      1983426       128.00
       1.736     0.992969      1984989       142.22
       1.814     0.993750      1986547       160.00
       1.902     0.994531      1988109       182.86
       2.006     0.995313      1989678       213.33
       2.137     0.996094      1991241       256.00
       2.211     0.996484      1992023       284.44
       2.295     0.996875      1992804       320.00
       2.393     0.997266      1993583       365.71
       2.517     0.997656      1994354       426.67
       2.697     0.998047      1995135       512.00
       2.825     0.998242      1995530       568.89
       2.991     0.998437      1995920       640.00
       3.183     0.998633      1996306       731.43
       3.403     0.998828      1996701       853.33
       3.677     0.999023      1997091      1024.00
       3.845     0.999121      1997286      1137.78
       4.027     0.999219      1997478      1280.00
       4.231     0.999316      1997675      1462.86
       4.511     0.999414      1997869      1706.67
       4.851     0.999512      1998063      2048.00
       5.015     0.999561      1998163      2275.56
       5.291     0.999609      1998262      2560.00
       5.579     0.999658      1998356      2925.71
       5.951     0.999707      1998455      3413.33
       6.371     0.999756      1998554      4096.00
       6.655     0.999780      1998600      4551.11
       7.019     0.999805      1998649      5120.00
       7.391     0.999829      1998698      5851.43
       7.839     0.999854      1998749      6826.67
       8.163     0.999878      1998795      8192.00
       8.391     0.999890      1998822      9102.22
       8.599     0.999902      1998845     10240.00
       8.879     0.999915      1998870     11702.86
       9.207     0.999927      1998893     13653.33
       9.543     0.999939      1998917     16384.00
       9.935     0.999945      1998930     18204.44
      10.143     0.999951      1998942     20480.00
      10.599     0.999957      1998954     23405.71
      11.015     0.999963      1998966     27306.67
      11.359     0.999969      1998978     32768.00
      11.479     0.999973      1998985     36408.89
      11.751     0.999976      1998991     40960.00
      11.975     0.999979      1998997     46811.43
      12.127     0.999982      1999003     54613.33
      12.527     0.999985      1999010     65536.00
      12.575     0.999986      1999012     72817.78
      12.631     0.999988      1999015     81920.00
      12.807     0.999989      1999018     93622.86
      13.119     0.999991      1999021    109226.67
      13.399     0.999992      1999024    131072.00
      13.471     0.999993      1999026    145635.56
      13.543     0.999994      1999027    163840.00
      13.823     0.999995      1999029    187245.71
      13.911     0.999995      1999030    218453.33
      14.407     0.999996      1999032    262144.00
      14.431     0.999997      1999033    291271.11
      14.431     0.999997      1999033    327680.00
      14.639     0.999997      1999034    374491.43
      14.879     0.999998      1999035    436906.67
      15.351     0.999998      1999036    524288.00
      15.351     0.999998      1999036    582542.22
      15.351     0.999998      1999036    655360.00
      15.951     0.999999      1999037    748982.86
      15.951     0.999999      1999037    873813.33
      16.047     0.999999      1999038   1048576.00
      16.047     0.999999      1999038   1165084.44
      16.047     0.999999      1999038   1310720.00
      16.047     0.999999      1999038   1497965.71
      16.047     0.999999      1999038   1747626.67
      17.359     1.000000      1999039   2097152.00
      17.359     1.000000      1999039          inf
#[Mean    =        0.666, StdDeviation   =        0.368]
#[Max     =       17.344, Total count    =      1999039]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  2399290 requests in 1.00m, 189.92MB read
Requests/sec:  40031.51
Transfer/sec:      3.17MB

