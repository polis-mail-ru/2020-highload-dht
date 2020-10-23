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

39,72% занимает работа селекторов, оставшиеся 60,28% занимает работа ExecutorService (т.е. непосредственно DAO.upsert (0.54 - 0.55% для каждого воркера ) и отправка ответов)
В предыдущей реализации DAO.upsert был около 3% для каждого воркера.

-t64 -c64 -d60s -R80000 -s ./wrk/put.lua --latency http://localhost:8080
Running 1m test @ http://localhost:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 65.572ms, rate sampling interval: 502ms
  Thread calibration: mean lat.: 67.992ms, rate sampling interval: 526ms
  Thread calibration: mean lat.: 65.489ms, rate sampling interval: 511ms
  Thread calibration: mean lat.: 63.835ms, rate sampling interval: 512ms
  Thread calibration: mean lat.: 61.639ms, rate sampling interval: 495ms
  Thread calibration: mean lat.: 65.225ms, rate sampling interval: 518ms
  Thread calibration: mean lat.: 66.986ms, rate sampling interval: 511ms
  Thread calibration: mean lat.: 63.285ms, rate sampling interval: 499ms
  Thread calibration: mean lat.: 65.217ms, rate sampling interval: 514ms
  Thread calibration: mean lat.: 62.649ms, rate sampling interval: 492ms
  Thread calibration: mean lat.: 65.134ms, rate sampling interval: 505ms
  Thread calibration: mean lat.: 61.944ms, rate sampling interval: 494ms
  Thread calibration: mean lat.: 63.841ms, rate sampling interval: 498ms
  Thread calibration: mean lat.: 66.924ms, rate sampling interval: 517ms
  Thread calibration: mean lat.: 61.986ms, rate sampling interval: 497ms
  Thread calibration: mean lat.: 65.868ms, rate sampling interval: 511ms
  Thread calibration: mean lat.: 66.595ms, rate sampling interval: 517ms
  Thread calibration: mean lat.: 62.786ms, rate sampling interval: 504ms
  Thread calibration: mean lat.: 60.916ms, rate sampling interval: 485ms
  Thread calibration: mean lat.: 63.177ms, rate sampling interval: 501ms
  Thread calibration: mean lat.: 66.662ms, rate sampling interval: 511ms
  Thread calibration: mean lat.: 64.944ms, rate sampling interval: 512ms
  Thread calibration: mean lat.: 64.504ms, rate sampling interval: 513ms
  Thread calibration: mean lat.: 60.719ms, rate sampling interval: 484ms
  Thread calibration: mean lat.: 62.941ms, rate sampling interval: 494ms
  Thread calibration: mean lat.: 65.221ms, rate sampling interval: 509ms
  Thread calibration: mean lat.: 62.233ms, rate sampling interval: 492ms
  Thread calibration: mean lat.: 61.985ms, rate sampling interval: 485ms
  Thread calibration: mean lat.: 61.964ms, rate sampling interval: 494ms
  Thread calibration: mean lat.: 64.540ms, rate sampling interval: 500ms
  Thread calibration: mean lat.: 61.842ms, rate sampling interval: 487ms
  Thread calibration: mean lat.: 59.941ms, rate sampling interval: 470ms
  Thread calibration: mean lat.: 58.433ms, rate sampling interval: 469ms
  Thread calibration: mean lat.: 57.680ms, rate sampling interval: 470ms
  Thread calibration: mean lat.: 54.995ms, rate sampling interval: 458ms
  Thread calibration: mean lat.: 56.154ms, rate sampling interval: 460ms
  Thread calibration: mean lat.: 57.792ms, rate sampling interval: 468ms
  Thread calibration: mean lat.: 53.433ms, rate sampling interval: 437ms
  Thread calibration: mean lat.: 53.908ms, rate sampling interval: 448ms
  Thread calibration: mean lat.: 54.853ms, rate sampling interval: 449ms
  Thread calibration: mean lat.: 53.845ms, rate sampling interval: 439ms
  Thread calibration: mean lat.: 54.917ms, rate sampling interval: 446ms
  Thread calibration: mean lat.: 54.658ms, rate sampling interval: 450ms
  Thread calibration: mean lat.: 52.198ms, rate sampling interval: 433ms
  Thread calibration: mean lat.: 57.379ms, rate sampling interval: 463ms
  Thread calibration: mean lat.: 51.939ms, rate sampling interval: 430ms
  Thread calibration: mean lat.: 53.734ms, rate sampling interval: 448ms
  Thread calibration: mean lat.: 56.559ms, rate sampling interval: 455ms
  Thread calibration: mean lat.: 51.471ms, rate sampling interval: 426ms
  Thread calibration: mean lat.: 54.750ms, rate sampling interval: 450ms
  Thread calibration: mean lat.: 53.959ms, rate sampling interval: 450ms
  Thread calibration: mean lat.: 57.336ms, rate sampling interval: 468ms
  Thread calibration: mean lat.: 55.085ms, rate sampling interval: 448ms
  Thread calibration: mean lat.: 52.708ms, rate sampling interval: 441ms
  Thread calibration: mean lat.: 55.614ms, rate sampling interval: 455ms
  Thread calibration: mean lat.: 53.010ms, rate sampling interval: 438ms
  Thread calibration: mean lat.: 56.860ms, rate sampling interval: 462ms
  Thread calibration: mean lat.: 53.423ms, rate sampling interval: 443ms
  Thread calibration: mean lat.: 54.325ms, rate sampling interval: 449ms
  Thread calibration: mean lat.: 52.165ms, rate sampling interval: 439ms
  Thread calibration: mean lat.: 52.840ms, rate sampling interval: 439ms
  Thread calibration: mean lat.: 56.388ms, rate sampling interval: 454ms
  Thread calibration: mean lat.: 56.635ms, rate sampling interval: 458ms
  Thread calibration: mean lat.: 56.024ms, rate sampling interval: 455ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    56.75ms  104.13ms 429.31ms   82.13%
    Req/Sec     1.24k    23.25     1.33k    89.31%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.41ms
 75.000%   45.69ms
 90.000%  260.73ms
 99.000%  356.86ms
 99.900%  412.93ms
 99.990%  427.52ms
 99.999%  429.05ms
100.000%  429.57ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.033     0.000000            1         1.00
       0.594     0.100000       397946         1.11
       0.816     0.200000       794637         1.25
       0.998     0.300000      1192040         1.43
       1.171     0.400000      1587712         1.67
       1.415     0.500000      1984762         2.00
       1.544     0.550000      2183350         2.22
       1.708     0.600000      2380989         2.50
       2.657     0.650000      2578975         2.86
      15.255     0.700000      2777293         3.33
      45.695     0.750000      2975662         4.00
      66.559     0.775000      3074945         4.44
     130.047     0.800000      3174089         5.00
     165.887     0.825000      3273503         5.71
     191.999     0.850000      3372793         6.67
     232.959     0.875000      3471781         8.00
     248.063     0.887500      3521295         8.89
     260.735     0.900000      3571103        10.00
     272.383     0.912500      3621061        11.43
     281.599     0.925000      3670841        13.33
     291.583     0.937500      3720321        16.00
     296.959     0.943750      3744493        17.78
     302.079     0.950000      3769224        20.00
     308.479     0.956250      3794223        22.86
     315.903     0.962500      3819042        26.67
     324.351     0.968750      3844111        32.00
     329.215     0.971875      3856357        35.56
     333.823     0.975000      3868400        40.00
     337.919     0.978125      3880974        45.71
     342.271     0.981250      3893327        53.33
     346.623     0.984375      3905651        64.00
     348.927     0.985938      3911957        71.11
     351.999     0.987500      3918425        80.00
     354.559     0.989062      3924298        91.43
     358.143     0.990625      3930600       106.67
     360.959     0.992188      3936624       128.00
     362.495     0.992969      3939814       142.22
     364.799     0.993750      3942818       160.00
     367.359     0.994531      3945929       182.86
     371.455     0.995313      3948926       213.33
     374.783     0.996094      3952133       256.00
     377.855     0.996484      3953560       284.44
     380.927     0.996875      3955102       320.00
     388.351     0.997266      3956674       365.71
     394.239     0.997656      3958246       426.67
     401.407     0.998047      3959797       512.00
     403.711     0.998242      3960564       568.89
     405.759     0.998437      3961325       640.00
     408.063     0.998633      3962106       731.43
     410.623     0.998828      3962920       853.33
     413.439     0.999023      3963620      1024.00
     414.719     0.999121      3964005      1137.78
     416.255     0.999219      3964439      1280.00
     417.535     0.999316      3964872      1462.86
     418.303     0.999414      3965187      1706.67
     419.327     0.999512      3965641      2048.00
     419.839     0.999561      3965838      2275.56
     420.095     0.999609      3965943      2560.00
     420.607     0.999658      3966177      2925.71
     421.119     0.999707      3966348      3413.33
     421.887     0.999756      3966528      4096.00
     422.655     0.999780      3966636      4551.11
     423.423     0.999805      3966721      5120.00
     424.703     0.999829      3966817      5851.43
     426.239     0.999854      3966926      6826.67
     426.495     0.999878      3967009      8192.00
     427.007     0.999890      3967059      9102.22
     427.519     0.999902      3967116     10240.00
     427.775     0.999915      3967209     11702.86
     427.775     0.999927      3967209     13653.33
     428.031     0.999939      3967267     16384.00
     428.287     0.999945      3967299     18204.44
     428.287     0.999951      3967299     20480.00
     428.543     0.999957      3967351     23405.71
     428.543     0.999963      3967351     27306.67
     428.799     0.999969      3967418     32768.00
     428.799     0.999973      3967418     36408.89
     428.799     0.999976      3967418     40960.00
     428.799     0.999979      3967418     46811.43
     429.055     0.999982      3967465     54613.33
     429.055     0.999985      3967465     65536.00
     429.055     0.999986      3967465     72817.78
     429.055     0.999988      3967465     81920.00
     429.055     0.999989      3967465     93622.86
     429.055     0.999991      3967465    109226.67
     429.055     0.999992      3967465    131072.00
     429.055     0.999993      3967465    145635.56
     429.311     0.999994      3967489    163840.00
     429.311     0.999995      3967489    187245.71
     429.311     0.999995      3967489    218453.33
     429.311     0.999996      3967489    262144.00
     429.311     0.999997      3967489    291271.11
     429.311     0.999997      3967489    327680.00
     429.311     0.999997      3967489    374491.43
     429.311     0.999998      3967489    436906.67
     429.311     0.999998      3967489    524288.00
     429.311     0.999998      3967489    582542.22
     429.311     0.999998      3967489    655360.00
     429.311     0.999999      3967489    748982.86
     429.311     0.999999      3967489    873813.33
     429.311     0.999999      3967489   1048576.00
     429.311     0.999999      3967489   1165084.44
     429.311     0.999999      3967489   1310720.00
     429.311     0.999999      3967489   1497965.71
     429.311     0.999999      3967489   1747626.67
     429.567     1.000000      3967491   2097152.00
     429.567     1.000000      3967491          inf
#[Mean    =       56.746, StdDeviation   =      104.133]
#[Max     =      429.312, Total count    =      3967491]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  4767722 requests in 1.00m, 377.40MB read
Requests/sec:  79531.68
Transfer/sec:      6.30MB


