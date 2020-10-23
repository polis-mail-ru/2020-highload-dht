-t1 -c64 -d60s -R2000 -s ./wrk/get.lua --latency http://localhost:8081
Running 1m test @ http://localhost:8081
  1 threads and 64 connections
  Thread calibration: mean lat.: 1.398ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.39ms  661.69us   8.61ms   68.09%
    Req/Sec     2.10k   210.07     3.20k    71.62%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.31ms
 75.000%    1.80ms
 90.000%    2.27ms
 99.000%    2.85ms
 99.900%    5.70ms
 99.990%    7.13ms
 99.999%    8.06ms
100.000%    8.61ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.057     0.000000            1         1.00
       0.608     0.100000         9958         1.11
       0.808     0.200000        19905         1.25
       0.978     0.300000        29860         1.43
       1.148     0.400000        39747         1.67
       1.311     0.500000        49686         2.00
       1.395     0.550000        54690         2.22
       1.484     0.600000        59663         2.50
       1.579     0.650000        64633         2.86
       1.682     0.700000        69552         3.33
       1.805     0.750000        74535         4.00
       1.874     0.775000        77036         4.44
       1.946     0.800000        79510         5.00
       2.021     0.825000        81997         5.71
       2.097     0.850000        84488         6.67
       2.181     0.875000        86945         8.00
       2.227     0.887500        88228         8.89
       2.271     0.900000        89448        10.00
       2.319     0.912500        90680        11.43
       2.371     0.925000        91963        13.33
       2.425     0.937500        93189        16.00
       2.453     0.943750        93809        17.78
       2.483     0.950000        94404        20.00
       2.517     0.956250        95022        22.86
       2.555     0.962500        95646        26.67
       2.595     0.968750        96255        32.00
       2.619     0.971875        96581        35.56
       2.645     0.975000        96891        40.00
       2.673     0.978125        97199        45.71
       2.705     0.981250        97511        53.33
       2.743     0.984375        97813        64.00
       2.767     0.985938        97979        71.11
       2.791     0.987500        98119        80.00
       2.825     0.989062        98283        91.43
       2.863     0.990625        98433       106.67
       2.939     0.992188        98585       128.00
       3.011     0.992969        98664       142.22
       3.185     0.993750        98739       160.00
       3.415     0.994531        98817       182.86
       3.733     0.995313        98895       213.33
       4.021     0.996094        98972       256.00
       4.191     0.996484        99012       284.44
       4.367     0.996875        99051       320.00
       4.523     0.997266        99090       365.71
       4.695     0.997656        99128       426.67
       4.959     0.998047        99166       512.00
       5.075     0.998242        99186       568.89
       5.215     0.998437        99205       640.00
       5.331     0.998633        99225       731.43
       5.487     0.998828        99244       853.33
       5.731     0.999023        99264      1024.00
       5.819     0.999121        99274      1137.78
       5.871     0.999219        99283      1280.00
       6.055     0.999316        99294      1462.86
       6.151     0.999414        99303      1706.67
       6.259     0.999512        99312      2048.00
       6.435     0.999561        99318      2275.56
       6.463     0.999609        99322      2560.00
       6.615     0.999658        99328      2925.71
       6.667     0.999707        99331      3413.33
       6.715     0.999756        99336      4096.00
       6.799     0.999780        99339      4551.11
       6.899     0.999805        99342      5120.00
       6.915     0.999829        99344      5851.43
       7.079     0.999854        99346      6826.67
       7.107     0.999878        99348      8192.00
       7.127     0.999890        99350      9102.22
       7.187     0.999902        99351     10240.00
       7.231     0.999915        99352     11702.86
       7.243     0.999927        99353     13653.33
       7.351     0.999939        99354     16384.00
       7.391     0.999945        99355     18204.44
       7.575     0.999951        99356     20480.00
       7.575     0.999957        99356     23405.71
       7.867     0.999963        99357     27306.67
       7.867     0.999969        99357     32768.00
       7.875     0.999973        99358     36408.89
       7.875     0.999976        99358     40960.00
       7.875     0.999979        99358     46811.43
       8.059     0.999982        99359     54613.33
       8.059     0.999985        99359     65536.00
       8.059     0.999986        99359     72817.78
       8.059     0.999988        99359     81920.00
       8.059     0.999989        99359     93622.86
       8.615     0.999991        99360    109226.67
       8.615     1.000000        99360          inf
#[Mean    =        1.386, StdDeviation   =        0.662]
#[Max     =        8.608, Total count    =        99360]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  119715 requests in 1.00m, 10.16MB read
Requests/sec:   1995.20
Transfer/sec:    173.39KB

-t64 -c64 -d60s -R40000 -s ./wrk/get.lua --latency http://localhost:8080
Running 1m test @ http://localhost:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 0.662ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.662ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.660ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.662ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.664ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.659ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.664ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.659ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.649ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.659ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.663ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.660ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.666ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.660ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.660ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.659ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   657.85us  338.07us  11.99ms   66.76%
    Req/Sec   672.96     46.36     1.22k    76.05%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%  649.00us
 75.000%    0.90ms
 90.000%    1.05ms
 99.000%    1.22ms
 99.900%    3.31ms
 99.990%    6.45ms
 99.999%    8.35ms
100.000%   12.00ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.041     0.000000            1         1.00
       0.241     0.100000       200544         1.11
       0.345     0.200000       401468         1.25
       0.447     0.300000       600420         1.43
       0.549     0.400000       801580         1.67
       0.649     0.500000       999772         2.00
       0.700     0.550000      1100087         2.22
       0.751     0.600000      1200637         2.50
       0.802     0.650000      1300515         2.86
       0.853     0.700000      1400847         3.33
       0.903     0.750000      1499643         4.00
       0.929     0.775000      1551185         4.44
       0.954     0.800000      1600409         5.00
       0.979     0.825000      1649861         5.71
       1.004     0.850000      1699646         6.67
       1.029     0.875000      1749221         8.00
       1.042     0.887500      1775205         8.89
       1.055     0.900000      1801087        10.00
       1.067     0.912500      1824611        11.43
       1.080     0.925000      1850505        13.33
       1.093     0.937500      1875788        16.00
       1.099     0.943750      1887061        17.78
       1.106     0.950000      1899462        20.00
       1.114     0.956250      1912487        22.86
       1.122     0.962500      1924582        26.67
       1.131     0.968750      1937083        32.00
       1.136     0.971875      1943441        35.56
       1.142     0.975000      1950004        40.00
       1.149     0.978125      1956035        45.71
       1.157     0.981250      1961703        53.33
       1.170     0.984375      1968067        64.00
       1.178     0.985938      1971005        71.11
       1.189     0.987500      1974224        80.00
       1.205     0.989062      1977272        91.43
       1.239     0.990625      1980388       106.67
       1.330     0.992188      1983508       128.00
       1.403     0.992969      1985043       142.22
       1.492     0.993750      1986614       160.00
       1.593     0.994531      1988176       182.86
       1.708     0.995313      1989736       213.33
       1.843     0.996094      1991293       256.00
       1.925     0.996484      1992072       284.44
       2.012     0.996875      1992855       320.00
       2.117     0.997266      1993643       365.71
       2.251     0.997656      1994420       426.67
       2.439     0.998047      1995196       512.00
       2.571     0.998242      1995585       568.89
       2.723     0.998437      1995977       640.00
       2.899     0.998633      1996368       731.43
       3.111     0.998828      1996758       853.33
       3.345     0.999023      1997149      1024.00
       3.489     0.999121      1997342      1137.78
       3.639     0.999219      1997538      1280.00
       3.807     0.999316      1997737      1462.86
       3.999     0.999414      1997928      1706.67
       4.235     0.999512      1998124      2048.00
       4.371     0.999561      1998222      2275.56
       4.499     0.999609      1998320      2560.00
       4.651     0.999658      1998419      2925.71
       4.863     0.999707      1998515      3413.33
       5.111     0.999756      1998612      4096.00
       5.267     0.999780      1998661      4551.11
       5.435     0.999805      1998710      5120.00
       5.627     0.999829      1998758      5851.43
       5.867     0.999854      1998807      6826.67
       6.147     0.999878      1998855      8192.00
       6.323     0.999890      1998881      9102.22
       6.491     0.999902      1998904     10240.00
       6.647     0.999915      1998929     11702.86
       6.795     0.999927      1998953     13653.33
       6.999     0.999939      1998977     16384.00
       7.163     0.999945      1998990     18204.44
       7.267     0.999951      1999002     20480.00
       7.323     0.999957      1999014     23405.71
       7.407     0.999963      1999027     27306.67
       7.531     0.999969      1999038     32768.00
       7.623     0.999973      1999045     36408.89
       7.719     0.999976      1999051     40960.00
       7.847     0.999979      1999057     46811.43
       7.991     0.999982      1999063     54613.33
       8.083     0.999985      1999069     65536.00
       8.155     0.999986      1999072     72817.78
       8.231     0.999988      1999076     81920.00
       8.255     0.999989      1999078     93622.86
       8.375     0.999991      1999081    109226.67
       8.479     0.999992      1999084    131072.00
       8.551     0.999993      1999087    145635.56
       8.551     0.999994      1999087    163840.00
       9.159     0.999995      1999089    187245.71
       9.271     0.999995      1999090    218453.33
       9.503     0.999996      1999092    262144.00
       9.519     0.999997      1999093    291271.11
       9.519     0.999997      1999093    327680.00
       9.847     0.999997      1999094    374491.43
       9.863     0.999998      1999095    436906.67
       9.895     0.999998      1999096    524288.00
       9.895     0.999998      1999096    582542.22
       9.895     0.999998      1999096    655360.00
      10.695     0.999999      1999097    748982.86
      10.695     0.999999      1999097    873813.33
      11.487     0.999999      1999098   1048576.00
      11.487     0.999999      1999098   1165084.44
      11.487     0.999999      1999098   1310720.00
      11.487     0.999999      1999098   1497965.71
      11.487     0.999999      1999098   1747626.67
      11.999     1.000000      1999099   2097152.00
      11.999     1.000000      1999099          inf
#[Mean    =        0.658, StdDeviation   =        0.338]
#[Max     =       11.992, Total count    =      1999099]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  2399348 requests in 1.00m, 202.36MB read
Requests/sec:  40025.90
Transfer/sec:      3.38MB

24.24 занимает работа селекторов (парсинг запроса и передача данных), оставшиеся 75,76% занимает работа ExecutorService (DAO.get 0.33 - 0.45%, отправка ответа <1% для каждого воркера)
MySimpleHttpServer.proxy < 2.64% для каждого воркера.

