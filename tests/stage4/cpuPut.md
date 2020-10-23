-t1 -c64 -d60s -R2000 -s ./wrk/put.lua --latency http://localhost:8081
Running 1m test @ http://localhost:8081
  1 threads and 64 connections
  Thread calibration: mean lat.: 1.467ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.42ms  679.72us   9.50ms   68.92%
    Req/Sec     2.11k   249.84     3.44k    69.71%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.35ms
 75.000%    1.86ms
 90.000%    2.30ms
 99.000%    2.86ms
 99.900%    6.32ms
 99.990%    8.33ms
 99.999%    9.19ms
100.000%    9.51ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.058     0.000000            1         1.00
       0.637     0.100000         9960         1.11
       0.838     0.200000        19896         1.25
       1.013     0.300000        29825         1.43
       1.184     0.400000        39790         1.67
       1.345     0.500000        49720         2.00
       1.427     0.550000        54675         2.22
       1.514     0.600000        59637         2.50
       1.615     0.650000        64615         2.86
       1.727     0.700000        69564         3.33
       1.856     0.750000        74518         4.00
       1.928     0.775000        77024         4.44
       2.000     0.800000        79489         5.00
       2.071     0.825000        82006         5.71
       2.141     0.850000        84480         6.67
       2.219     0.875000        87015         8.00
       2.257     0.887500        88224         8.89
       2.297     0.900000        89454        10.00
       2.339     0.912500        90702        11.43
       2.383     0.925000        91951        13.33
       2.429     0.937500        93189        16.00
       2.453     0.943750        93772        17.78
       2.483     0.950000        94428        20.00
       2.513     0.956250        95024        22.86
       2.547     0.962500        95643        26.67
       2.587     0.968750        96276        32.00
       2.609     0.971875        96587        35.56
       2.635     0.975000        96889        40.00
       2.663     0.978125        97190        45.71
       2.699     0.981250        97514        53.33
       2.737     0.984375        97810        64.00
       2.763     0.985938        97964        71.11
       2.791     0.987500        98125        80.00
       2.829     0.989062        98278        91.43
       2.885     0.990625        98429       106.67
       3.051     0.992188        98582       128.00
       3.263     0.992969        98659       142.22
       3.613     0.993750        98737       160.00
       3.907     0.994531        98814       182.86
       4.223     0.995313        98895       213.33
       4.547     0.996094        98969       256.00
       4.731     0.996484        99010       284.44
       4.919     0.996875        99047       320.00
       5.071     0.997266        99086       365.71
       5.323     0.997656        99125       426.67
       5.535     0.998047        99164       512.00
       5.655     0.998242        99183       568.89
       5.751     0.998437        99202       640.00
       5.919     0.998633        99222       731.43
       6.151     0.998828        99241       853.33
       6.327     0.999023        99260      1024.00
       6.439     0.999121        99270      1137.78
       6.587     0.999219        99280      1280.00
       6.659     0.999316        99290      1462.86
       6.791     0.999414        99299      1706.67
       6.991     0.999512        99309      2048.00
       7.111     0.999561        99314      2275.56
       7.195     0.999609        99319      2560.00
       7.371     0.999658        99324      2925.71
       7.419     0.999707        99328      3413.33
       7.675     0.999756        99334      4096.00
       7.699     0.999780        99336      4551.11
       7.759     0.999805        99338      5120.00
       8.003     0.999829        99341      5851.43
       8.095     0.999854        99343      6826.67
       8.303     0.999878        99346      8192.00
       8.327     0.999890        99347      9102.22
       8.351     0.999902        99348     10240.00
       8.407     0.999915        99349     11702.86
       8.471     0.999927        99350     13653.33
       8.575     0.999939        99351     16384.00
       8.591     0.999945        99353     18204.44
       8.591     0.999951        99353     20480.00
       8.591     0.999957        99353     23405.71
       8.703     0.999963        99354     27306.67
       8.703     0.999969        99354     32768.00
       8.775     0.999973        99355     36408.89
       8.775     0.999976        99355     40960.00
       8.775     0.999979        99355     46811.43
       9.191     0.999982        99356     54613.33
       9.191     0.999985        99356     65536.00
       9.191     0.999986        99356     72817.78
       9.191     0.999988        99356     81920.00
       9.191     0.999989        99356     93622.86
       9.511     0.999991        99357    109226.67
       9.511     1.000000        99357          inf
#[Mean    =        1.423, StdDeviation   =        0.680]
#[Max     =        9.504, Total count    =        99357]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  119713 requests in 1.00m, 9.48MB read
Requests/sec:   1995.17
Transfer/sec:    161.72KB

-t64 -c64 -d60s -R40000 -s ./wrk/put.lua --latency http://localhost:8080
Running 1m test @ http://localhost:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.662ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.660ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.659ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.646ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.650ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.662ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.661ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.661ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.661ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.655ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.651ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.656ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.654ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.647ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   652.66us  332.60us   9.02ms   65.69%
    Req/Sec   674.73     45.53     1.11k    77.32%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%  645.00us
 75.000%    0.90ms
 90.000%    1.05ms
 99.000%    1.22ms
 99.900%    3.19ms
 99.990%    5.49ms
 99.999%    7.26ms
100.000%    9.03ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.040     0.000000            1         1.00
       0.237     0.100000       201341         1.11
       0.340     0.200000       401143         1.25
       0.442     0.300000       600037         1.43
       0.544     0.400000       800549         1.67
       0.645     0.500000      1000725         2.00
       0.696     0.550000      1101395         2.22
       0.746     0.600000      1199582         2.50
       0.797     0.650000      1300665         2.86
       0.848     0.700000      1400696         3.33
       0.898     0.750000      1499772         4.00
       0.924     0.775000      1551131         4.44
       0.949     0.800000      1600572         5.00
       0.974     0.825000      1649962         5.71
       1.000     0.850000      1701199         6.67
       1.025     0.875000      1750562         8.00
       1.037     0.887500      1774483         8.89
       1.050     0.900000      1800109        10.00
       1.063     0.912500      1825892        11.43
       1.075     0.925000      1849721        13.33
       1.088     0.937500      1874969        16.00
       1.095     0.943750      1888272        17.78
       1.101     0.950000      1899563        20.00
       1.109     0.956250      1912765        22.86
       1.117     0.962500      1924639        26.67
       1.126     0.968750      1937161        32.00
       1.131     0.971875      1943604        35.56
       1.136     0.975000      1949399        40.00
       1.143     0.978125      1955987        45.71
       1.151     0.981250      1961730        53.33
       1.163     0.984375      1967915        64.00
       1.172     0.985938      1971236        71.11
       1.182     0.987500      1974129        80.00
       1.199     0.989062      1977353        91.43
       1.232     0.990625      1980359       106.67
       1.324     0.992188      1983461       128.00
       1.397     0.992969      1985021       142.22
       1.485     0.993750      1986593       160.00
       1.583     0.994531      1988149       182.86
       1.700     0.995313      1989711       213.33
       1.831     0.996094      1991280       256.00
       1.907     0.996484      1992049       284.44
       1.998     0.996875      1992835       320.00
       2.101     0.997266      1993623       365.71
       2.223     0.997656      1994397       426.67
       2.369     0.998047      1995174       512.00
       2.471     0.998242      1995564       568.89
       2.601     0.998437      1995957       640.00
       2.753     0.998633      1996346       731.43
       2.983     0.998828      1996739       853.33
       3.227     0.999023      1997125      1024.00
       3.383     0.999121      1997322      1137.78
       3.511     0.999219      1997517      1280.00
       3.683     0.999316      1997711      1462.86
       3.859     0.999414      1997906      1706.67
       4.071     0.999512      1998101      2048.00
       4.207     0.999561      1998201      2275.56
       4.339     0.999609      1998297      2560.00
       4.471     0.999658      1998395      2925.71
       4.619     0.999707      1998495      3413.33
       4.763     0.999756      1998589      4096.00
       4.843     0.999780      1998639      4551.11
       4.927     0.999805      1998687      5120.00
       5.051     0.999829      1998736      5851.43
       5.163     0.999854      1998786      6826.67
       5.327     0.999878      1998833      8192.00
       5.411     0.999890      1998858      9102.22
       5.507     0.999902      1998882     10240.00
       5.643     0.999915      1998907     11702.86
       5.743     0.999927      1998932     13653.33
       5.887     0.999939      1998955     16384.00
       5.995     0.999945      1998968     18204.44
       6.115     0.999951      1998980     20480.00
       6.195     0.999957      1998993     23405.71
       6.291     0.999963      1999004     27306.67
       6.451     0.999969      1999016     32768.00
       6.507     0.999973      1999024     36408.89
       6.603     0.999976      1999029     40960.00
       6.791     0.999979      1999035     46811.43
       6.875     0.999982      1999041     54613.33
       7.123     0.999985      1999047     65536.00
       7.159     0.999986      1999050     72817.78
       7.207     0.999988      1999053     81920.00
       7.247     0.999989      1999056     93622.86
       7.439     0.999991      1999060    109226.67
       7.471     0.999992      1999062    131072.00
       7.547     0.999993      1999064    145635.56
       7.575     0.999994      1999065    163840.00
       7.671     0.999995      1999067    187245.71
       7.759     0.999995      1999068    218453.33
       7.883     0.999996      1999070    262144.00
       7.931     0.999997      1999071    291271.11
       7.931     0.999997      1999071    327680.00
       7.959     0.999997      1999072    374491.43
       8.287     0.999998      1999073    436906.67
       8.375     0.999998      1999074    524288.00
       8.375     0.999998      1999074    582542.22
       8.375     0.999998      1999074    655360.00
       8.607     0.999999      1999075    748982.86
       8.607     0.999999      1999075    873813.33
       8.655     0.999999      1999076   1048576.00
       8.655     0.999999      1999076   1165084.44
       8.655     0.999999      1999076   1310720.00
       8.655     0.999999      1999076   1497965.71
       8.655     0.999999      1999076   1747626.67
       9.031     1.000000      1999077   2097152.00
       9.031     1.000000      1999077          inf
#[Mean    =        0.653, StdDeviation   =        0.333]
#[Max     =        9.024, Total count    =      1999077]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  2399319 requests in 1.00m, 189.92MB read
Requests/sec:  40033.62
Transfer/sec:      3.17MB

26.18% занимает работа селекторов (парсинг запроса и передача данных), оставшиеся 73,82% занимает работа ExecutorService (DAO.upsert 0.1 - 0.2 %, отправка ответа ~1% для каждого воркера)

