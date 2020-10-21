# PUT
wrk2 -t64 -c64 -d30s -R* -s test-results/lua/wrk2-scripts/put-request.lua --latency\\
10k:
```Running 30s test @ http://127.0.0.1:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 1.104ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.175ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.018ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.142ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.809ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.516ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.071ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.072ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.109ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.046ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.055ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.754ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.860ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.777ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.180ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.064ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.290ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.115ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.092ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.129ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.038ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.144ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.039ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.084ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.850ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.024ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.766ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.114ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.645ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.049ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.809ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.858ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.701ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.222ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.607ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.118ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.877ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.309ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.101ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.199ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.414ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.186ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.218ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.623ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.866ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.657ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.646ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.595ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.959ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.635ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.866ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.595ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.603ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.011ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.777ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.652ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.123ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.808ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.614ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.231ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.701ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.543ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.138ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.302ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.05ms    2.34ms  50.98ms   96.16%
    Req/Sec    73.85     89.27     1.00k    73.84%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%  706.00us
 75.000%    0.98ms
 90.000%    1.22ms
 99.000%    8.36ms
 99.900%   38.11ms
 99.990%   48.13ms
 99.999%   50.85ms
100.000%   51.01ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.029     0.000000            2         1.00
       0.254     0.100000         6562         1.11
       0.371     0.200000        13126         1.25
       0.483     0.300000        19696         1.43
       0.595     0.400000        26210         1.67
       0.706     0.500000        32762         2.00
       0.761     0.550000        36025         2.22
       0.816     0.600000        39340         2.50
       0.871     0.650000        42576         2.86
       0.927     0.700000        45878         3.33
       0.982     0.750000        49126         4.00
       1.010     0.775000        50790         4.44
       1.037     0.800000        52448         5.00
       1.064     0.825000        54059         5.71
       1.093     0.850000        55691         6.67
       1.129     0.875000        57338         8.00
       1.153     0.887500        58138         8.89
       1.217     0.900000        58953        10.00
       1.431     0.912500        59767        11.43
       1.775     0.925000        60584        13.33
       2.225     0.937500        61402        16.00
       2.489     0.943750        61813        17.78
       2.793     0.950000        62222        20.00
       3.095     0.956250        62631        22.86
       3.443     0.962500        63040        26.67
       3.903     0.968750        63449        32.00
       4.195     0.971875        63657        35.56
       4.539     0.975000        63862        40.00
       4.983     0.978125        64065        45.71
       5.491     0.981250        64270        53.33
       6.195     0.984375        64472        64.00
       6.599     0.985938        64576        71.11
       7.067     0.987500        64677        80.00
       7.847     0.989062        64781        91.43
       8.775     0.990625        64881       106.67
      10.247     0.992188        64984       128.00
      11.183     0.992969        65035       142.22
      12.135     0.993750        65086       160.00
      13.903     0.994531        65137       182.86
      16.399     0.995313        65188       213.33
      18.991     0.996094        65241       256.00
      21.055     0.996484        65265       284.44
      22.943     0.996875        65291       320.00
      24.847     0.997266        65316       365.71
      28.063     0.997656        65342       426.67
      30.223     0.998047        65368       512.00
      32.159     0.998242        65380       568.89
      33.631     0.998437        65394       640.00
      34.751     0.998633        65406       731.43
      36.543     0.998828        65419       853.33
      38.239     0.999023        65432      1024.00
      38.975     0.999121        65438      1137.78
      39.519     0.999219        65444      1280.00
      40.703     0.999316        65451      1462.86
      41.631     0.999414        65458      1706.67
      42.495     0.999512        65464      2048.00
      43.199     0.999561        65467      2275.56
      44.223     0.999609        65470      2560.00
      44.287     0.999658        65474      2925.71
      44.799     0.999707        65476      3413.33
      45.919     0.999756        65480      4096.00
      46.367     0.999780        65482      4551.11
      46.623     0.999805        65483      5120.00
      46.911     0.999829        65484      5851.43
      47.199     0.999854        65486      6826.67
      48.127     0.999878        65488      8192.00
      48.127     0.999890        65488      9102.22
      48.287     0.999902        65489     10240.00
      48.927     0.999915        65490     11702.86
      49.119     0.999927        65491     13653.33
      49.855     0.999939        65492     16384.00
      49.855     0.999945        65492     18204.44
      49.855     0.999951        65492     20480.00
      50.431     0.999957        65493     23405.71
      50.431     0.999963        65493     27306.67
      50.847     0.999969        65494     32768.00
      50.847     0.999973        65494     36408.89
      50.847     0.999976        65494     40960.00
      50.847     0.999979        65494     46811.43
      50.847     0.999982        65494     54613.33
      51.007     0.999985        65495     65536.00
      51.007     1.000000        65495          inf
#[Mean    =        1.049, StdDeviation   =        2.342]
#[Max     =       50.976, Total count    =        65495]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  109825 requests in 30.16s, 8.69MB read
  Socket errors: connect 0, read 0, write 0, timeout 579
Requests/sec:   3640.90
Transfer/sec:    295.07KB
```
30k:
```Running 30s test @ http://127.0.0.1:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 4.583ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1502.699ms, rate sampling interval: 8134ms
  Thread calibration: mean lat.: 1489.096ms, rate sampling interval: 8044ms
  Thread calibration: mean lat.: 1460.817ms, rate sampling interval: 7905ms
  Thread calibration: mean lat.: 1524.763ms, rate sampling interval: 7827ms
  Thread calibration: mean lat.: 1499.727ms, rate sampling interval: 8142ms
  Thread calibration: mean lat.: 1537.340ms, rate sampling interval: 8085ms
  Thread calibration: mean lat.: 0.684ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 6.195ms, rate sampling interval: 54ms
  Thread calibration: mean lat.: 1627.678ms, rate sampling interval: 8396ms
  Thread calibration: mean lat.: 0.193ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.075ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.715ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1363.523ms, rate sampling interval: 7356ms
  Thread calibration: mean lat.: 0.923ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.430ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.448ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1455.736ms, rate sampling interval: 7516ms
  Thread calibration: mean lat.: 1.143ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.465ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 8.472ms, rate sampling interval: 56ms
  Thread calibration: mean lat.: 1.255ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.761ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1529.158ms, rate sampling interval: 7991ms
  Thread calibration: mean lat.: 0.874ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.950ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.926ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1559.125ms, rate sampling interval: 8298ms
  Thread calibration: mean lat.: 1535.556ms, rate sampling interval: 8146ms
  Thread calibration: mean lat.: 1.292ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1482.725ms, rate sampling interval: 8077ms
  Thread calibration: mean lat.: 1429.515ms, rate sampling interval: 7761ms
  Thread calibration: mean lat.: 1.752ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.930ms, rate sampling interval: 14ms
  Thread calibration: mean lat.: 9.633ms, rate sampling interval: 49ms
  Thread calibration: mean lat.: 1433.968ms, rate sampling interval: 7794ms
  Thread calibration: mean lat.: 1467.258ms, rate sampling interval: 7880ms
  Thread calibration: mean lat.: 2.069ms, rate sampling interval: 13ms
  Thread calibration: mean lat.: 0.894ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.157ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.057ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.413ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.348ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 9.339ms, rate sampling interval: 42ms
  Thread calibration: mean lat.: 4.481ms, rate sampling interval: 27ms
  Thread calibration: mean lat.: 0.920ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 9.437ms, rate sampling interval: 55ms
  Thread calibration: mean lat.: 1.225ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 3.627ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 5.572ms, rate sampling interval: 29ms
  Thread calibration: mean lat.: 1.231ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.718ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.276ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.801ms, rate sampling interval: 15ms
  Thread calibration: mean lat.: 8.244ms, rate sampling interval: 74ms
  Thread calibration: mean lat.: 0.894ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.282ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.200ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 11.563ms, rate sampling interval: 66ms
  Thread calibration: mean lat.: 0.939ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.608ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 3.364ms, rate sampling interval: 20ms
  Thread calibration: mean lat.: 2.999ms, rate sampling interval: 14ms
  Thread calibration: mean lat.: 1.092ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     8.06s     1.57s   11.92s    64.97%
    Req/Sec     0.29     10.83   553.00     99.92%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    7.86s 
 75.000%    9.25s 
 90.000%   10.39s 
 99.000%   11.24s 
 99.900%   11.81s 
 99.990%   11.92s 
 99.999%   11.93s 
100.000%   11.93s 

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

    4382.719     0.000000            4         1.00
    6176.767     0.100000        11947         1.11
    6787.071     0.200000        23932         1.25
    7147.519     0.300000        35957         1.43
    7475.199     0.400000        47852         1.67
    7864.319     0.500000        59737         2.00
    8052.735     0.550000        65809         2.22
    8265.727     0.600000        71709         2.50
    8486.911     0.650000        77740         2.86
    8830.975     0.700000        83717         3.33
    9248.767     0.750000        89628         4.00
    9478.143     0.775000        92616         4.44
    9715.711     0.800000        95572         5.00
    9945.087     0.825000        98554         5.71
   10108.927     0.850000       101695         6.67
   10239.999     0.875000       104575         8.00
   10305.535     0.887500       106047         8.89
   10387.455     0.900000       107613        10.00
   10452.991     0.912500       109115        11.43
   10502.143     0.925000       110498        13.33
   10567.679     0.937500       112126        16.00
   10600.447     0.943750       112784        17.78
   10657.791     0.950000       113588        20.00
   10715.135     0.956250       114220        22.86
   10797.055     0.962500       115006        26.67
   10878.975     0.968750       115742        32.00
   10919.935     0.971875       116138        35.56
   10952.703     0.975000       116462        40.00
   11001.855     0.978125       116842        45.71
   11051.007     0.981250       117211        53.33
   11108.351     0.984375       117590        64.00
   11141.119     0.985938       117765        71.11
   11173.887     0.987500       117952        80.00
   11206.655     0.989062       118144        91.43
   11255.807     0.990625       118328       106.67
   11386.879     0.992188       118513       128.00
   11517.951     0.992969       118606       142.22
   11608.063     0.993750       118706       160.00
   11657.215     0.994531       118829       182.86
   11689.983     0.995313       118907       213.33
   11730.943     0.996094       118986       256.00
   11755.519     0.996484       119033       284.44
   11771.903     0.996875       119110       320.00
   11780.095     0.997266       119199       365.71
   11780.095     0.997656       119199       426.67
   11788.287     0.998047       119247       512.00
   11788.287     0.998242       119247       568.89
   11804.671     0.998437       119299       640.00
   11804.671     0.998633       119299       731.43
   11812.863     0.998828       119337       853.33
   11812.863     0.999023       119337      1024.00
   11829.247     0.999121       119353      1137.78
   11829.247     0.999219       119353      1280.00
   11837.439     0.999316       119372      1462.86
   11845.631     0.999414       119378      1706.67
   11862.015     0.999512       119388      2048.00
   11878.399     0.999561       119393      2275.56
   11902.975     0.999609       119404      2560.00
   11902.975     0.999658       119404      2925.71
   11911.167     0.999707       119415      3413.33
   11911.167     0.999756       119415      4096.00
   11919.359     0.999780       119437      4551.11
   11919.359     0.999805       119437      5120.00
   11919.359     0.999829       119437      5851.43
   11919.359     0.999854       119437      6826.67
   11919.359     0.999878       119437      8192.00
   11919.359     0.999890       119437      9102.22
   11919.359     0.999902       119437     10240.00
   11919.359     0.999915       119437     11702.86
   11919.359     0.999927       119437     13653.33
   11919.359     0.999939       119437     16384.00
   11927.551     0.999945       119444     18204.44
   11927.551     1.000000       119444          inf
#[Mean    =     8058.049, StdDeviation   =     1572.626]
#[Max     =    11919.360, Total count    =       119444]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  166963 requests in 30.25s, 13.22MB read
  Socket errors: connect 0, read 0, write 0, timeout 674
Requests/sec:   5519.99
Transfer/sec:    447.41KB
```
50k:
```Running 30s test @ http://127.0.0.1:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 0.947ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.660ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 3524.075ms, rate sampling interval: 13328ms
  Thread calibration: mean lat.: 0.946ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 4046.559ms, rate sampling interval: 13918ms
  Thread calibration: mean lat.: 3240.109ms, rate sampling interval: 12730ms
  Thread calibration: mean lat.: 0.679ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.631ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.990ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 3850.832ms, rate sampling interval: 14057ms
  Thread calibration: mean lat.: 3542.178ms, rate sampling interval: 13557ms
  Thread calibration: mean lat.: 0.674ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 3961.060ms, rate sampling interval: 14188ms
  Thread calibration: mean lat.: 0.635ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.617ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 4240.163ms, rate sampling interval: 13983ms
  Thread calibration: mean lat.: 4056.556ms, rate sampling interval: 14065ms
  Thread calibration: mean lat.: 0.636ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.685ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 3896.999ms, rate sampling interval: 14090ms
  Thread calibration: mean lat.: 41.178ms, rate sampling interval: 275ms
  Thread calibration: mean lat.: 3971.253ms, rate sampling interval: 14196ms
  Thread calibration: mean lat.: 4331.559ms, rate sampling interval: 14221ms
  Thread calibration: mean lat.: 0.653ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.628ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 4336.569ms, rate sampling interval: 14254ms
  Thread calibration: mean lat.: 4140.627ms, rate sampling interval: 13991ms
  Thread calibration: mean lat.: 0.692ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 44.223ms, rate sampling interval: 317ms
  Thread calibration: mean lat.: 1.463ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 3568.904ms, rate sampling interval: 13508ms
  Thread calibration: mean lat.: 0.629ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 4055.276ms, rate sampling interval: 14303ms
  Thread calibration: mean lat.: 3842.158ms, rate sampling interval: 14131ms
  Thread calibration: mean lat.: 4012.472ms, rate sampling interval: 14073ms
  Thread calibration: mean lat.: 0.631ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.698ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.648ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.028ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.986ms, rate sampling interval: 14ms
  Thread calibration: mean lat.: 0.610ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.637ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.623ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.685ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 357.581ms, rate sampling interval: 1651ms
  Thread calibration: mean lat.: 0.617ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.613ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.667ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.526ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.801ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 9.271ms, rate sampling interval: 59ms
  Thread calibration: mean lat.: 0.596ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.636ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.599ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.912ms, rate sampling interval: 24ms
  Thread calibration: mean lat.: 0.629ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.610ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.636ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.768ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.700ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.600ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.699ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.676ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 399.119ms, rate sampling interval: 1721ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    13.84s     4.52s   22.40s    56.55%
    Req/Sec     0.09      4.36   234.00     99.96%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%   13.76s 
 75.000%   17.68s 
 90.000%   20.28s 
 99.000%   21.87s 
 99.900%   22.28s 
 99.990%   22.40s 
 99.999%   22.41s 
100.000%   22.41s 

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

    7102.463     0.000000            1         1.00
    7815.167     0.100000         6706         1.11
    9043.967     0.200000        13407         1.25
   10231.807     0.300000        20101         1.43
   11919.359     0.400000        26810         1.67
   13762.559     0.500000        33500         2.00
   14598.143     0.550000        36856         2.22
   15384.575     0.600000        40223         2.50
   16105.471     0.650000        43568         2.86
   16875.519     0.700000        46899         3.33
   17678.335     0.750000        50293         4.00
   18120.703     0.775000        51932         4.44
   18661.375     0.800000        53641         5.00
   19070.975     0.825000        55304         5.71
   19447.807     0.850000        56971         6.67
   19841.023     0.875000        58624         8.00
   20054.015     0.887500        59473         8.89
   20283.391     0.900000        60322        10.00
   20512.767     0.912500        61192        11.43
   20725.759     0.925000        62022        13.33
   20938.751     0.937500        62849        16.00
   21053.439     0.943750        63293        17.78
   21151.743     0.950000        63709        20.00
   21250.047     0.956250        64079        22.86
   21348.351     0.962500        64514        26.67
   21463.039     0.968750        64948        32.00
   21512.191     0.971875        65126        35.56
   21577.727     0.975000        65372        40.00
   21626.879     0.978125        65559        45.71
   21692.415     0.981250        65814        53.33
   21741.567     0.984375        65980        64.00
   21774.335     0.985938        66080        71.11
   21807.103     0.987500        66185        80.00
   21839.871     0.989062        66267        91.43
   21889.023     0.990625        66386       106.67
   21938.175     0.992188        66503       128.00
   21954.559     0.992969        66540       142.22
   21987.327     0.993750        66622       160.00
   22003.711     0.994531        66666       182.86
   22020.095     0.995313        66708       213.33
   22036.479     0.996094        66735       256.00
   22052.863     0.996484        66766       284.44
   22069.247     0.996875        66789       320.00
   22085.631     0.997266        66817       365.71
   22118.399     0.997656        66847       426.67
   22151.167     0.998047        66868       512.00
   22183.935     0.998242        66881       568.89
   22200.319     0.998437        66893       640.00
   22233.087     0.998633        66908       731.43
   22265.855     0.998828        66922       853.33
   22282.239     0.999023        66931      1024.00
   22298.623     0.999121        66942      1137.78
   22315.007     0.999219        66948      1280.00
   22331.391     0.999316        66959      1462.86
   22331.391     0.999414        66959      1706.67
   22347.775     0.999512        66965      2048.00
   22364.159     0.999561        66977      2275.56
   22364.159     0.999609        66977      2560.00
   22364.159     0.999658        66977      2925.71
   22364.159     0.999707        66977      3413.33
   22380.543     0.999756        66988      4096.00
   22380.543     0.999780        66988      4551.11
   22380.543     0.999805        66988      5120.00
   22380.543     0.999829        66988      5851.43
   22380.543     0.999854        66988      6826.67
   22380.543     0.999878        66988      8192.00
   22396.927     0.999890        66994      9102.22
   22396.927     0.999902        66994     10240.00
   22396.927     0.999915        66994     11702.86
   22396.927     0.999927        66994     13653.33
   22396.927     0.999939        66994     16384.00
   22396.927     0.999945        66994     18204.44
   22396.927     0.999951        66994     20480.00
   22396.927     0.999957        66994     23405.71
   22396.927     0.999963        66994     27306.67
   22396.927     0.999969        66994     32768.00
   22413.311     0.999973        66996     36408.89
   22413.311     1.000000        66996          inf
#[Mean    =    13836.372, StdDeviation   =     4521.325]
#[Max     =    22396.928, Total count    =        66996]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  105810 requests in 30.25s, 8.37MB read
  Socket errors: connect 0, read 0, write 0, timeout 668
Requests/sec:   3497.41
Transfer/sec:    283.46KB
```

80k:
```Running 30s test @ http://127.0.0.1:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 1245.159ms, rate sampling interval: 3952ms
  Thread calibration: mean lat.: 4052.740ms, rate sampling interval: 13713ms
  Thread calibration: mean lat.: 1151.842ms, rate sampling interval: 3997ms
  Thread calibration: mean lat.: 4304.559ms, rate sampling interval: 14548ms
  Thread calibration: mean lat.: 3107.763ms, rate sampling interval: 9723ms
  Thread calibration: mean lat.: 4187.883ms, rate sampling interval: 14540ms
  Thread calibration: mean lat.: 4167.920ms, rate sampling interval: 14499ms
  Thread calibration: mean lat.: 4240.353ms, rate sampling interval: 14942ms
  Thread calibration: mean lat.: 4059.526ms, rate sampling interval: 13967ms
  Thread calibration: mean lat.: 4235.565ms, rate sampling interval: 14712ms
  Thread calibration: mean lat.: 4326.126ms, rate sampling interval: 14491ms
  Thread calibration: mean lat.: 1104.256ms, rate sampling interval: 3942ms
  Thread calibration: mean lat.: 4453.266ms, rate sampling interval: 15040ms
  Thread calibration: mean lat.: 4309.453ms, rate sampling interval: 14548ms
  Thread calibration: mean lat.: 1202.818ms, rate sampling interval: 4046ms
  Thread calibration: mean lat.: 1221.547ms, rate sampling interval: 4110ms
  Thread calibration: mean lat.: 1170.027ms, rate sampling interval: 4071ms
  Thread calibration: mean lat.: 1218.211ms, rate sampling interval: 4102ms
  Thread calibration: mean lat.: 4323.544ms, rate sampling interval: 14966ms
  Thread calibration: mean lat.: 4313.907ms, rate sampling interval: 15007ms
  Thread calibration: mean lat.: 1208.196ms, rate sampling interval: 3930ms
  Thread calibration: mean lat.: 1085.290ms, rate sampling interval: 3803ms
  Thread calibration: mean lat.: 1213.372ms, rate sampling interval: 3928ms
  Thread calibration: mean lat.: 1310.301ms, rate sampling interval: 4163ms
  Thread calibration: mean lat.: 1204.937ms, rate sampling interval: 4179ms
  Thread calibration: mean lat.: 4297.438ms, rate sampling interval: 14958ms
  Thread calibration: mean lat.: 2977.043ms, rate sampling interval: 9797ms
  Thread calibration: mean lat.: 4210.092ms, rate sampling interval: 13983ms
  Thread calibration: mean lat.: 1293.285ms, rate sampling interval: 4139ms
  Thread calibration: mean lat.: 1177.995ms, rate sampling interval: 3952ms
  Thread calibration: mean lat.: 4107.106ms, rate sampling interval: 13787ms
  Thread calibration: mean lat.: 1280.517ms, rate sampling interval: 4112ms
  Thread calibration: mean lat.: 1191.115ms, rate sampling interval: 3889ms
  Thread calibration: mean lat.: 1174.206ms, rate sampling interval: 3936ms
  Thread calibration: mean lat.: 1176.394ms, rate sampling interval: 4030ms
  Thread calibration: mean lat.: 1141.535ms, rate sampling interval: 3880ms
  Thread calibration: mean lat.: 1097.826ms, rate sampling interval: 3807ms
  Thread calibration: mean lat.: 1217.664ms, rate sampling interval: 3958ms
  Thread calibration: mean lat.: 1211.898ms, rate sampling interval: 4087ms
  Thread calibration: mean lat.: 1304.240ms, rate sampling interval: 4186ms
  Thread calibration: mean lat.: 1155.805ms, rate sampling interval: 3874ms
  Thread calibration: mean lat.: 1235.222ms, rate sampling interval: 4098ms
  Thread calibration: mean lat.: 1100.982ms, rate sampling interval: 3790ms
  Thread calibration: mean lat.: 1222.264ms, rate sampling interval: 4179ms
  Thread calibration: mean lat.: 1237.729ms, rate sampling interval: 4130ms
  Thread calibration: mean lat.: 1094.140ms, rate sampling interval: 3878ms
  Thread calibration: mean lat.: 1181.063ms, rate sampling interval: 3997ms
  Thread calibration: mean lat.: 1181.838ms, rate sampling interval: 3921ms
  Thread calibration: mean lat.: 1190.677ms, rate sampling interval: 3928ms
  Thread calibration: mean lat.: 4409.553ms, rate sampling interval: 14761ms
  Thread calibration: mean lat.: 1234.346ms, rate sampling interval: 4231ms
  Thread calibration: mean lat.: 1232.667ms, rate sampling interval: 3966ms
  Thread calibration: mean lat.: 1159.708ms, rate sampling interval: 3889ms
  Thread calibration: mean lat.: 1196.716ms, rate sampling interval: 4024ms
  Thread calibration: mean lat.: 1208.380ms, rate sampling interval: 4022ms
  Thread calibration: mean lat.: 1255.172ms, rate sampling interval: 4171ms
  Thread calibration: mean lat.: 1175.308ms, rate sampling interval: 4007ms
  Thread calibration: mean lat.: 1186.604ms, rate sampling interval: 3971ms
  Thread calibration: mean lat.: 1222.330ms, rate sampling interval: 3979ms
  Thread calibration: mean lat.: 2870.467ms, rate sampling interval: 9674ms
  Thread calibration: mean lat.: 1084.567ms, rate sampling interval: 3801ms
  Thread calibration: mean lat.: 1232.356ms, rate sampling interval: 4073ms
  Thread calibration: mean lat.: 1199.668ms, rate sampling interval: 3989ms
  Thread calibration: mean lat.: 1141.378ms, rate sampling interval: 3948ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    16.55s     4.60s   24.90s    56.88%
    Req/Sec    15.84     57.77   266.00     92.95%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%   16.76s 
 75.000%   20.58s 
 90.000%   22.71s 
 99.000%   24.36s 
 99.900%   24.76s 
 99.990%   24.89s 
 99.999%   24.92s 
100.000%   24.92s 

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

    7946.239     0.000000            1         1.00
   10313.727     0.100000         7369         1.11
   11657.215     0.200000        14681         1.25
   13172.735     0.300000        22007         1.43
   14966.783     0.400000        29371         1.67
   16760.831     0.500000        36690         2.00
   17678.335     0.550000        40389         2.22
   18448.383     0.600000        44081         2.50
   19152.895     0.650000        47746         2.86
   19857.407     0.700000        51379         3.33
   20578.303     0.750000        55069         4.00
   20938.751     0.775000        56918         4.44
   21282.815     0.800000        58710         5.00
   21643.263     0.825000        60561         5.71
   21970.943     0.850000        62388         6.67
   22331.391     0.875000        64213         8.00
   22527.999     0.887500        65167         8.89
   22708.223     0.900000        66095        10.00
   22888.447     0.912500        66978        11.43
   23101.439     0.925000        67909        13.33
   23281.663     0.937500        68824        16.00
   23396.351     0.943750        69271        17.78
   23494.655     0.950000        69714        20.00
   23609.343     0.956250        70154        22.86
   23724.031     0.962500        70620        26.67
   23838.719     0.968750        71070        32.00
   23904.255     0.971875        71312        35.56
   23986.175     0.975000        71566        40.00
   24035.327     0.978125        71754        45.71
   24117.247     0.981250        72022        53.33
   24182.783     0.984375        72227        64.00
   24231.935     0.985938        72336        71.11
   24281.087     0.987500        72459        80.00
   24313.855     0.989062        72551        91.43
   24379.391     0.990625        72685       106.67
   24428.543     0.992188        72780       128.00
   24461.311     0.992969        72841       142.22
   24494.079     0.993750        72906       160.00
   24526.847     0.994531        72951       182.86
   24559.615     0.995313        73008       213.33
   24592.383     0.996094        73071       256.00
   24608.767     0.996484        73104       284.44
   24625.151     0.996875        73129       320.00
   24641.535     0.997266        73154       365.71
   24657.919     0.997656        73187       426.67
   24690.687     0.998047        73226       512.00
   24690.687     0.998242        73226       568.89
   24707.071     0.998437        73240       640.00
   24723.455     0.998633        73253       731.43
   24739.839     0.998828        73273       853.33
   24756.223     0.999023        73288      1024.00
   24756.223     0.999121        73288      1137.78
   24772.607     0.999219        73298      1280.00
   24788.991     0.999316        73308      1462.86
   24788.991     0.999414        73308      1706.67
   24805.375     0.999512        73317      2048.00
   24821.759     0.999561        73327      2275.56
   24821.759     0.999609        73327      2560.00
   24821.759     0.999658        73327      2925.71
   24838.143     0.999707        73337      3413.33
   24838.143     0.999756        73337      4096.00
   24838.143     0.999780        73337      4551.11
   24838.143     0.999805        73337      5120.00
   24854.527     0.999829        73340      5851.43
   24854.527     0.999854        73340      6826.67
   24870.911     0.999878        73342      8192.00
   24870.911     0.999890        73342      9102.22
   24887.295     0.999902        73344     10240.00
   24887.295     0.999915        73344     11702.86
   24903.679     0.999927        73346     13653.33
   24903.679     0.999939        73346     16384.00
   24903.679     0.999945        73346     18204.44
   24920.063     0.999951        73350     20480.00
   24920.063     1.000000        73350          inf
#[Mean    =    16553.619, StdDeviation   =     4595.906]
#[Max     =    24903.680, Total count    =        73350]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  136481 requests in 30.18s, 10.70MB read
  Socket errors: connect 0, read 0, write 0, timeout 618
Requests/sec:   4522.23
Transfer/sec:    363.14KB
```
Putting CPU:
profiler.sh -d 30 -f cpu.svg ___
![Cpu_svg](https://github.com/s3ponia/2020-highload-dht/blob/master/test-results/task4/cpu.svg)


profiler.sh -t -d 30 -f cpu.svg ___
![Cpu_svg](https://github.com/s3ponia/2020-highload-dht/blob/master/test-results/task4/cpu-theads.svg)


profiler.sh -e alloc -t -d 30 -f get-cpu.svg ___

![](https://github.com/s3ponia/2020-highload-dht/blob/master/test-results/task4/put-alloc-cpu.svg)

profiler.sh -e lock -t -d 30 -f get-cpu.svg ___

![](https://github.com/s3ponia/2020-highload-dht/blob/master/test-results/task4/lock-put-cpu.svg)

# GET
wrk2 -t64 -c64 -d30s -R* -s test-results/lua/wrk2-scripts/put-request.lua --latency\\
10k:
```Running 1m test @ http://127.0.0.1:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 17.448ms, rate sampling interval: 113ms
  Thread calibration: mean lat.: 18.165ms, rate sampling interval: 115ms
  Thread calibration: mean lat.: 41.957ms, rate sampling interval: 358ms
  Thread calibration: mean lat.: 86.135ms, rate sampling interval: 580ms
  Thread calibration: mean lat.: 34.944ms, rate sampling interval: 274ms
  Thread calibration: mean lat.: 7.730ms, rate sampling interval: 35ms
  Thread calibration: mean lat.: 18.291ms, rate sampling interval: 119ms
  Thread calibration: mean lat.: 34.718ms, rate sampling interval: 288ms
  Thread calibration: mean lat.: 6.370ms, rate sampling interval: 26ms
  Thread calibration: mean lat.: 6.511ms, rate sampling interval: 24ms
  Thread calibration: mean lat.: 7.559ms, rate sampling interval: 34ms
  Thread calibration: mean lat.: 68.150ms, rate sampling interval: 489ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 6.964ms, rate sampling interval: 26ms
  Thread calibration: mean lat.: 4.236ms, rate sampling interval: 14ms
  Thread calibration: mean lat.: 63.810ms, rate sampling interval: 461ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 4.359ms, rate sampling interval: 15ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 63.910ms, rate sampling interval: 405ms
  Thread calibration: mean lat.: 4.671ms, rate sampling interval: 18ms
  Thread calibration: mean lat.: 6.694ms, rate sampling interval: 27ms
  Thread calibration: mean lat.: 4.060ms, rate sampling interval: 16ms
  Thread calibration: mean lat.: 4.819ms, rate sampling interval: 17ms
  Thread calibration: mean lat.: 4.401ms, rate sampling interval: 18ms
  Thread calibration: mean lat.: 6.100ms, rate sampling interval: 28ms
  Thread calibration: mean lat.: 45.552ms, rate sampling interval: 91ms
  Thread calibration: mean lat.: 41.182ms, rate sampling interval: 93ms
  Thread calibration: mean lat.: 34.139ms, rate sampling interval: 73ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 6.659ms, rate sampling interval: 21ms
  Thread calibration: mean lat.: 5.498ms, rate sampling interval: 24ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 7.151ms, rate sampling interval: 24ms
  Thread calibration: mean lat.: 118.658ms, rate sampling interval: 553ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 5.137ms, rate sampling interval: 20ms
  Thread calibration: mean lat.: 11.731ms, rate sampling interval: 73ms
  Thread calibration: mean lat.: 51.683ms, rate sampling interval: 333ms
  Thread calibration: mean lat.: 8.826ms, rate sampling interval: 35ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 4.442ms, rate sampling interval: 16ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 5.542ms, rate sampling interval: 21ms
  Thread calibration: mean lat.: 14.284ms, rate sampling interval: 86ms
  Thread calibration: mean lat.: 45.394ms, rate sampling interval: 276ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 9.206ms, rate sampling interval: 26ms
  Thread calibration: mean lat.: 22.497ms, rate sampling interval: 57ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 18.484ms, rate sampling interval: 54ms
  Thread calibration: mean lat.: 9223372036854776.000ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.658ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 10.063ms, rate sampling interval: 44ms
  Thread calibration: mean lat.: 18.238ms, rate sampling interval: 120ms
  Thread calibration: mean lat.: 5.986ms, rate sampling interval: 14ms
  Thread calibration: mean lat.: 4.805ms, rate sampling interval: 16ms
  Thread calibration: mean lat.: 4.261ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 14.029ms, rate sampling interval: 79ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.07ms    2.52ms  46.08ms   91.46%
    Req/Sec     7.42     33.27   232.00     95.26%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.33ms
 75.000%    2.12ms
 90.000%    4.25ms
 99.000%   11.73ms
 99.900%   31.66ms
 99.990%   40.74ms
 99.999%   46.01ms
100.000%   46.11ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.382     0.000000            1         1.00
       0.717     0.100000        12549         1.11
       0.896     0.200000        25076         1.25
       1.045     0.300000        37534         1.43
       1.196     0.400000        50040         1.67
       1.334     0.500000        62499         2.00
       1.398     0.550000        68786         2.22
       1.462     0.600000        74985         2.50
       1.603     0.650000        81220         2.86
       1.802     0.700000        87476         3.33
       2.115     0.750000        93730         4.00
       2.317     0.775000        96851         4.44
       2.577     0.800000        99957         5.00
       2.915     0.825000       103090         5.71
       3.291     0.850000       106201         6.67
       3.731     0.875000       109325         8.00
       3.975     0.887500       110883         8.89
       4.251     0.900000       112459        10.00
       4.535     0.912500       114024        11.43
       4.887     0.925000       115568        13.33
       5.299     0.937500       117154        16.00
       5.519     0.943750       117919        17.78
       5.771     0.950000       118696        20.00
       6.087     0.956250       119478        22.86
       6.459     0.962500       120258        26.67
       6.931     0.968750       121034        32.00
       7.247     0.971875       121428        35.56
       7.579     0.975000       121817        40.00
       7.951     0.978125       122208        45.71
       8.495     0.981250       122597        53.33
       9.343     0.984375       122986        64.00
       9.799     0.985938       123185        71.11
      10.415     0.987500       123378        80.00
      11.215     0.989062       123576        91.43
      12.175     0.990625       123767       106.67
      13.711     0.992188       123963       128.00
      14.583     0.992969       124060       142.22
      15.527     0.993750       124158       160.00
      16.671     0.994531       124255       182.86
      18.223     0.995313       124353       213.33
      20.127     0.996094       124450       256.00
      20.927     0.996484       124499       284.44
      21.919     0.996875       124548       320.00
      23.311     0.997266       124598       365.71
      24.767     0.997656       124646       426.67
      26.335     0.998047       124694       512.00
      26.895     0.998242       124720       568.89
      28.383     0.998437       124744       640.00
      29.631     0.998633       124768       731.43
      30.767     0.998828       124792       853.33
      31.775     0.999023       124816      1024.00
      32.303     0.999121       124829      1137.78
      33.279     0.999219       124841      1280.00
      34.111     0.999316       124854      1462.86
      34.719     0.999414       124865      1706.67
      35.807     0.999512       124877      2048.00
      36.415     0.999561       124884      2275.56
      37.055     0.999609       124890      2560.00
      37.375     0.999658       124896      2925.71
      37.823     0.999707       124902      3413.33
      38.175     0.999756       124909      4096.00
      38.335     0.999780       124911      4551.11
      38.463     0.999805       124914      5120.00
      38.783     0.999829       124917      5851.43
      39.295     0.999854       124920      6826.67
      40.191     0.999878       124923      8192.00
      40.479     0.999890       124925      9102.22
      40.735     0.999902       124926     10240.00
      41.535     0.999915       124928     11702.86
      41.791     0.999927       124929     13653.33
      42.015     0.999939       124931     16384.00
      42.975     0.999945       124932     18204.44
      42.975     0.999951       124932     20480.00
      43.263     0.999957       124933     23405.71
      44.063     0.999963       124934     27306.67
      44.127     0.999969       124935     32768.00
      44.127     0.999973       124935     36408.89
      44.127     0.999976       124935     40960.00
      44.511     0.999979       124936     46811.43
      44.511     0.999982       124936     54613.33
      46.015     0.999985       124937     65536.00
      46.015     0.999986       124937     72817.78
      46.015     0.999988       124937     81920.00
      46.015     0.999989       124937     93622.86
      46.015     0.999991       124937    109226.67
      46.111     0.999992       124938    131072.00
      46.111     1.000000       124938          inf
#[Mean    =        2.070, StdDeviation   =        2.519]
#[Max     =       46.080, Total count    =       124938]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  150781 requests in 1.01m, 12.37MB read
  Socket errors: connect 0, read 0, write 0, timeout 1408
  Non-2xx or 3xx responses: 150781
Requests/sec:   2499.78
Transfer/sec:    210.05KB```

30k:
```Running 1m test @ http://127.0.0.1:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 1658.408ms, rate sampling interval: 6397ms
  Thread calibration: mean lat.: 1470.076ms, rate sampling interval: 6737ms
  Thread calibration: mean lat.: 12.208ms, rate sampling interval: 39ms
  Thread calibration: mean lat.: 3.848ms, rate sampling interval: 23ms
  Thread calibration: mean lat.: 1851.702ms, rate sampling interval: 8544ms
  Thread calibration: mean lat.: 3.245ms, rate sampling interval: 18ms
  Thread calibration: mean lat.: 1.175ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 8.178ms, rate sampling interval: 57ms
  Thread calibration: mean lat.: 1832.929ms, rate sampling interval: 6946ms
  Thread calibration: mean lat.: 1784.585ms, rate sampling interval: 8445ms
  Thread calibration: mean lat.: 8.952ms, rate sampling interval: 51ms
  Thread calibration: mean lat.: 1607.109ms, rate sampling interval: 6197ms
  Thread calibration: mean lat.: 1464.646ms, rate sampling interval: 7565ms
  Thread calibration: mean lat.: 1687.137ms, rate sampling interval: 6180ms
  Thread calibration: mean lat.: 5.195ms, rate sampling interval: 16ms
  Thread calibration: mean lat.: 16.510ms, rate sampling interval: 78ms
  Thread calibration: mean lat.: 28.129ms, rate sampling interval: 181ms
  Thread calibration: mean lat.: 1265.387ms, rate sampling interval: 5246ms
  Thread calibration: mean lat.: 1543.236ms, rate sampling interval: 6520ms
  Thread calibration: mean lat.: 15.129ms, rate sampling interval: 51ms
  Thread calibration: mean lat.: 3.765ms, rate sampling interval: 13ms
  Thread calibration: mean lat.: 14.326ms, rate sampling interval: 97ms
  Thread calibration: mean lat.: 1886.023ms, rate sampling interval: 8773ms
  Thread calibration: mean lat.: 7.388ms, rate sampling interval: 54ms
  Thread calibration: mean lat.: 21.687ms, rate sampling interval: 93ms
  Thread calibration: mean lat.: 4.117ms, rate sampling interval: 25ms
  Thread calibration: mean lat.: 53.546ms, rate sampling interval: 153ms
  Thread calibration: mean lat.: 1569.257ms, rate sampling interval: 7667ms
  Thread calibration: mean lat.: 1729.764ms, rate sampling interval: 6443ms
  Thread calibration: mean lat.: 4.181ms, rate sampling interval: 24ms
  Thread calibration: mean lat.: 18.943ms, rate sampling interval: 58ms
  Thread calibration: mean lat.: 1.855ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 9.443ms, rate sampling interval: 46ms
  Thread calibration: mean lat.: 72.138ms, rate sampling interval: 354ms
  Thread calibration: mean lat.: 1617.243ms, rate sampling interval: 6717ms
  Thread calibration: mean lat.: 29.484ms, rate sampling interval: 107ms
  Thread calibration: mean lat.: 2.746ms, rate sampling interval: 11ms
  Thread calibration: mean lat.: 33.404ms, rate sampling interval: 123ms
  Thread calibration: mean lat.: 51.994ms, rate sampling interval: 239ms
  Thread calibration: mean lat.: 1791.626ms, rate sampling interval: 8486ms
  Thread calibration: mean lat.: 1.994ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 16.113ms, rate sampling interval: 60ms
  Thread calibration: mean lat.: 50.675ms, rate sampling interval: 357ms
  Thread calibration: mean lat.: 72.587ms, rate sampling interval: 171ms
  Thread calibration: mean lat.: 5.377ms, rate sampling interval: 16ms
  Thread calibration: mean lat.: 3.530ms, rate sampling interval: 18ms
  Thread calibration: mean lat.: 38.510ms, rate sampling interval: 120ms
  Thread calibration: mean lat.: 47.978ms, rate sampling interval: 159ms
  Thread calibration: mean lat.: 5.168ms, rate sampling interval: 27ms
  Thread calibration: mean lat.: 21.738ms, rate sampling interval: 88ms
  Thread calibration: mean lat.: 21.339ms, rate sampling interval: 82ms
  Thread calibration: mean lat.: 63.768ms, rate sampling interval: 331ms
  Thread calibration: mean lat.: 5.986ms, rate sampling interval: 43ms
  Thread calibration: mean lat.: 24.623ms, rate sampling interval: 76ms
  Thread calibration: mean lat.: 47.306ms, rate sampling interval: 293ms
  Thread calibration: mean lat.: 1.451ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 30.150ms, rate sampling interval: 95ms
  Thread calibration: mean lat.: 6.394ms, rate sampling interval: 32ms
  Thread calibration: mean lat.: 12.792ms, rate sampling interval: 39ms
  Thread calibration: mean lat.: 22.417ms, rate sampling interval: 76ms
  Thread calibration: mean lat.: 2456.585ms, rate sampling interval: 8757ms
  Thread calibration: mean lat.: 23.243ms, rate sampling interval: 77ms
  Thread calibration: mean lat.: 15.255ms, rate sampling interval: 55ms
  Thread calibration: mean lat.: 32.887ms, rate sampling interval: 87ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    14.82s     5.62s   26.48s    61.53%
    Req/Sec     0.61     13.28   390.00     99.78%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%   15.02s 
 75.000%   19.19s 
 90.000%   22.20s 
 99.000%   25.21s 
 99.900%   26.35s 
 99.990%   26.48s 
 99.999%   26.49s 
100.000%   26.49s 

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

    3198.975     0.000000            6         1.00
    6397.951     0.100000        22778         1.11
    9150.463     0.200000        45525         1.25
   12263.423     0.300000        68269         1.43
   13721.599     0.400000        91059         1.67
   15024.127     0.500000       113851         2.00
   15835.135     0.550000       125179         2.22
   16564.223     0.600000       136584         2.50
   17317.887     0.650000       148094         2.86
   18399.231     0.700000       159280         3.33
   19185.663     0.750000       170793         4.00
   19546.111     0.775000       176459         4.44
   20086.783     0.800000       182115         5.00
   20627.455     0.825000       187692         5.71
   21266.431     0.850000       193379         6.67
   21757.951     0.875000       199166         8.00
   21970.943     0.887500       201959         8.89
   22200.319     0.900000       204874        10.00
   22478.847     0.912500       207657        11.43
   22806.527     0.925000       210450        13.33
   23281.663     0.937500       213304        16.00
   23494.655     0.943750       214772        17.78
   23724.031     0.950000       216207        20.00
   23920.639     0.956250       217779        22.86
   24051.711     0.962500       218987        26.67
   24150.015     0.968750       220426        32.00
   24215.551     0.971875       221307        35.56
   24264.703     0.975000       221981        40.00
   24346.623     0.978125       222527        45.71
   24428.543     0.981250       223241        53.33
   24526.847     0.984375       223944        64.00
   24674.303     0.985938       224341        71.11
   24887.295     0.987500       224677        80.00
   25083.903     0.989062       225030        91.43
   25313.279     0.990625       225376       106.67
   25460.735     0.992188       225734       128.00
   25575.423     0.992969       225944       142.22
   25640.959     0.993750       226101       160.00
   25722.879     0.994531       226288       182.86
   25788.415     0.995313       226470       213.33
   25886.719     0.996094       226609       256.00
   25968.639     0.996484       226711       284.44
   26066.943     0.996875       226817       320.00
   26099.711     0.997266       226882       365.71
   26116.095     0.997656       226976       426.67
   26181.631     0.998047       227059       512.00
   26214.399     0.998242       227104       568.89
   26230.783     0.998437       227144       640.00
   26279.935     0.998633       227205       731.43
   26296.319     0.998828       227245       853.33
   26345.471     0.999023       227288      1024.00
   26361.855     0.999121       227310      1137.78
   26378.239     0.999219       227328      1280.00
   26411.007     0.999316       227343      1462.86
   26460.159     0.999414       227441      1706.67
   26460.159     0.999512       227441      2048.00
   26460.159     0.999561       227441      2275.56
   26460.159     0.999609       227441      2560.00
   26460.159     0.999658       227441      2925.71
   26460.159     0.999707       227441      3413.33
   26476.543     0.999756       227487      4096.00
   26476.543     0.999780       227487      4551.11
   26476.543     0.999805       227487      5120.00
   26476.543     0.999829       227487      5851.43
   26476.543     0.999854       227487      6826.67
   26476.543     0.999878       227487      8192.00
   26476.543     0.999890       227487      9102.22
   26476.543     0.999902       227487     10240.00
   26476.543     0.999915       227487     11702.86
   26476.543     0.999927       227487     13653.33
   26476.543     0.999939       227487     16384.00
   26476.543     0.999945       227487     18204.44
   26476.543     0.999951       227487     20480.00
   26492.927     0.999957       227497     23405.71
   26492.927     1.000000       227497          inf
#[Mean    =    14820.637, StdDeviation   =     5623.292]
#[Max     =    26476.544, Total count    =       227497]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  277107 requests in 1.01m, 23.02MB read
  Socket errors: connect 0, read 0, write 0, timeout 1392
  Non-2xx or 3xx responses: 277107
Requests/sec:   4588.97
Transfer/sec:    390.33KB
```

50k:
```Running 1m test @ http://127.0.0.1:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 102.286ms, rate sampling interval: 448ms
  Thread calibration: mean lat.: 198.214ms, rate sampling interval: 1079ms
  Thread calibration: mean lat.: 3245.355ms, rate sampling interval: 12214ms
  Thread calibration: mean lat.: 3289.318ms, rate sampling interval: 12328ms
  Thread calibration: mean lat.: 132.258ms, rate sampling interval: 599ms
  Thread calibration: mean lat.: 159.392ms, rate sampling interval: 526ms
  Thread calibration: mean lat.: 3236.086ms, rate sampling interval: 12156ms
  Thread calibration: mean lat.: 3457.430ms, rate sampling interval: 12189ms
  Thread calibration: mean lat.: 3344.001ms, rate sampling interval: 12140ms
  Thread calibration: mean lat.: 3248.896ms, rate sampling interval: 12255ms
  Thread calibration: mean lat.: 3531.219ms, rate sampling interval: 12984ms
  Thread calibration: mean lat.: 133.870ms, rate sampling interval: 491ms
  Thread calibration: mean lat.: 3129.463ms, rate sampling interval: 12165ms
  Thread calibration: mean lat.: 121.399ms, rate sampling interval: 505ms
  Thread calibration: mean lat.: 135.062ms, rate sampling interval: 495ms
  Thread calibration: mean lat.: 124.634ms, rate sampling interval: 505ms
  Thread calibration: mean lat.: 3720.955ms, rate sampling interval: 12574ms
  Thread calibration: mean lat.: 3756.237ms, rate sampling interval: 12378ms
  Thread calibration: mean lat.: 174.553ms, rate sampling interval: 715ms
  Thread calibration: mean lat.: 129.311ms, rate sampling interval: 496ms
  Thread calibration: mean lat.: 3558.959ms, rate sampling interval: 13131ms
  Thread calibration: mean lat.: 142.338ms, rate sampling interval: 427ms
  Thread calibration: mean lat.: 177.600ms, rate sampling interval: 553ms
  Thread calibration: mean lat.: 179.337ms, rate sampling interval: 649ms
  Thread calibration: mean lat.: 182.247ms, rate sampling interval: 666ms
  Thread calibration: mean lat.: 146.275ms, rate sampling interval: 502ms
  Thread calibration: mean lat.: 145.971ms, rate sampling interval: 547ms
  Thread calibration: mean lat.: 3640.694ms, rate sampling interval: 13017ms
  Thread calibration: mean lat.: 158.628ms, rate sampling interval: 602ms
  Thread calibration: mean lat.: 3369.009ms, rate sampling interval: 12156ms
  Thread calibration: mean lat.: 110.720ms, rate sampling interval: 428ms
  Thread calibration: mean lat.: 3384.427ms, rate sampling interval: 12132ms
  Thread calibration: mean lat.: 128.707ms, rate sampling interval: 475ms
  Thread calibration: mean lat.: 173.798ms, rate sampling interval: 654ms
  Thread calibration: mean lat.: 3535.688ms, rate sampling interval: 12550ms
  Thread calibration: mean lat.: 140.767ms, rate sampling interval: 521ms
  Thread calibration: mean lat.: 157.091ms, rate sampling interval: 579ms
  Thread calibration: mean lat.: 155.958ms, rate sampling interval: 385ms
  Thread calibration: mean lat.: 111.642ms, rate sampling interval: 441ms
  Thread calibration: mean lat.: 186.516ms, rate sampling interval: 610ms
  Thread calibration: mean lat.: 204.602ms, rate sampling interval: 686ms
  Thread calibration: mean lat.: 177.483ms, rate sampling interval: 592ms
  Thread calibration: mean lat.: 204.123ms, rate sampling interval: 1013ms
  Thread calibration: mean lat.: 111.838ms, rate sampling interval: 448ms
  Thread calibration: mean lat.: 122.984ms, rate sampling interval: 418ms
  Thread calibration: mean lat.: 122.331ms, rate sampling interval: 457ms
  Thread calibration: mean lat.: 141.776ms, rate sampling interval: 523ms
  Thread calibration: mean lat.: 195.619ms, rate sampling interval: 904ms
  Thread calibration: mean lat.: 170.883ms, rate sampling interval: 611ms
  Thread calibration: mean lat.: 3395.054ms, rate sampling interval: 12361ms
  Thread calibration: mean lat.: 136.110ms, rate sampling interval: 624ms
  Thread calibration: mean lat.: 200.468ms, rate sampling interval: 648ms
  Thread calibration: mean lat.: 190.566ms, rate sampling interval: 686ms
  Thread calibration: mean lat.: 151.762ms, rate sampling interval: 509ms
  Thread calibration: mean lat.: 142.881ms, rate sampling interval: 514ms
  Thread calibration: mean lat.: 189.829ms, rate sampling interval: 677ms
  Thread calibration: mean lat.: 197.611ms, rate sampling interval: 681ms
  Thread calibration: mean lat.: 247.892ms, rate sampling interval: 959ms
  Thread calibration: mean lat.: 178.420ms, rate sampling interval: 671ms
  Thread calibration: mean lat.: 216.020ms, rate sampling interval: 667ms
  Thread calibration: mean lat.: 200.577ms, rate sampling interval: 638ms
  Thread calibration: mean lat.: 201.677ms, rate sampling interval: 691ms
  Thread calibration: mean lat.: 116.763ms, rate sampling interval: 428ms
  Thread calibration: mean lat.: 238.141ms, rate sampling interval: 994ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    24.65s     9.48s   41.29s    56.15%
    Req/Sec     3.78     32.21   376.00     98.60%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%   25.59s 
 75.000%   33.37s 
 90.000%   36.57s 
 99.000%   39.68s 
 99.900%   40.89s 
 99.990%   41.29s 
 99.999%   41.32s 
100.000%   41.32s 

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

    6537.215     0.000000            2         1.00
   10756.095     0.100000        22133         1.11
   14811.135     0.200000        44324         1.25
   17743.871     0.300000        66431         1.43
   22265.855     0.400000        88622         1.67
   25591.807     0.500000       110726         2.00
   26738.687     0.550000       121735         2.22
   28442.623     0.600000       132863         2.50
   30490.623     0.650000       143900         2.86
   32129.023     0.700000       154969         3.33
   33374.207     0.750000       166067         4.00
   34045.951     0.775000       171619         4.44
   34701.311     0.800000       177257         5.00
   35094.527     0.825000       182684         5.71
   35651.583     0.850000       188322         6.67
   36077.567     0.875000       193984         8.00
   36306.943     0.887500       196627         8.89
   36569.087     0.900000       199241        10.00
   36863.999     0.912500       202169        11.43
   37158.911     0.925000       204814        13.33
   37421.055     0.937500       207458        16.00
   37617.663     0.943750       209032        17.78
   37781.503     0.950000       210482        20.00
   38010.879     0.956250       211658        22.86
   38240.255     0.962500       213040        26.67
   38469.631     0.968750       214591        32.00
   38567.935     0.971875       215220        35.56
   38699.007     0.975000       215851        40.00
   38830.079     0.978125       216509        45.71
   39026.687     0.981250       217208        53.33
   39288.831     0.984375       217891        64.00
   39387.135     0.985938       218193        71.11
   39518.207     0.987500       218605        80.00
   39616.511     0.989062       218898        91.43
   39747.583     0.990625       219282       106.67
   39845.887     0.992188       219599       128.00
   39944.191     0.992969       219785       142.22
   40042.495     0.993750       219958       160.00
   40140.799     0.994531       220095       182.86
   40239.103     0.995313       220294       213.33
   40337.407     0.996094       220452       256.00
   40402.943     0.996484       220545       284.44
   40468.479     0.996875       220655       320.00
   40501.247     0.997266       220691       365.71
   40599.551     0.997656       220788       426.67
   40665.087     0.998047       220857       512.00
   40730.623     0.998242       220939       568.89
   40763.391     0.998437       220968       640.00
   40796.159     0.998633       220984       731.43
   40861.695     0.998828       221043       853.33
   40894.463     0.999023       221078      1024.00
   40927.231     0.999121       221101      1137.78
   40959.999     0.999219       221127      1280.00
   40992.767     0.999316       221148      1462.86
   41025.535     0.999414       221169      1706.67
   41058.303     0.999512       221187      2048.00
   41091.071     0.999561       221208      2275.56
   41091.071     0.999609       221208      2560.00
   41123.839     0.999658       221215      2925.71
   41189.375     0.999707       221229      3413.33
   41222.143     0.999756       221251      4096.00
   41222.143     0.999780       221251      4551.11
   41222.143     0.999805       221251      5120.00
   41222.143     0.999829       221251      5851.43
   41254.911     0.999854       221263      6826.67
   41254.911     0.999878       221263      8192.00
   41254.911     0.999890       221263      9102.22
   41287.679     0.999902       221279     10240.00
   41287.679     0.999915       221279     11702.86
   41287.679     0.999927       221279     13653.33
   41287.679     0.999939       221279     16384.00
   41287.679     0.999945       221279     18204.44
   41287.679     0.999951       221279     20480.00
   41287.679     0.999957       221279     23405.71
   41287.679     0.999963       221279     27306.67
   41320.447     0.999969       221286     32768.00
   41320.447     1.000000       221286          inf
#[Mean    =    24646.065, StdDeviation   =     9477.197]
#[Max     =    41287.680, Total count    =       221286]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  278939 requests in 1.01m, 21.91MB read
  Socket errors: connect 0, read 0, write 0, timeout 1392
  Non-2xx or 3xx responses: 278939
Requests/sec:   4620.16
Transfer/sec:    371.58KB
```

80k:
```Running 1m test @ http://127.0.0.1:8080
  64 threads and 64 connections
  Thread calibration: mean lat.: 2575.654ms, rate sampling interval: 10854ms
  Thread calibration: mean lat.: 2616.518ms, rate sampling interval: 11313ms
  Thread calibration: mean lat.: 29.174ms, rate sampling interval: 161ms
  Thread calibration: mean lat.: 50.337ms, rate sampling interval: 200ms
  Thread calibration: mean lat.: 38.767ms, rate sampling interval: 178ms
  Thread calibration: mean lat.: 38.033ms, rate sampling interval: 171ms
  Thread calibration: mean lat.: 2909.121ms, rate sampling interval: 12066ms
  Thread calibration: mean lat.: 3011.348ms, rate sampling interval: 11862ms
  Thread calibration: mean lat.: 57.615ms, rate sampling interval: 207ms
  Thread calibration: mean lat.: 45.372ms, rate sampling interval: 204ms
  Thread calibration: mean lat.: 57.951ms, rate sampling interval: 214ms
  Thread calibration: mean lat.: 57.230ms, rate sampling interval: 221ms
  Thread calibration: mean lat.: 3014.127ms, rate sampling interval: 12140ms
  Thread calibration: mean lat.: 42.910ms, rate sampling interval: 194ms
  Thread calibration: mean lat.: 2888.950ms, rate sampling interval: 11771ms
  Thread calibration: mean lat.: 42.177ms, rate sampling interval: 156ms
  Thread calibration: mean lat.: 55.555ms, rate sampling interval: 212ms
  Thread calibration: mean lat.: 72.433ms, rate sampling interval: 227ms
  Thread calibration: mean lat.: 64.409ms, rate sampling interval: 225ms
  Thread calibration: mean lat.: 3243.364ms, rate sampling interval: 12156ms
  Thread calibration: mean lat.: 58.739ms, rate sampling interval: 204ms
  Thread calibration: mean lat.: 55.294ms, rate sampling interval: 212ms
  Thread calibration: mean lat.: 65.681ms, rate sampling interval: 210ms
  Thread calibration: mean lat.: 2933.003ms, rate sampling interval: 11624ms
  Thread calibration: mean lat.: 13.056ms, rate sampling interval: 92ms
  Thread calibration: mean lat.: 59.580ms, rate sampling interval: 215ms
  Thread calibration: mean lat.: 2770.388ms, rate sampling interval: 11763ms
  Thread calibration: mean lat.: 45.949ms, rate sampling interval: 233ms
  Thread calibration: mean lat.: 2941.543ms, rate sampling interval: 11542ms
  Thread calibration: mean lat.: 63.537ms, rate sampling interval: 226ms
  Thread calibration: mean lat.: 65.738ms, rate sampling interval: 217ms
  Thread calibration: mean lat.: 51.132ms, rate sampling interval: 211ms
  Thread calibration: mean lat.: 2792.850ms, rate sampling interval: 11640ms
  Thread calibration: mean lat.: 56.692ms, rate sampling interval: 194ms
  Thread calibration: mean lat.: 58.428ms, rate sampling interval: 211ms
  Thread calibration: mean lat.: 2909.587ms, rate sampling interval: 11673ms
  Thread calibration: mean lat.: 48.494ms, rate sampling interval: 159ms
  Thread calibration: mean lat.: 43.567ms, rate sampling interval: 187ms
  Thread calibration: mean lat.: 38.750ms, rate sampling interval: 141ms
  Thread calibration: mean lat.: 52.779ms, rate sampling interval: 210ms
  Thread calibration: mean lat.: 68.299ms, rate sampling interval: 225ms
  Thread calibration: mean lat.: 61.367ms, rate sampling interval: 237ms
  Thread calibration: mean lat.: 65.562ms, rate sampling interval: 219ms
  Thread calibration: mean lat.: 58.144ms, rate sampling interval: 204ms
  Thread calibration: mean lat.: 51.914ms, rate sampling interval: 177ms
  Thread calibration: mean lat.: 66.371ms, rate sampling interval: 219ms
  Thread calibration: mean lat.: 41.583ms, rate sampling interval: 176ms
  Thread calibration: mean lat.: 60.348ms, rate sampling interval: 201ms
  Thread calibration: mean lat.: 46.429ms, rate sampling interval: 161ms
  Thread calibration: mean lat.: 69.658ms, rate sampling interval: 256ms
  Thread calibration: mean lat.: 333.317ms, rate sampling interval: 1205ms
  Thread calibration: mean lat.: 2836.886ms, rate sampling interval: 11427ms
  Thread calibration: mean lat.: 68.602ms, rate sampling interval: 216ms
  Thread calibration: mean lat.: 3173.989ms, rate sampling interval: 12632ms
  Thread calibration: mean lat.: 56.936ms, rate sampling interval: 197ms
  Thread calibration: mean lat.: 48.062ms, rate sampling interval: 172ms
  Thread calibration: mean lat.: 46.820ms, rate sampling interval: 166ms
  Thread calibration: mean lat.: 2998.176ms, rate sampling interval: 11902ms
  Thread calibration: mean lat.: 41.739ms, rate sampling interval: 151ms
  Thread calibration: mean lat.: 48.342ms, rate sampling interval: 176ms
  Thread calibration: mean lat.: 47.629ms, rate sampling interval: 170ms
  Thread calibration: mean lat.: 3171.804ms, rate sampling interval: 12484ms
  Thread calibration: mean lat.: 58.294ms, rate sampling interval: 199ms
  Thread calibration: mean lat.: 69.698ms, rate sampling interval: 256ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    28.40s    10.90s   46.86s    59.66%
    Req/Sec     1.52     21.04   383.00     99.47%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%   28.82s 
 75.000%   37.91s 
 90.000%   43.19s 
 99.000%   46.01s 
 99.900%   46.53s 
 99.990%   46.86s 
 99.999%   46.89s 
100.000%   46.89s 

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

    7278.591     0.000000            1         1.00
   12992.511     0.100000        23293         1.11
   17301.503     0.200000        46592         1.25
   21364.735     0.300000        69852         1.43
   24936.447     0.400000        93127         1.67
   28819.455     0.500000       116459         2.00
   31031.295     0.550000       128064         2.22
   33095.679     0.600000       139793         2.50
   34603.007     0.650000       151477         2.86
   36143.103     0.700000       163140         3.33
   37912.575     0.750000       174811         4.00
   38600.703     0.775000       180668         4.44
   39321.599     0.800000       186390         5.00
   40140.799     0.825000       192093         5.71
   41156.607     0.850000       198022         6.67
   42237.951     0.875000       203781         8.00
   42762.239     0.887500       206747         8.89
   43188.223     0.900000       209640        10.00
   43581.439     0.912500       212548        11.43
   43909.119     0.925000       215536        13.33
   44236.799     0.937500       218266        16.00
   44466.175     0.943750       220014        17.78
   44597.247     0.950000       221207        20.00
   44793.855     0.956250       222882        22.86
   44957.695     0.962500       224289        26.67
   45121.535     0.968750       225624        32.00
   45252.607     0.971875       226414        35.56
   45350.911     0.975000       227004        40.00
   45514.751     0.978125       227829        45.71
   45645.823     0.981250       228618        53.33
   45776.895     0.984375       229230        64.00
   45842.431     0.985938       229561        71.11
   45907.967     0.987500       229920        80.00
   45973.503     0.989062       230370        91.43
   46039.039     0.990625       230741       106.67
   46104.575     0.992188       231081       128.00
   46137.343     0.992969       231252       142.22
   46170.111     0.993750       231438       160.00
   46202.879     0.994531       231637       182.86
   46235.647     0.995313       231795       213.33
   46268.415     0.996094       231955       256.00
   46301.183     0.996484       232121       284.44
   46301.183     0.996875       232121       320.00
   46333.951     0.997266       232245       365.71
   46366.719     0.997656       232342       426.67
   46399.487     0.998047       232463       512.00
   46399.487     0.998242       232463       568.89
   46399.487     0.998437       232463       640.00
   46432.255     0.998633       232516       731.43
   46465.023     0.998828       232549       853.33
   46530.559     0.999023       232604      1024.00
   46563.327     0.999121       232645      1137.78
   46563.327     0.999219       232645      1280.00
   46596.095     0.999316       232670      1462.86
   46628.863     0.999414       232682      1706.67
   46661.631     0.999512       232703      2048.00
   46694.399     0.999561       232715      2275.56
   46727.167     0.999609       232730      2560.00
   46759.935     0.999658       232753      2925.71
   46759.935     0.999707       232753      3413.33
   46792.703     0.999756       232771      4096.00
   46792.703     0.999780       232771      4551.11
   46792.703     0.999805       232771      5120.00
   46825.471     0.999829       232786      5851.43
   46825.471     0.999854       232786      6826.67
   46858.239     0.999878       232804      8192.00
   46858.239     0.999890       232804      9102.22
   46858.239     0.999902       232804     10240.00
   46858.239     0.999915       232804     11702.86
   46858.239     0.999927       232804     13653.33
   46858.239     0.999939       232804     16384.00
   46858.239     0.999945       232804     18204.44
   46858.239     0.999951       232804     20480.00
   46891.007     0.999957       232815     23405.71
   46891.007     1.000000       232815          inf
#[Mean    =    28400.673, StdDeviation   =    10902.856]
#[Max     =    46858.240, Total count    =       232815]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  285730 requests in 1.01m, 23.19MB read
  Socket errors: connect 0, read 0, write 0, timeout 1392
  Non-2xx or 3xx responses: 285730
Requests/sec:   4735.45
Transfer/sec:    393.56KB
```

profiler.sh -d 30 -f get-cpu.svg ___
![](https://github.com/s3ponia/2020-highload-dht/blob/master/test-results/task4/get-cpu.svg)


profiler.sh -t -d 30 -f get-cpu.svg ___
![](https://github.com/s3ponia/2020-highload-dht/blob/master/test-results/task4/get-cpu-threaded.svg)

profiler.sh -e alloc -t -d 30 -f get-cpu.svg ___

![](https://github.com/s3ponia/2020-highload-dht/blob/master/test-results/task4/get-alloc-cpu.svg)

profiler.sh -e lock -t -d 30 -f get-cpu.svg ___

![](https://github.com/s3ponia/2020-highload-dht/blob/master/test-results/task4/get-lock-cpu.svg)
