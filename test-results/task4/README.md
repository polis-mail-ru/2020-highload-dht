# PUT
wrk2 -t64 -c64 -d30s -R* -s test-results/lua/wrk2-scripts/put-request.lua --latency\
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
## CPU
![CPU](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/cpu.svg)

## CPU threads
![CPU](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/cpu-theads.svg)


## ALLOC

![ALLOC](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/put-alloc-cpu.svg)

## LOCK:

![LOCK](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/lock-put-cpu.svg)

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

## CPU
![CPU](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/get-cpu.svg)


## CPU threads
![CPU](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/get-cpu-threaded.svg)

## ALLOC

![ALLOC](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/get-alloc-cpu.svg)
## LOCK

![LOCK](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/get-lock-cpu.svg)

# Комментарий
Очень странные результаты. Очень плохое время и куча выбросов в latency. Прогонял на 12 потоках 12 соединениях было все нормально. Система на amd Ryzen 5600 (6 ядер/12 хипертред). Если еще убрать весь прокси и просто крутить на одном кластере только, то тоже все нормально. Видимо проблема в прокси и ошибка в реализации кластеризации, пытался вешать таймауты на прокси, но ничего не меняется. Как не пытался, так и не смог найти где могло бы утечь время.

# Продолжение веселья с половиной таймаутов
Путем длительного просмотра проекта, картинок с профилировкой, решил посмотреть что с локами. В итоге обнаружено прекрасные локи в iterator, upsert, remove, где захватывается writeLock вместо readLock, что и приводило к таким задержкам.

# PUT
wrk2 -t64 -c64 -d30s -R* -s test-results/lua/wrk2-scripts/put-request.lua --latency\
20k:
```Running 1m test @ http://127.0.0.1:8082
  64 threads and 64 connections
  Thread calibration: mean lat.: 0.629ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.626ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.636ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.631ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.625ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.630ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.627ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.631ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.631ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.628ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.621ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.626ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.630ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.623ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.622ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.631ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.633ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.625ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.632ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.628ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.627ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.634ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.629ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.626ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.625ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.631ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.628ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.633ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.633ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.631ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.631ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.627ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.631ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.629ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.627ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.630ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.624ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.621ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.629ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.633ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.632ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.628ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.628ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.624ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.626ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.624ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.633ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.630ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.626ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.625ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.627ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.632ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.633ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.628ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.628ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.636ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.633ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.634ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.637ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.625ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.621ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.627ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.629ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.625ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   647.92us  344.31us  11.27ms   68.24%
    Req/Sec   326.79     40.27   666.00     81.83%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%  641.00us
 75.000%    0.89ms
 90.000%    1.04ms
 99.000%    1.17ms
 99.900%    3.54ms
 99.990%    7.80ms
 99.999%    9.85ms
100.000%   11.28ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.043     0.000000            2         1.00
       0.236     0.100000       100726         1.11
       0.337     0.200000       200644         1.25
       0.438     0.300000       300827         1.43
       0.539     0.400000       400722         1.67
       0.641     0.500000       500892         2.00
       0.691     0.550000       550168         2.22
       0.742     0.600000       600555         2.50
       0.792     0.650000       649917         2.86
       0.843     0.700000       700316         3.33
       0.894     0.750000       750730         4.00
       0.919     0.775000       774973         4.44
       0.945     0.800000       800815         5.00
       0.970     0.825000       825576         5.71
       0.995     0.850000       850547         6.67
       1.020     0.875000       875571         8.00
       1.032     0.887500       887533         8.89
       1.045     0.900000       900505        10.00
       1.057     0.912500       912351        11.43
       1.070     0.925000       925172        13.33
       1.083     0.937500       937989        16.00
       1.089     0.943750       943838        17.78
       1.096     0.950000       950585        20.00
       1.103     0.956250       956774        22.86
       1.110     0.962500       962439        26.67
       1.119     0.968750       969059        32.00
       1.123     0.971875       971853        35.56
       1.128     0.975000       975042        40.00
       1.134     0.978125       978303        45.71
       1.141     0.981250       981336        53.33
       1.149     0.984375       984242        64.00
       1.154     0.985938       985890        71.11
       1.160     0.987500       987531        80.00
       1.166     0.989062       988907        91.43
       1.174     0.990625       990509       106.67
       1.184     0.992188       992127       128.00
       1.191     0.992969       992853       142.22
       1.203     0.993750       993602       160.00
       1.231     0.994531       994355       182.86
       1.314     0.995313       995131       213.33
       1.470     0.996094       995910       256.00
       1.575     0.996484       996304       284.44
       1.717     0.996875       996690       320.00
       1.879     0.997266       997082       365.71
       2.069     0.997656       997473       426.67
       2.311     0.998047       997864       512.00
       2.481     0.998242       998057       568.89
       2.681     0.998437       998253       640.00
       2.903     0.998633       998448       731.43
       3.191     0.998828       998644       853.33
       3.597     0.999023       998839      1024.00
       3.775     0.999121       998936      1137.78
       4.029     0.999219       999033      1280.00
       4.327     0.999316       999133      1462.86
       4.695     0.999414       999229      1706.67
       5.031     0.999512       999326      2048.00
       5.319     0.999561       999377      2275.56
       5.535     0.999609       999424      2560.00
       5.795     0.999658       999475      2925.71
       6.039     0.999707       999522      3413.33
       6.415     0.999756       999570      4096.00
       6.607     0.999780       999595      4551.11
       6.803     0.999805       999619      5120.00
       6.975     0.999829       999645      5851.43
       7.255     0.999854       999669      6826.67
       7.583     0.999878       999693      8192.00
       7.739     0.999890       999705      9102.22
       7.807     0.999902       999717     10240.00
       7.907     0.999915       999730     11702.86
       8.015     0.999927       999741     13653.33
       8.343     0.999939       999753     16384.00
       8.471     0.999945       999760     18204.44
       8.575     0.999951       999767     20480.00
       8.743     0.999957       999772     23405.71
       8.879     0.999963       999778     27306.67
       9.031     0.999969       999784     32768.00
       9.087     0.999973       999787     36408.89
       9.135     0.999976       999790     40960.00
       9.319     0.999979       999794     46811.43
       9.359     0.999982       999796     54613.33
       9.511     0.999985       999799     65536.00
       9.575     0.999986       999801     72817.78
       9.799     0.999988       999802     81920.00
       9.855     0.999989       999804     93622.86
       9.927     0.999991       999805    109226.67
      10.191     0.999992       999807    131072.00
      10.279     0.999993       999808    145635.56
      10.279     0.999994       999808    163840.00
      10.479     0.999995       999809    187245.71
      10.631     0.999995       999810    218453.33
      10.951     0.999996       999811    262144.00
      10.951     0.999997       999811    291271.11
      10.951     0.999997       999811    327680.00
      11.095     0.999997       999812    374491.43
      11.095     0.999998       999812    436906.67
      11.151     0.999998       999813    524288.00
      11.151     0.999998       999813    582542.22
      11.151     0.999998       999813    655360.00
      11.151     0.999999       999813    748982.86
      11.151     0.999999       999813    873813.33
      11.279     0.999999       999814   1048576.00
      11.279     1.000000       999814          inf
#[Mean    =        0.648, StdDeviation   =        0.344]
#[Max     =       11.272, Total count    =       999814]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  1200004 requests in 1.00m, 94.99MB read
Requests/sec:  20001.43
Transfer/sec:      1.58MB
```
50k:
```Running 1m test @ http://127.0.0.1:8082
  64 threads and 64 connections
  Thread calibration: mean lat.: 0.622ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.616ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.622ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.620ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.620ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.621ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.619ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.617ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.619ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.616ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.615ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.618ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.621ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.622ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.617ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.620ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.616ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.619ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.618ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.616ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.616ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.618ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.619ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.620ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.616ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.617ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.625ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.618ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.619ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.619ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.619ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.621ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.616ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.617ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.621ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.619ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.618ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.616ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.622ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.620ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.623ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.618ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.618ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.619ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.618ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.617ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.621ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.617ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.616ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.624ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.620ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.619ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.619ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.614ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.618ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.619ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.618ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.618ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.621ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.621ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.614ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.618ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.618ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.618ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   637.71us  373.56us  18.50ms   73.81%
    Req/Sec   842.35     61.86     2.00k    76.73%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%  625.00us
 75.000%    0.88ms
 90.000%    1.03ms
 99.000%    1.18ms
 99.900%    4.48ms
 99.990%    8.53ms
 99.999%   12.94ms
100.000%   18.51ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.026     0.000000            1         1.00
       0.219     0.100000       251224         1.11
       0.321     0.200000       501324         1.25
       0.422     0.300000       750098         1.43
       0.524     0.400000      1000790         1.67
       0.625     0.500000      1249782         2.00
       0.676     0.550000      1375118         2.22
       0.727     0.600000      1501663         2.50
       0.778     0.650000      1627126         2.86
       0.829     0.700000      1752122         3.33
       0.879     0.750000      1875989         4.00
       0.904     0.775000      1937735         4.44
       0.929     0.800000      1999801         5.00
       0.955     0.825000      2063328         5.71
       0.981     0.850000      2126881         6.67
       1.006     0.875000      2188604         8.00
       1.018     0.887500      2218367         8.89
       1.031     0.900000      2250557        10.00
       1.044     0.912500      2282761        11.43
       1.056     0.925000      2312099        13.33
       1.069     0.937500      2343948        16.00
       1.076     0.943750      2360789        17.78
       1.082     0.950000      2375109        20.00
       1.089     0.956250      2391512        22.86
       1.096     0.962500      2406743        26.67
       1.104     0.968750      2421975        32.00
       1.109     0.971875      2430619        35.56
       1.114     0.975000      2438535        40.00
       1.119     0.978125      2445529        45.71
       1.126     0.981250      2453159        53.33
       1.137     0.984375      2461014        64.00
       1.144     0.985938      2464571        71.11
       1.154     0.987500      2468415        80.00
       1.169     0.989062      2472373        91.43
       1.200     0.990625      2476205       106.67
       1.316     0.992188      2480036       128.00
       1.413     0.992969      2481987       142.22
       1.528     0.993750      2483951       160.00
       1.670     0.994531      2485892       182.86
       1.831     0.995313      2487848       213.33
       2.034     0.996094      2489798       256.00
       2.159     0.996484      2490777       284.44
       2.313     0.996875      2491755       320.00
       2.509     0.997266      2492736       365.71
       2.779     0.997656      2493708       426.67
       3.141     0.998047      2494682       512.00
       3.361     0.998242      2495169       568.89
       3.589     0.998437      2495658       640.00
       3.849     0.998633      2496147       731.43
       4.167     0.998828      2496634       853.33
       4.515     0.999023      2497124      1024.00
       4.723     0.999121      2497374      1137.78
       4.939     0.999219      2497610      1280.00
       5.183     0.999316      2497856      1462.86
       5.411     0.999414      2498099      1706.67
       5.703     0.999512      2498341      2048.00
       5.855     0.999561      2498467      2275.56
       6.039     0.999609      2498587      2560.00
       6.263     0.999658      2498708      2925.71
       6.507     0.999707      2498830      3413.33
       6.779     0.999756      2498951      4096.00
       6.951     0.999780      2499014      4551.11
       7.155     0.999805      2499076      5120.00
       7.387     0.999829      2499134      5851.43
       7.699     0.999854      2499196      6826.67
       8.051     0.999878      2499256      8192.00
       8.319     0.999890      2499288      9102.22
       8.599     0.999902      2499317     10240.00
       8.903     0.999915      2499350     11702.86
       9.247     0.999927      2499379     13653.33
       9.807     0.999939      2499409     16384.00
       9.951     0.999945      2499424     18204.44
      10.287     0.999951      2499439     20480.00
      10.639     0.999957      2499455     23405.71
      10.991     0.999963      2499471     27306.67
      11.367     0.999969      2499486     32768.00
      11.503     0.999973      2499493     36408.89
      11.663     0.999976      2499500     40960.00
      11.927     0.999979      2499508     46811.43
      12.199     0.999982      2499516     54613.33
      12.399     0.999985      2499523     65536.00
      12.479     0.999986      2499527     72817.78
      12.663     0.999988      2499531     81920.00
      12.839     0.999989      2499535     93622.86
      13.135     0.999991      2499539    109226.67
      13.223     0.999992      2499542    131072.00
      13.287     0.999993      2499544    145635.56
      13.463     0.999994      2499546    163840.00
      13.623     0.999995      2499548    187245.71
      13.847     0.999995      2499550    218453.33
      14.303     0.999996      2499552    262144.00
      14.391     0.999997      2499553    291271.11
      14.647     0.999997      2499554    327680.00
      14.791     0.999997      2499555    374491.43
      15.103     0.999998      2499556    436906.67
      15.247     0.999998      2499557    524288.00
      15.247     0.999998      2499557    582542.22
      16.295     0.999998      2499558    655360.00
      16.295     0.999999      2499558    748982.86
      16.463     0.999999      2499559    873813.33
      16.463     0.999999      2499559   1048576.00
      16.463     0.999999      2499559   1165084.44
      17.455     0.999999      2499560   1310720.00
      17.455     0.999999      2499560   1497965.71
      17.455     0.999999      2499560   1747626.67
      17.455     1.000000      2499560   2097152.00
      17.455     1.000000      2499560   2330168.89
      18.511     1.000000      2499561   2621440.00
      18.511     1.000000      2499561          inf
#[Mean    =        0.638, StdDeviation   =        0.374]
#[Max     =       18.496, Total count    =      2499561]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  2999853 requests in 1.00m, 237.46MB read
Requests/sec:  50002.00
Transfer/sec:      3.96MB
```
80k:
```Running 1m test @ http://127.0.0.1:8082
  64 threads and 64 connections
  Thread calibration: mean lat.: 0.934ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.932ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.939ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.935ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.937ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.943ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.943ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.938ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.929ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.940ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.943ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.935ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.944ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.943ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.939ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.938ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.936ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.939ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.937ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.941ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.946ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.932ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.939ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.939ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.941ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.934ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.940ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.938ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.939ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.927ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.940ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.937ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.946ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.941ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.933ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.934ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.934ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.930ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.932ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.936ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.943ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.938ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.940ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.936ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.938ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.944ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.936ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.946ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.942ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.937ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.938ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.938ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.943ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.936ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.934ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.940ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.938ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.939ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.939ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.943ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.938ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.942ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.946ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.933ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.12ms    5.30ms 269.82ms   99.75%
    Req/Sec     1.31k   119.51    14.56k    89.54%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    0.93ms
 75.000%    1.20ms
 90.000%    1.37ms
 99.000%    2.02ms
 99.900%   57.47ms
 99.990%  235.77ms
 99.999%  265.98ms
100.000%  270.08ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.026     0.000000            1         1.00
       0.499     0.100000       400971         1.11
       0.610     0.200000       802390         1.25
       0.717     0.300000      1201543         1.43
       0.823     0.400000      1600980         1.67
       0.931     0.500000      2001399         2.00
       0.985     0.550000      2202908         2.22
       1.038     0.600000      2399874         2.50
       1.092     0.650000      2603317         2.86
       1.147     0.700000      2801668         3.33
       1.204     0.750000      3002211         4.00
       1.232     0.775000      3102111         4.44
       1.260     0.800000      3202869         5.00
       1.287     0.825000      3300636         5.71
       1.315     0.850000      3402430         6.67
       1.342     0.875000      3501351         8.00
       1.356     0.887500      3552588         8.89
       1.369     0.900000      3600436        10.00
       1.383     0.912500      3651484        11.43
       1.397     0.925000      3701677        13.33
       1.412     0.937500      3752535        16.00
       1.419     0.943750      3774582        17.78
       1.428     0.950000      3800265        20.00
       1.438     0.956250      3825291        22.86
       1.450     0.962500      3850464        26.67
       1.465     0.968750      3874759        32.00
       1.475     0.971875      3887164        35.56
       1.488     0.975000      3899867        40.00
       1.505     0.978125      3912002        45.71
       1.533     0.981250      3924541        53.33
       1.593     0.984375      3936939        64.00
       1.664     0.985938      3943068        71.11
       1.785     0.987500      3949332        80.00
       1.928     0.989062      3955599        91.43
       2.089     0.990625      3961839       106.67
       2.279     0.992188      3968108       128.00
       2.389     0.992969      3971198       142.22
       2.521     0.993750      3974322       160.00
       2.693     0.994531      3977463       182.86
       2.953     0.995313      3980561       213.33
       3.475     0.996094      3983696       256.00
       3.903     0.996484      3985246       284.44
       4.547     0.996875      3986812       320.00
       5.575     0.997266      3988373       365.71
       7.083     0.997656      3989933       426.67
       8.583     0.998047      3991497       512.00
       9.535     0.998242      3992275       568.89
      10.759     0.998437      3993059       640.00
      12.591     0.998633      3993838       731.43
      22.351     0.998828      3994619       853.33
      62.015     0.999023      3995401      1024.00
      81.727     0.999121      3995790      1137.78
     101.311     0.999219      3996181      1280.00
     120.127     0.999316      3996574      1462.86
     139.391     0.999414      3996962      1706.67
     158.335     0.999512      3997353      2048.00
     167.679     0.999561      3997549      2275.56
     177.151     0.999609      3997744      2560.00
     186.879     0.999658      3997941      2925.71
     196.351     0.999707      3998135      3413.33
     206.079     0.999756      3998330      4096.00
     211.071     0.999780      3998431      4551.11
     215.679     0.999805      3998525      5120.00
     220.927     0.999829      3998624      5851.43
     225.919     0.999854      3998722      6826.67
     230.911     0.999878      3998818      8192.00
     233.599     0.999890      3998866      9102.22
     236.287     0.999902      3998916     10240.00
     238.975     0.999915      3998966     11702.86
     241.663     0.999927      3999013     13653.33
     245.887     0.999939      3999061     16384.00
     248.447     0.999945      3999086     18204.44
     250.751     0.999951      3999110     20480.00
     253.055     0.999957      3999135     23405.71
     255.359     0.999963      3999159     27306.67
     257.663     0.999969      3999183     32768.00
     258.943     0.999973      3999196     36408.89
     260.095     0.999976      3999208     40960.00
     261.247     0.999979      3999220     46811.43
     262.655     0.999982      3999233     54613.33
     263.935     0.999985      3999247     65536.00
     264.447     0.999986      3999251     72817.78
     264.959     0.999988      3999257     81920.00
     265.727     0.999989      3999264     93622.86
     266.239     0.999991      3999270    109226.67
     267.007     0.999992      3999277    131072.00
     267.263     0.999993      3999280    145635.56
     267.519     0.999994      3999281    163840.00
     268.031     0.999995      3999286    187245.71
     268.287     0.999995      3999288    218453.33
     268.543     0.999996      3999293    262144.00
     268.543     0.999997      3999293    291271.11
     268.543     0.999997      3999293    327680.00
     269.055     0.999997      3999296    374491.43
     269.055     0.999998      3999296    436906.67
     269.311     0.999998      3999301    524288.00
     269.311     0.999998      3999301    582542.22
     269.311     0.999998      3999301    655360.00
     269.311     0.999999      3999301    748982.86
     269.311     0.999999      3999301    873813.33
     269.823     0.999999      3999303   1048576.00
     269.823     0.999999      3999303   1165084.44
     269.823     0.999999      3999303   1310720.00
     269.823     0.999999      3999303   1497965.71
     269.823     0.999999      3999303   1747626.67
     270.079     1.000000      3999305   2097152.00
     270.079     1.000000      3999305          inf
#[Mean    =        1.116, StdDeviation   =        5.300]
#[Max     =      269.824, Total count    =      3999305]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  4799682 requests in 1.00m, 379.91MB read
Requests/sec:  80003.17
Transfer/sec:      6.33MB
```

150k:
```Running 1m test @ http://127.0.0.1:8082
  64 threads and 64 connections
  Thread calibration: mean lat.: 25.909ms, rate sampling interval: 235ms
  Thread calibration: mean lat.: 35.779ms, rate sampling interval: 338ms
  Thread calibration: mean lat.: 4.505ms, rate sampling interval: 12ms
  Thread calibration: mean lat.: 29.169ms, rate sampling interval: 284ms
  Thread calibration: mean lat.: 4.501ms, rate sampling interval: 11ms
  Thread calibration: mean lat.: 5.193ms, rate sampling interval: 11ms
  Thread calibration: mean lat.: 30.805ms, rate sampling interval: 292ms
  Thread calibration: mean lat.: 4.780ms, rate sampling interval: 15ms
  Thread calibration: mean lat.: 4.572ms, rate sampling interval: 11ms
  Thread calibration: mean lat.: 24.801ms, rate sampling interval: 233ms
  Thread calibration: mean lat.: 26.752ms, rate sampling interval: 246ms
  Thread calibration: mean lat.: 33.071ms, rate sampling interval: 307ms
  Thread calibration: mean lat.: 1.942ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 34.636ms, rate sampling interval: 324ms
  Thread calibration: mean lat.: 27.962ms, rate sampling interval: 268ms
  Thread calibration: mean lat.: 24.444ms, rate sampling interval: 230ms
  Thread calibration: mean lat.: 4.759ms, rate sampling interval: 15ms
  Thread calibration: mean lat.: 1.846ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 4.429ms, rate sampling interval: 11ms
  Thread calibration: mean lat.: 5.852ms, rate sampling interval: 21ms
  Thread calibration: mean lat.: 5.328ms, rate sampling interval: 17ms
  Thread calibration: mean lat.: 3.566ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 3.861ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 3.987ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 5.380ms, rate sampling interval: 12ms
  Thread calibration: mean lat.: 1.813ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 39.006ms, rate sampling interval: 360ms
  Thread calibration: mean lat.: 26.517ms, rate sampling interval: 239ms
  Thread calibration: mean lat.: 28.078ms, rate sampling interval: 257ms
  Thread calibration: mean lat.: 4.210ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 5.099ms, rate sampling interval: 17ms
  Thread calibration: mean lat.: 6.547ms, rate sampling interval: 14ms
  Thread calibration: mean lat.: 4.298ms, rate sampling interval: 14ms
  Thread calibration: mean lat.: 39.587ms, rate sampling interval: 362ms
  Thread calibration: mean lat.: 26.389ms, rate sampling interval: 238ms
  Thread calibration: mean lat.: 28.122ms, rate sampling interval: 268ms
  Thread calibration: mean lat.: 21.965ms, rate sampling interval: 214ms
  Thread calibration: mean lat.: 1.733ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 6.253ms, rate sampling interval: 20ms
  Thread calibration: mean lat.: 5.607ms, rate sampling interval: 15ms
  Thread calibration: mean lat.: 27.629ms, rate sampling interval: 258ms
  Thread calibration: mean lat.: 27.705ms, rate sampling interval: 259ms
  Thread calibration: mean lat.: 5.411ms, rate sampling interval: 12ms
  Thread calibration: mean lat.: 25.527ms, rate sampling interval: 230ms
  Thread calibration: mean lat.: 5.960ms, rate sampling interval: 16ms
  Thread calibration: mean lat.: 27.367ms, rate sampling interval: 259ms
  Thread calibration: mean lat.: 4.305ms, rate sampling interval: 11ms
  Thread calibration: mean lat.: 6.100ms, rate sampling interval: 15ms
  Thread calibration: mean lat.: 4.449ms, rate sampling interval: 11ms
  Thread calibration: mean lat.: 23.523ms, rate sampling interval: 215ms
  Thread calibration: mean lat.: 30.146ms, rate sampling interval: 265ms
  Thread calibration: mean lat.: 3.657ms, rate sampling interval: 13ms
  Thread calibration: mean lat.: 3.987ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 4.205ms, rate sampling interval: 11ms
  Thread calibration: mean lat.: 23.832ms, rate sampling interval: 219ms
  Thread calibration: mean lat.: 25.178ms, rate sampling interval: 217ms
  Thread calibration: mean lat.: 28.876ms, rate sampling interval: 265ms
  Thread calibration: mean lat.: 29.433ms, rate sampling interval: 265ms
  Thread calibration: mean lat.: 5.413ms, rate sampling interval: 14ms
  Thread calibration: mean lat.: 4.486ms, rate sampling interval: 13ms
  Thread calibration: mean lat.: 24.463ms, rate sampling interval: 230ms
  Thread calibration: mean lat.: 27.125ms, rate sampling interval: 247ms
  Thread calibration: mean lat.: 21.689ms, rate sampling interval: 211ms
  Thread calibration: mean lat.: 3.955ms, rate sampling interval: 12ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.71ms   14.98ms 451.33ms   98.03%
    Req/Sec     2.45k   298.13    12.11k    87.67%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.07ms
 75.000%    1.57ms
 90.000%    2.34ms
 99.000%   39.29ms
 99.900%  296.45ms
 99.990%  427.52ms
 99.999%  447.49ms
100.000%  451.58ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.019     0.000000            2         1.00
       0.351     0.100000       749969         1.11
       0.592     0.200000      1500313         1.25
       0.805     0.300000      2251103         1.43
       0.942     0.400000      3000327         1.67
       1.067     0.500000      3754246         2.00
       1.136     0.550000      4128698         2.22
       1.231     0.600000      4501441         2.50
       1.339     0.650000      4876686         2.86
       1.453     0.700000      5249863         3.33
       1.573     0.750000      5625942         4.00
       1.631     0.775000      5812238         4.44
       1.689     0.800000      5999296         5.00
       1.748     0.825000      6187221         5.71
       1.814     0.850000      6376142         6.67
       1.934     0.875000      6561799         8.00
       2.085     0.887500      6655227         8.89
       2.335     0.900000      6749153        10.00
       2.679     0.912500      6842854        11.43
       3.169     0.925000      6936510        13.33
       3.957     0.937500      7030350        16.00
       4.535     0.943750      7077078        17.78
       5.287     0.950000      7123978        20.00
       6.331     0.956250      7170778        22.86
       7.819     0.962500      7217703        26.67
      10.055     0.968750      7264619        32.00
      11.559     0.971875      7288045        35.56
      13.463     0.975000      7311448        40.00
      15.695     0.978125      7334849        45.71
      18.687     0.981250      7358310        53.33
      23.119     0.984375      7381729        64.00
      26.239     0.985938      7393399        71.11
      30.479     0.987500      7405146        80.00
      35.743     0.989062      7416847        91.43
      42.303     0.990625      7428548       106.67
      49.471     0.992188      7440270       128.00
      52.927     0.992969      7446115       142.22
      56.703     0.993750      7451970       160.00
      60.671     0.994531      7457848       182.86
      64.415     0.995313      7463725       213.33
      69.247     0.996094      7469570       256.00
      72.511     0.996484      7472498       284.44
      77.247     0.996875      7475425       320.00
      84.991     0.997266      7478354       365.71
      98.367     0.997656      7481277       426.67
     146.943     0.998047      7484194       512.00
     174.335     0.998242      7485660       568.89
     204.287     0.998437      7487124       640.00
     235.007     0.998633      7488592       731.43
     268.031     0.998828      7490056       853.33
     300.287     0.999023      7491514      1024.00
     315.647     0.999121      7492247      1137.78
     329.983     0.999219      7492982      1280.00
     343.295     0.999316      7493728      1462.86
     356.607     0.999414      7494455      1706.67
     371.455     0.999512      7495179      2048.00
     378.367     0.999561      7495542      2275.56
     386.047     0.999609      7495919      2560.00
     393.471     0.999658      7496282      2925.71
     400.639     0.999707      7496648      3413.33
     407.807     0.999756      7497025      4096.00
     410.879     0.999780      7497197      4551.11
     414.463     0.999805      7497375      5120.00
     417.791     0.999829      7497557      5851.43
     420.863     0.999854      7497740      6826.67
     424.191     0.999878      7497927      8192.00
     425.983     0.999890      7498023      9102.22
     427.775     0.999902      7498109     10240.00
     429.567     0.999915      7498201     11702.86
     432.127     0.999927      7498290     13653.33
     434.943     0.999939      7498387     16384.00
     436.479     0.999945      7498426     18204.44
     437.759     0.999951      7498475     20480.00
     439.039     0.999957      7498518     23405.71
     440.831     0.999963      7498571     27306.67
     442.111     0.999969      7498616     32768.00
     442.623     0.999973      7498636     36408.89
     443.135     0.999976      7498657     40960.00
     443.903     0.999979      7498682     46811.43
     444.671     0.999982      7498700     54613.33
     445.439     0.999985      7498724     65536.00
     445.951     0.999986      7498736     72817.78
     446.719     0.999988      7498749     81920.00
     447.231     0.999989      7498758     93622.86
     447.999     0.999991      7498770    109226.67
     448.511     0.999992      7498782    131072.00
     449.023     0.999993      7498788    145635.56
     449.279     0.999994      7498793    163840.00
     449.535     0.999995      7498799    187245.71
     450.047     0.999995      7498814    218453.33
     450.047     0.999996      7498814    262144.00
     450.047     0.999997      7498814    291271.11
     450.303     0.999997      7498826    327680.00
     450.303     0.999997      7498826    374491.43
     450.303     0.999998      7498826    436906.67
     450.303     0.999998      7498826    524288.00
     450.303     0.999998      7498826    582542.22
     450.303     0.999998      7498826    655360.00
     450.559     0.999999      7498832    748982.86
     450.559     0.999999      7498832    873813.33
     450.559     0.999999      7498832   1048576.00
     450.559     0.999999      7498832   1165084.44
     450.559     0.999999      7498832   1310720.00
     450.559     0.999999      7498832   1497965.71
     450.815     0.999999      7498833   1747626.67
     451.071     1.000000      7498835   2097152.00
     451.071     1.000000      7498835   2330168.89
     451.071     1.000000      7498835   2621440.00
     451.071     1.000000      7498835   2995931.43
     451.071     1.000000      7498835   3495253.33
     451.583     1.000000      7498837   4194304.00
     451.583     1.000000      7498837          inf
#[Mean    =        2.705, StdDeviation   =       14.977]
#[Max     =      451.328, Total count    =      7498837]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  8999316 requests in 1.00m, 712.34MB read
Requests/sec: 150007.84
Transfer/sec:     11.87MB
```

![GRAPH](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/fixed/put.png)

Видно, что кластер спокойно выдерживает нагрузку в 150 тысяч запросов в секунду.

## CPU
![CPU](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/fixed/cpu.svg)

## CPU threads
![CPU](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/fixed/cpu-threaded.svg)

Больше всего времени занимало проксирование ~36% и отправка запросов во всех остальных случаях.

## ALLOC

![ALLOC](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/fixed/alloc-cpu-put.svg)

Аллоцирование в большинстве случае происходило либо при проксировании ~23%, либо при обработке вставки ключа ~32%

## LOCK:

![LOCK](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/fixed/lock-cpu-put.svg)

Блокирование в проксировании заняло ~60%.

# GET
wrk2 -t64 -c64 -d30s -R* -s test-results/lua/wrk2-scripts/put-request.lua --latency\\
20k:
```Running 1m test @ http://127.0.0.1:8082
  64 threads and 64 connections
  Thread calibration: mean lat.: 0.871ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.554ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.040ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.039ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.975ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.899ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.102ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.158ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.114ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.134ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.017ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.965ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.140ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.786ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.925ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.890ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.996ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.887ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.950ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.207ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.844ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.418ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.777ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.029ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.971ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.087ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.071ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.965ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.083ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.243ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.098ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.936ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.155ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.061ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.944ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.859ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.161ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.108ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.930ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.215ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.008ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.111ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.165ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.017ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.819ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.124ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.202ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.921ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.902ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.213ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.857ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.924ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.027ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.375ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.060ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.447ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.900ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.209ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.112ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.243ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 1.086ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.792ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.941ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 0.810ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   720.85us  314.81us   7.14ms   59.98%
    Req/Sec   334.50     37.60   555.00     85.15%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%  723.00us
 75.000%    0.98ms
 90.000%    1.13ms
 99.000%    1.33ms
 99.900%    1.83ms
 99.990%    2.80ms
 99.999%    5.39ms
100.000%    7.14ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.089     0.000000            1         1.00
       0.294     0.100000       100202         1.11
       0.399     0.200000       200274         1.25
       0.506     0.300000       300761         1.43
       0.616     0.400000       400518         1.67
       0.723     0.500000       500408         2.00
       0.776     0.550000       550789         2.22
       0.828     0.600000       600591         2.50
       0.879     0.650000       649902         2.86
       0.930     0.700000       700733         3.33
       0.980     0.750000       750600         4.00
       1.005     0.775000       775428         4.44
       1.031     0.800000       800815         5.00
       1.056     0.825000       825477         5.71
       1.082     0.850000       850795         6.67
       1.107     0.875000       875278         8.00
       1.120     0.887500       887926         8.89
       1.133     0.900000       900591        10.00
       1.146     0.912500       913056        11.43
       1.159     0.925000       925099        13.33
       1.175     0.937500       937975        16.00
       1.183     0.943750       943694        17.78
       1.193     0.950000       950079        20.00
       1.204     0.956250       956539        22.86
       1.216     0.962500       962643        26.67
       1.230     0.968750       968717        32.00
       1.239     0.971875       971992        35.56
       1.248     0.975000       974943        40.00
       1.259     0.978125       978073        45.71
       1.272     0.981250       981147        53.33
       1.288     0.984375       984285        64.00
       1.297     0.985938       985770        71.11
       1.308     0.987500       987401        80.00
       1.320     0.989062       988983        91.43
       1.335     0.990625       990538       106.67
       1.354     0.992188       992041       128.00
       1.365     0.992969       992781       142.22
       1.379     0.993750       993584       160.00
       1.395     0.994531       994358       182.86
       1.416     0.995313       995164       213.33
       1.440     0.996094       995918       256.00
       1.457     0.996484       996311       284.44
       1.477     0.996875       996694       320.00
       1.505     0.997266       997079       365.71
       1.538     0.997656       997466       426.67
       1.592     0.998047       997858       512.00
       1.623     0.998242       998054       568.89
       1.660     0.998437       998252       640.00
       1.707     0.998633       998446       731.43
       1.771     0.998828       998641       853.33
       1.835     0.999023       998835      1024.00
       1.866     0.999121       998933      1137.78
       1.907     0.999219       999032      1280.00
       1.947     0.999316       999126      1462.86
       1.999     0.999414       999224      1706.67
       2.063     0.999512       999323      2048.00
       2.101     0.999561       999372      2275.56
       2.147     0.999609       999422      2560.00
       2.189     0.999658       999468      2925.71
       2.245     0.999707       999519      3413.33
       2.323     0.999756       999565      4096.00
       2.363     0.999780       999590      4551.11
       2.413     0.999805       999614      5120.00
       2.509     0.999829       999640      5851.43
       2.571     0.999854       999663      6826.67
       2.657     0.999878       999687      8192.00
       2.725     0.999890       999700      9102.22
       2.813     0.999902       999713     10240.00
       2.921     0.999915       999724     11702.86
       3.113     0.999927       999736     13653.33
       3.329     0.999939       999748     16384.00
       3.455     0.999945       999755     18204.44
       3.537     0.999951       999761     20480.00
       3.623     0.999957       999767     23405.71
       3.737     0.999963       999773     27306.67
       3.883     0.999969       999779     32768.00
       4.031     0.999973       999782     36408.89
       4.275     0.999976       999785     40960.00
       4.471     0.999979       999788     46811.43
       4.579     0.999982       999791     54613.33
       4.727     0.999985       999794     65536.00
       4.839     0.999986       999796     72817.78
       4.987     0.999988       999797     81920.00
       5.395     0.999989       999799     93622.86
       5.571     0.999991       999800    109226.67
       5.847     0.999992       999802    131072.00
       5.863     0.999993       999803    145635.56
       5.863     0.999994       999803    163840.00
       6.231     0.999995       999804    187245.71
       6.363     0.999995       999805    218453.33
       6.379     0.999996       999806    262144.00
       6.379     0.999997       999806    291271.11
       6.379     0.999997       999806    327680.00
       6.619     0.999997       999807    374491.43
       6.619     0.999998       999807    436906.67
       6.807     0.999998       999808    524288.00
       6.807     0.999998       999808    582542.22
       6.807     0.999998       999808    655360.00
       6.807     0.999999       999808    748982.86
       6.807     0.999999       999808    873813.33
       7.143     0.999999       999809   1048576.00
       7.143     1.000000       999809          inf
#[Mean    =        0.721, StdDeviation   =        0.315]
#[Max     =        7.140, Total count    =       999809]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  1199996 requests in 1.00m, 105.29MB read
Requests/sec:  20001.29
Transfer/sec:      1.75MB ```

50k:
```Running 1m test @ http://127.0.0.1:8082
  64 threads and 64 connections
  Thread calibration: mean lat.: 2857.440ms, rate sampling interval: 10002ms
  Thread calibration: mean lat.: 1380.610ms, rate sampling interval: 4669ms
  Thread calibration: mean lat.: 1368.909ms, rate sampling interval: 4739ms
  Thread calibration: mean lat.: 425.763ms, rate sampling interval: 1344ms
  Thread calibration: mean lat.: 2307.755ms, rate sampling interval: 8171ms
  Thread calibration: mean lat.: 2239.061ms, rate sampling interval: 8077ms
  Thread calibration: mean lat.: 1352.567ms, rate sampling interval: 4710ms
  Thread calibration: mean lat.: 1383.831ms, rate sampling interval: 4677ms
  Thread calibration: mean lat.: 485.371ms, rate sampling interval: 1559ms
  Thread calibration: mean lat.: 1883.636ms, rate sampling interval: 6569ms
  Thread calibration: mean lat.: 2163.403ms, rate sampling interval: 5730ms
  Thread calibration: mean lat.: 491.966ms, rate sampling interval: 1571ms
  Thread calibration: mean lat.: 1355.899ms, rate sampling interval: 4694ms
  Thread calibration: mean lat.: 212.114ms, rate sampling interval: 900ms
  Thread calibration: mean lat.: 1933.768ms, rate sampling interval: 6770ms
  Thread calibration: mean lat.: 1334.226ms, rate sampling interval: 4612ms
  Thread calibration: mean lat.: 2134.137ms, rate sampling interval: 5693ms
  Thread calibration: mean lat.: 235.021ms, rate sampling interval: 980ms
  Thread calibration: mean lat.: 1646.323ms, rate sampling interval: 4143ms
  Thread calibration: mean lat.: 648.252ms, rate sampling interval: 2473ms
  Thread calibration: mean lat.: 323.192ms, rate sampling interval: 1099ms
  Thread calibration: mean lat.: 1336.650ms, rate sampling interval: 4616ms
  Thread calibration: mean lat.: 2853.844ms, rate sampling interval: 10133ms
  Thread calibration: mean lat.: 316.518ms, rate sampling interval: 1277ms
  Thread calibration: mean lat.: 460.662ms, rate sampling interval: 1476ms
  Thread calibration: mean lat.: 2804.431ms, rate sampling interval: 9953ms
  Thread calibration: mean lat.: 397.443ms, rate sampling interval: 1432ms
  Thread calibration: mean lat.: 1933.294ms, rate sampling interval: 6770ms
  Thread calibration: mean lat.: 1154.132ms, rate sampling interval: 5779ms
  Thread calibration: mean lat.: 681.065ms, rate sampling interval: 3641ms
  Thread calibration: mean lat.: 1382.454ms, rate sampling interval: 4673ms
  Thread calibration: mean lat.: 443.562ms, rate sampling interval: 1501ms
  Thread calibration: mean lat.: 324.742ms, rate sampling interval: 1524ms
  Thread calibration: mean lat.: 427.663ms, rate sampling interval: 1350ms
  Thread calibration: mean lat.: 2966.871ms, rate sampling interval: 9142ms
  Thread calibration: mean lat.: 1357.431ms, rate sampling interval: 4698ms
  Thread calibration: mean lat.: 2704.071ms, rate sampling interval: 9846ms
  Thread calibration: mean lat.: 493.342ms, rate sampling interval: 1546ms
  Thread calibration: mean lat.: 2982.236ms, rate sampling interval: 10002ms
  Thread calibration: mean lat.: 1760.017ms, rate sampling interval: 6197ms
  Thread calibration: mean lat.: 1354.335ms, rate sampling interval: 4718ms
  Thread calibration: mean lat.: 1934.304ms, rate sampling interval: 6770ms
  Thread calibration: mean lat.: 1440.981ms, rate sampling interval: 4800ms
  Thread calibration: mean lat.: 1356.987ms, rate sampling interval: 4698ms
  Thread calibration: mean lat.: 1359.339ms, rate sampling interval: 5214ms
  Thread calibration: mean lat.: 289.052ms, rate sampling interval: 1219ms
  Thread calibration: mean lat.: 2352.551ms, rate sampling interval: 8253ms
  Thread calibration: mean lat.: 1362.600ms, rate sampling interval: 4706ms
  Thread calibration: mean lat.: 2217.469ms, rate sampling interval: 8146ms
  Thread calibration: mean lat.: 485.450ms, rate sampling interval: 1558ms
  Thread calibration: mean lat.: 501.890ms, rate sampling interval: 1614ms
  Thread calibration: mean lat.: 1354.009ms, rate sampling interval: 4718ms
  Thread calibration: mean lat.: 3634.307ms, rate sampling interval: 12550ms
  Thread calibration: mean lat.: 490.714ms, rate sampling interval: 1539ms
  Thread calibration: mean lat.: 243.928ms, rate sampling interval: 972ms
  Thread calibration: mean lat.: 2865.935ms, rate sampling interval: 10149ms
  Thread calibration: mean lat.: 2315.542ms, rate sampling interval: 8237ms
  Thread calibration: mean lat.: 2863.281ms, rate sampling interval: 10117ms
  Thread calibration: mean lat.: 2328.458ms, rate sampling interval: 8183ms
  Thread calibration: mean lat.: 2083.499ms, rate sampling interval: 6840ms
  Thread calibration: mean lat.: 1382.373ms, rate sampling interval: 4669ms
  Thread calibration: mean lat.: 1122.079ms, rate sampling interval: 5623ms
  Thread calibration: mean lat.: 2898.242ms, rate sampling interval: 10158ms
  Thread calibration: mean lat.: 462.856ms, rate sampling interval: 1480ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     8.97s     6.45s   30.06s    63.37%
    Req/Sec   622.79    168.71     0.99k    64.29%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    8.26s 
 75.000%   13.18s 
 90.000%   17.87s 
 99.000%   25.64s 
 99.900%   28.97s 
 99.990%   29.93s 
 99.999%   30.06s 
100.000%   30.08s 

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.558     0.000000            1         1.00
     983.039     0.100000       179618         1.11
    2500.607     0.200000       359329         1.25
    4583.423     0.300000       539169         1.43
    6377.471     0.400000       718451         1.67
    8257.535     0.500000       898024         2.00
    9347.071     0.550000       987962         2.22
   10215.423     0.600000      1077747         2.50
   11239.423     0.650000      1167646         2.86
   12189.695     0.700000      1257427         3.33
   13180.927     0.750000      1346976         4.00
   13778.943     0.775000      1391981         4.44
   14417.919     0.800000      1436767         5.00
   15147.007     0.825000      1481695         5.71
   15900.671     0.850000      1526535         6.67
   16842.751     0.875000      1572035         8.00
   17317.887     0.887500      1594409         8.89
   17874.943     0.900000      1616713        10.00
   18677.759     0.912500      1639073        11.43
   19529.727     0.925000      1661262        13.33
   20365.311     0.937500      1683723        16.00
   20840.447     0.943750      1695236        17.78
   21266.431     0.950000      1706138        20.00
   21708.799     0.956250      1717568        22.86
   22200.319     0.962500      1728717        26.67
   22724.607     0.968750      1739924        32.00
   23019.519     0.971875      1745560        35.56
   23330.815     0.975000      1751023        40.00
   23691.263     0.978125      1756764        45.71
   24150.015     0.981250      1762410        53.33
   24625.151     0.984375      1767997        64.00
   24854.527     0.985938      1770725        71.11
   25149.439     0.987500      1773474        80.00
   25477.119     0.989062      1776432        91.43
   25772.031     0.990625      1779071       106.67
   26116.095     0.992188      1781895       128.00
   26279.935     0.992969      1783314       142.22
   26443.775     0.993750      1784723       160.00
   26623.999     0.994531      1786113       182.86
   26836.991     0.995313      1787538       213.33
   27082.751     0.996094      1788914       256.00
   27328.511     0.996484      1789607       284.44
   27574.271     0.996875      1790301       320.00
   27836.415     0.997266      1791013       365.71
   28098.559     0.997656      1791730       426.67
   28344.319     0.998047      1792404       512.00
   28475.391     0.998242      1792762       568.89
   28606.463     0.998437      1793115       640.00
   28737.535     0.998633      1793481       731.43
   28868.607     0.998828      1793847       853.33
   28983.295     0.999023      1794168      1024.00
   29048.831     0.999121      1794352      1137.78
   29130.751     0.999219      1794511      1280.00
   29229.055     0.999316      1794684      1462.86
   29327.359     0.999414      1794883      1706.67
   29409.279     0.999512      1795033      2048.00
   29458.431     0.999561      1795148      2275.56
   29507.583     0.999609      1795213      2560.00
   29573.119     0.999658      1795297      2925.71
   29671.423     0.999707      1795399      3413.33
   29736.959     0.999756      1795481      4096.00
   29769.727     0.999780      1795526      4551.11
   29802.495     0.999805      1795571      5120.00
   29835.263     0.999829      1795609      5851.43
   29868.031     0.999854      1795645      6826.67
   29900.799     0.999878      1795688      8192.00
   29917.183     0.999890      1795720      9102.22
   29933.567     0.999902      1795738     10240.00
   29949.951     0.999915      1795756     11702.86
   29982.719     0.999927      1795783     13653.33
   29999.103     0.999939      1795806     16384.00
   30015.487     0.999945      1795836     18204.44
   30015.487     0.999951      1795836     20480.00
   30015.487     0.999957      1795836     23405.71
   30031.871     0.999963      1795858     27306.67
   30031.871     0.999969      1795858     32768.00
   30031.871     0.999973      1795858     36408.89
   30048.255     0.999976      1795876     40960.00
   30048.255     0.999979      1795876     46811.43
   30048.255     0.999982      1795876     54613.33
   30064.639     0.999985      1795897     65536.00
   30064.639     0.999986      1795897     72817.78
   30064.639     0.999988      1795897     81920.00
   30064.639     0.999989      1795897     93622.86
   30064.639     0.999991      1795897    109226.67
   30064.639     0.999992      1795897    131072.00
   30064.639     0.999993      1795897    145635.56
   30064.639     0.999994      1795897    163840.00
   30081.023     0.999995      1795907    187245.71
   30081.023     1.000000      1795907          inf
#[Mean    =     8969.465, StdDeviation   =     6453.269]
#[Max     =    30064.640, Total count    =      1795907]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  2155572 requests in 1.00m, 189.13MB read
Requests/sec:  35930.38
Transfer/sec:      3.15MB
```

80k:
```Running 1m test @ http://127.0.0.1:8082
  64 threads and 64 connections
  Thread calibration: mean lat.: 3548.683ms, rate sampling interval: 11591ms
  Thread calibration: mean lat.: 2160.948ms, rate sampling interval: 7774ms
  Thread calibration: mean lat.: 3061.079ms, rate sampling interval: 9936ms
  Thread calibration: mean lat.: 2714.637ms, rate sampling interval: 9814ms
  Thread calibration: mean lat.: 703.297ms, rate sampling interval: 3690ms
  Thread calibration: mean lat.: 2985.569ms, rate sampling interval: 10174ms
  Thread calibration: mean lat.: 3345.593ms, rate sampling interval: 12304ms
  Thread calibration: mean lat.: 3078.714ms, rate sampling interval: 11108ms
  Thread calibration: mean lat.: 2670.274ms, rate sampling interval: 11296ms
  Thread calibration: mean lat.: 2908.969ms, rate sampling interval: 10633ms
  Thread calibration: mean lat.: 3071.372ms, rate sampling interval: 11894ms
  Thread calibration: mean lat.: 3546.568ms, rate sampling interval: 13500ms
  Thread calibration: mean lat.: 3098.299ms, rate sampling interval: 11984ms
  Thread calibration: mean lat.: 807.657ms, rate sampling interval: 2916ms
  Thread calibration: mean lat.: 3384.831ms, rate sampling interval: 10190ms
  Thread calibration: mean lat.: 2898.609ms, rate sampling interval: 10084ms
  Thread calibration: mean lat.: 2775.735ms, rate sampling interval: 8806ms
  Thread calibration: mean lat.: 2681.181ms, rate sampling interval: 9650ms
  Thread calibration: mean lat.: 2703.887ms, rate sampling interval: 9732ms
  Thread calibration: mean lat.: 2975.673ms, rate sampling interval: 10756ms
  Thread calibration: mean lat.: 3577.827ms, rate sampling interval: 11960ms
  Thread calibration: mean lat.: 3520.627ms, rate sampling interval: 13041ms
  Thread calibration: mean lat.: 2951.978ms, rate sampling interval: 12378ms
  Thread calibration: mean lat.: 2158.257ms, rate sampling interval: 7811ms
  Thread calibration: mean lat.: 2902.488ms, rate sampling interval: 9789ms
  Thread calibration: mean lat.: 2771.255ms, rate sampling interval: 11124ms
  Thread calibration: mean lat.: 3119.018ms, rate sampling interval: 12419ms
  Thread calibration: mean lat.: 2742.089ms, rate sampling interval: 8757ms
  Thread calibration: mean lat.: 3419.142ms, rate sampling interval: 11698ms
  Thread calibration: mean lat.: 1773.078ms, rate sampling interval: 4849ms
  Thread calibration: mean lat.: 2742.377ms, rate sampling interval: 9797ms
  Thread calibration: mean lat.: 2723.678ms, rate sampling interval: 9822ms
  Thread calibration: mean lat.: 2506.340ms, rate sampling interval: 9641ms
  Thread calibration: mean lat.: 3569.644ms, rate sampling interval: 11157ms
  Thread calibration: mean lat.: 2134.200ms, rate sampling interval: 7675ms
  Thread calibration: mean lat.: 2539.723ms, rate sampling interval: 9543ms
  Thread calibration: mean lat.: 3080.758ms, rate sampling interval: 11108ms
  Thread calibration: mean lat.: 3214.822ms, rate sampling interval: 10510ms
  Thread calibration: mean lat.: 2825.728ms, rate sampling interval: 9986ms
  Thread calibration: mean lat.: 2147.979ms, rate sampling interval: 7774ms
  Thread calibration: mean lat.: 2093.211ms, rate sampling interval: 9289ms
  Thread calibration: mean lat.: 3343.343ms, rate sampling interval: 12877ms
  Thread calibration: mean lat.: 2646.283ms, rate sampling interval: 9543ms
  Thread calibration: mean lat.: 3941.755ms, rate sampling interval: 13041ms
  Thread calibration: mean lat.: 2898.988ms, rate sampling interval: 10231ms
  Thread calibration: mean lat.: 2301.832ms, rate sampling interval: 8839ms
  Thread calibration: mean lat.: 2800.707ms, rate sampling interval: 8871ms
  Thread calibration: mean lat.: 3930.260ms, rate sampling interval: 12976ms
  Thread calibration: mean lat.: 2703.716ms, rate sampling interval: 9732ms
  Thread calibration: mean lat.: 3566.134ms, rate sampling interval: 11870ms
  Thread calibration: mean lat.: 2148.946ms, rate sampling interval: 7774ms
  Thread calibration: mean lat.: 2542.737ms, rate sampling interval: 8863ms
  Thread calibration: mean lat.: 2367.638ms, rate sampling interval: 7987ms
  Thread calibration: mean lat.: 2457.281ms, rate sampling interval: 10543ms
  Thread calibration: mean lat.: 3289.464ms, rate sampling interval: 10543ms
  Thread calibration: mean lat.: 2079.967ms, rate sampling interval: 8032ms
  Thread calibration: mean lat.: 3273.607ms, rate sampling interval: 12509ms
  Thread calibration: mean lat.: 2395.263ms, rate sampling interval: 8134ms
  Thread calibration: mean lat.: 3310.933ms, rate sampling interval: 11845ms
  Thread calibration: mean lat.: 3334.545ms, rate sampling interval: 10158ms
  Thread calibration: mean lat.: 2442.525ms, rate sampling interval: 8667ms
  Thread calibration: mean lat.: 2176.681ms, rate sampling interval: 9461ms
  Thread calibration: mean lat.: 2189.613ms, rate sampling interval: 7860ms
  Thread calibration: mean lat.: 2926.630ms, rate sampling interval: 10117ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    25.45s    16.14s   50.53s    46.17%
    Req/Sec   254.61    255.17     1.36k    84.18%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%   29.13s 
 75.000%   41.52s 
 90.000%   44.24s 
 99.000%   47.42s 
 99.900%   49.81s 
 99.990%   50.33s 
 99.999%   50.53s 
100.000%   50.56s 

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

    1517.567     0.000000            2         1.00
    6356.991     0.100000        83989         1.11
    7823.359     0.200000       168053         1.25
    9388.031     0.300000       251951         1.43
   11976.703     0.400000       335872         1.67
   29130.751     0.500000       419760         2.00
   33062.911     0.550000       461753         2.22
   38109.183     0.600000       504182         2.50
   39682.047     0.650000       546020         2.86
   40730.623     0.700000       588030         3.33
   41517.055     0.750000       631042         4.00
   41910.271     0.775000       651797         4.44
   42303.487     0.800000       672842         5.00
   42729.471     0.825000       694054         5.71
   43155.455     0.850000       714089         6.67
   43646.975     0.875000       734776         8.00
   43941.887     0.887500       745763         8.89
   44236.799     0.900000       756173        10.00
   44531.711     0.912500       766900        11.43
   44859.391     0.925000       776906        13.33
   45252.607     0.937500       787417        16.00
   45449.215     0.943750       792638        17.78
   45645.823     0.950000       797807        20.00
   45842.431     0.956250       803047        22.86
   46039.039     0.962500       808238        26.67
   46268.415     0.968750       813661        32.00
   46399.487     0.971875       816219        35.56
   46530.559     0.975000       818749        40.00
   46694.399     0.978125       821669        45.71
   46825.471     0.981250       823852        53.33
   46989.311     0.984375       826412        64.00
   47087.615     0.985938       827961        71.11
   47185.919     0.987500       829281        80.00
   47316.991     0.989062       830552        91.43
   47513.599     0.990625       831697       106.67
   47808.511     0.992188       833097       128.00
   47972.351     0.992969       833672       142.22
   48168.959     0.993750       834288       160.00
   48365.567     0.994531       834925       182.86
   48562.175     0.995313       835581       213.33
   48824.319     0.996094       836242       256.00
   48955.391     0.996484       836605       284.44
   49086.463     0.996875       836924       320.00
   49217.535     0.997266       837292       365.71
   49315.839     0.997656       837593       426.67
   49446.911     0.998047       837917       512.00
   49512.447     0.998242       838072       568.89
   49577.983     0.998437       838232       640.00
   49643.519     0.998633       838370       731.43
   49709.055     0.998828       838528       853.33
   49807.359     0.999023       838711      1024.00
   49872.895     0.999121       838803      1137.78
   49938.431     0.999219       838895      1280.00
   50003.967     0.999316       838978      1462.86
   50036.735     0.999414       839029      1706.67
   50102.271     0.999512       839120      2048.00
   50135.039     0.999561       839183      2275.56
   50135.039     0.999609       839183      2560.00
   50167.807     0.999658       839240      2925.71
   50200.575     0.999707       839293      3413.33
   50233.343     0.999756       839337      4096.00
   50233.343     0.999780       839337      4551.11
   50266.111     0.999805       839375      5120.00
   50266.111     0.999829       839375      5851.43
   50298.879     0.999854       839404      6826.67
   50331.647     0.999878       839430      8192.00
   50331.647     0.999890       839430      9102.22
   50331.647     0.999902       839430     10240.00
   50364.415     0.999915       839445     11702.86
   50397.183     0.999927       839457     13653.33
   50429.951     0.999939       839474     16384.00
   50429.951     0.999945       839474     18204.44
   50429.951     0.999951       839474     20480.00
   50429.951     0.999957       839474     23405.71
   50462.719     0.999963       839483     27306.67
   50495.487     0.999969       839492     32768.00
   50495.487     0.999973       839492     36408.89
   50495.487     0.999976       839492     40960.00
   50495.487     0.999979       839492     46811.43
   50528.255     0.999982       839503     54613.33
   50528.255     0.999985       839503     65536.00
   50528.255     0.999986       839503     72817.78
   50528.255     0.999988       839503     81920.00
   50528.255     0.999989       839503     93622.86
   50528.255     0.999991       839503    109226.67
   50528.255     0.999992       839503    131072.00
   50561.023     0.999993       839509    145635.56
   50561.023     1.000000       839509          inf
#[Mean    =    25447.858, StdDeviation   =    16135.661]
#[Max     =    50528.256, Total count    =       839509]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  1197632 requests in 1.00m, 105.08MB read
Requests/sec:  19962.16
Transfer/sec:      1.75MB
```

150k:
```Running 1m test @ http://127.0.0.1:8082
  64 threads and 64 connections
  Thread calibration: mean lat.: 3606.846ms, rate sampling interval: 12378ms
  Thread calibration: mean lat.: 4242.950ms, rate sampling interval: 14614ms
  Thread calibration: mean lat.: 4761.680ms, rate sampling interval: 15515ms
  Thread calibration: mean lat.: 3482.309ms, rate sampling interval: 12525ms
  Thread calibration: mean lat.: 3752.016ms, rate sampling interval: 15097ms
  Thread calibration: mean lat.: 3770.849ms, rate sampling interval: 13533ms
  Thread calibration: mean lat.: 3459.714ms, rate sampling interval: 12484ms
  Thread calibration: mean lat.: 4314.703ms, rate sampling interval: 15065ms
  Thread calibration: mean lat.: 3972.293ms, rate sampling interval: 14278ms
  Thread calibration: mean lat.: 1793.042ms, rate sampling interval: 6406ms
  Thread calibration: mean lat.: 4183.124ms, rate sampling interval: 14262ms
  Thread calibration: mean lat.: 3771.857ms, rate sampling interval: 13508ms
  Thread calibration: mean lat.: 3482.953ms, rate sampling interval: 12861ms
  Thread calibration: mean lat.: 3511.578ms, rate sampling interval: 12500ms
  Thread calibration: mean lat.: 3733.153ms, rate sampling interval: 14770ms
  Thread calibration: mean lat.: 3724.919ms, rate sampling interval: 13508ms
  Thread calibration: mean lat.: 3375.847ms, rate sampling interval: 12173ms
  Thread calibration: mean lat.: 3539.966ms, rate sampling interval: 12574ms
  Thread calibration: mean lat.: 3774.293ms, rate sampling interval: 13508ms
  Thread calibration: mean lat.: 3875.348ms, rate sampling interval: 12951ms
  Thread calibration: mean lat.: 3964.979ms, rate sampling interval: 14467ms
  Thread calibration: mean lat.: 3797.116ms, rate sampling interval: 13574ms
  Thread calibration: mean lat.: 3546.717ms, rate sampling interval: 14196ms
  Thread calibration: mean lat.: 3957.637ms, rate sampling interval: 14237ms
  Thread calibration: mean lat.: 3540.123ms, rate sampling interval: 13934ms
  Thread calibration: mean lat.: 3785.485ms, rate sampling interval: 14819ms
  Thread calibration: mean lat.: 3440.034ms, rate sampling interval: 13598ms
  Thread calibration: mean lat.: 3296.908ms, rate sampling interval: 11796ms
  Thread calibration: mean lat.: 3252.971ms, rate sampling interval: 12222ms
  Thread calibration: mean lat.: 4197.191ms, rate sampling interval: 14032ms
  Thread calibration: mean lat.: 4579.843ms, rate sampling interval: 15187ms
  Thread calibration: mean lat.: 4142.011ms, rate sampling interval: 14409ms
  Thread calibration: mean lat.: 1766.714ms, rate sampling interval: 6352ms
  Thread calibration: mean lat.: 3460.037ms, rate sampling interval: 12476ms
  Thread calibration: mean lat.: 4059.598ms, rate sampling interval: 14082ms
  Thread calibration: mean lat.: 3170.373ms, rate sampling interval: 12804ms
  Thread calibration: mean lat.: 2981.768ms, rate sampling interval: 10739ms
  Thread calibration: mean lat.: 4398.166ms, rate sampling interval: 14458ms
  Thread calibration: mean lat.: 3727.938ms, rate sampling interval: 13516ms
  Thread calibration: mean lat.: 3956.755ms, rate sampling interval: 14237ms
  Thread calibration: mean lat.: 3474.143ms, rate sampling interval: 12492ms
  Thread calibration: mean lat.: 3591.645ms, rate sampling interval: 14516ms
  Thread calibration: mean lat.: 4059.589ms, rate sampling interval: 14729ms
  Thread calibration: mean lat.: 3490.553ms, rate sampling interval: 13271ms
  Thread calibration: mean lat.: 4424.351ms, rate sampling interval: 14204ms
  Thread calibration: mean lat.: 4023.327ms, rate sampling interval: 14409ms
  Thread calibration: mean lat.: 3975.404ms, rate sampling interval: 14278ms
  Thread calibration: mean lat.: 3495.836ms, rate sampling interval: 12525ms
  Thread calibration: mean lat.: 3955.968ms, rate sampling interval: 14229ms
  Thread calibration: mean lat.: 4627.015ms, rate sampling interval: 14737ms
  Thread calibration: mean lat.: 4308.750ms, rate sampling interval: 15163ms
  Thread calibration: mean lat.: 3918.786ms, rate sampling interval: 14942ms
  Thread calibration: mean lat.: 4268.777ms, rate sampling interval: 15032ms
  Thread calibration: mean lat.: 3803.140ms, rate sampling interval: 14671ms
  Thread calibration: mean lat.: 4356.995ms, rate sampling interval: 14508ms
  Thread calibration: mean lat.: 4092.111ms, rate sampling interval: 14704ms
  Thread calibration: mean lat.: 4607.828ms, rate sampling interval: 15073ms
  Thread calibration: mean lat.: 3399.988ms, rate sampling interval: 13565ms
  Thread calibration: mean lat.: 3991.223ms, rate sampling interval: 13803ms
  Thread calibration: mean lat.: 3961.274ms, rate sampling interval: 13762ms
  Thread calibration: mean lat.: 4115.186ms, rate sampling interval: 14721ms
  Thread calibration: mean lat.: 3809.865ms, rate sampling interval: 13492ms
  Thread calibration: mean lat.: 4133.161ms, rate sampling interval: 14761ms
  Thread calibration: mean lat.: 4003.603ms, rate sampling interval: 14303ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    25.67s    11.52s   50.59s    58.47%
    Req/Sec   598.77    262.82     1.55k    90.38%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%   25.02s 
 75.000%   35.55s 
 90.000%   41.84s 
 99.000%   47.28s 
 99.900%   49.45s 
 99.990%   50.40s 
 99.999%   50.56s 
100.000%   50.63s 

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

    3590.143     0.000000            4         1.00
   10452.991     0.100000       179197         1.11
   14008.319     0.200000       358090         1.25
   17514.495     0.300000       536732         1.43
   20987.903     0.400000       716055         1.67
   25018.367     0.500000       894809         2.00
   27115.519     0.550000       984209         2.22
   29245.439     0.600000      1073349         2.50
   31326.207     0.650000      1162575         2.86
   33357.823     0.700000      1251997         3.33
   35553.279     0.750000      1342170         4.00
   36634.623     0.775000      1387241         4.44
   37650.431     0.800000      1431544         5.00
   38666.239     0.825000      1475908         5.71
   39714.815     0.850000      1521040         6.67
   40763.391     0.875000      1566272         8.00
   41287.679     0.887500      1588353         8.89
   41844.735     0.900000      1610540        10.00
   42401.791     0.912500      1632837        11.43
   42958.847     0.925000      1654402        13.33
   43581.439     0.937500      1677044        16.00
   43909.119     0.943750      1688678        17.78
   44236.799     0.950000      1699624        20.00
   44630.015     0.956250      1710995        22.86
   45023.231     0.962500      1721663        26.67
   45449.215     0.968750      1733155        32.00
   45645.823     0.971875      1738314        35.56
   45875.199     0.975000      1744239        40.00
   46104.575     0.978125      1749952        45.71
   46366.719     0.981250      1755552        53.33
   46628.863     0.984375      1760696        64.00
   46792.703     0.985938      1763520        71.11
   46989.311     0.987500      1766475        80.00
   47153.151     0.989062      1768952        91.43
   47382.527     0.990625      1771990       106.67
   47611.903     0.992188      1774776       128.00
   47710.207     0.992969      1775930       142.22
   47841.279     0.993750      1777353       160.00
   47972.351     0.994531      1778726       182.86
   48136.191     0.995313      1780300       213.33
   48267.263     0.996094      1781525       256.00
   48365.567     0.996484      1782440       284.44
   48431.103     0.996875      1782935       320.00
   48562.175     0.997266      1783720       365.71
   48693.247     0.997656      1784338       426.67
   48857.087     0.998047      1785059       512.00
   48955.391     0.998242      1785386       568.89
   49086.463     0.998437      1785743       640.00
   49217.535     0.998633      1786121       731.43
   49315.839     0.998828      1786397       853.33
   49446.911     0.999023      1786747      1024.00
   49512.447     0.999121      1786898      1137.78
   49610.751     0.999219      1787107      1280.00
   49676.287     0.999316      1787261      1462.86
   49741.823     0.999414      1787423      1706.67
   49840.127     0.999512      1787627      2048.00
   49905.663     0.999561      1787706      2275.56
   49971.199     0.999609      1787780      2560.00
   50036.735     0.999658      1787877      2925.71
   50102.271     0.999707      1787960      3413.33
   50167.807     0.999756      1788044      4096.00
   50200.575     0.999780      1788082      4551.11
   50233.343     0.999805      1788120      5120.00
   50298.879     0.999829      1788200      5851.43
   50331.647     0.999854      1788237      6826.67
   50364.415     0.999878      1788272      8192.00
   50364.415     0.999890      1788272      9102.22
   50397.183     0.999902      1788301     10240.00
   50429.951     0.999915      1788335     11702.86
   50429.951     0.999927      1788335     13653.33
   50462.719     0.999939      1788365     16384.00
   50495.487     0.999945      1788398     18204.44
   50495.487     0.999951      1788398     20480.00
   50495.487     0.999957      1788398     23405.71
   50528.255     0.999963      1788426     27306.67
   50528.255     0.999969      1788426     32768.00
   50528.255     0.999973      1788426     36408.89
   50528.255     0.999976      1788426     40960.00
   50528.255     0.999979      1788426     46811.43
   50561.023     0.999982      1788450     54613.33
   50561.023     0.999985      1788450     65536.00
   50561.023     0.999986      1788450     72817.78
   50561.023     0.999988      1788450     81920.00
   50561.023     0.999989      1788450     93622.86
   50561.023     0.999991      1788450    109226.67
   50593.791     0.999992      1788460    131072.00
   50593.791     0.999993      1788460    145635.56
   50593.791     0.999994      1788460    163840.00
   50593.791     0.999995      1788460    187245.71
   50593.791     0.999995      1788460    218453.33
   50593.791     0.999996      1788460    262144.00
   50593.791     0.999997      1788460    291271.11
   50593.791     0.999997      1788460    327680.00
   50593.791     0.999997      1788460    374491.43
   50593.791     0.999998      1788460    436906.67
   50626.559     0.999998      1788464    524288.00
   50626.559     1.000000      1788464          inf
#[Mean    =    25674.708, StdDeviation   =    11515.121]
#[Max     =    50593.792, Total count    =      1788464]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  2148070 requests in 1.00m, 188.47MB read
Requests/sec:  35804.36
Transfer/sec:      3.14MB
```
![GRAPH](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/fixed/get.png)

Видно, что до 20 тысяч запросов в секунд кластер выдерживает нагрузку, но с 40 тысяч кластер уже не выдерживает и начинаются задержки от 10 с.

## CPU
![CPU](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/fixed/get-cpu.svg)

## CPU threads
![CPU](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/fixed/get-cpu-threaded.svg)

Большая часть времени как и ожидалось уходит на получение итератора, где происходит активное чтение с диска.

## ALLOC

![ALLOC](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/fixed/get-alloc-cpu.svg)

Для проксирования выделялось ~50% памяти. Для получения итератора ~23%
## LOCK

![LOCK](https://raw.githubusercontent.com/s3ponia/2020-highload-dht/master/test-results/task4/fixed/get-lock-cpu.svg)

Воркеры блокируют очередь и берут задачу на обработку. При вызове get добавляется задача в очередь, а значит очередь опять блокируется.


