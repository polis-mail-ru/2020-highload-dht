# Оценка времени отклика сервера в режиме симулирования запросов (wrk + async-profiler)

### Сведения о системе
| | |
|-|-|
| ОС Ubuntu | 18.04 LTS x64-bit |
| Процессор | Intel(R) Celeron(R) N4000 CPU @ 1.10GHz |
| Объём RAM | 8 ГБ |
| Количество ядер ЦПУ | 2 |


В ходе мониторинга производительности сервера (хост по адресу http://127.0.0.1:8080) с использованием инструмента <strong><em>wrk</em></strong> получены результирующие задержки обработки следующих запросов:<br/>
 <strong>1) wrk -t1 -c1 -d5m -R2000 --latency http://127.0.0.1:8080/ <br/>
2) <em>(PUT /v0/entity?id=<ID></em>) &nbsp; wrk -t1 -c1 -d5m -s src/profiling/wrk_scripts/put.lua -R2000 --latency http://127.0.0.1:8080/<br/>
3) <em>(GET /v0/entity?id=<ID></em>) &nbsp; wrk -t1 -c1 -d5m -s src/profiling/wrk_scripts/get.lua -R2000 --latency http://127.0.0.1:8080/<br/> 
4) <em>(DELETE /v0/entity?id=<ID></em>)&nbsp; wrk -t1 -c1 -d5m -s src/profiling/wrk_scripts/delete.lua -R2000 --latency http://127.0.0.1:8080/ </strong> <br/>

Основные параметры создания и передачи запросов через wrk (идентичны для всех выеприведённых операций) 

``` wrk -t1 -c1 -d5m -R2000 http://127.0.0.1:8080/ ```

Сводки наблюдений по каждой из указанных операций приведены далее.<br/>  
### 1) wrk -t1 -c1 -d5m -R2000 --latency http://127.0.0.1:8080/ <br/>

Консольный вывод:
```
max@max-Inspiron-15-3573:~/asynctool/async-profiler-1.8.1-linux-x64$ wrk -t1 -c1 -d3m -R2000 --latency http://localhost:8080
Running 3m test @ http://localhost:8080
  1 threads and 1 connections
  Thread calibration: mean lat.: 1.319ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.40ms    1.59ms  39.62ms   93.96%
    Req/Sec     2.12k   482.56     8.00k    88.36%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.09ms
 75.000%    1.57ms
 90.000%    2.03ms
 99.000%    9.04ms
 99.900%   15.57ms
 99.990%   29.69ms
 99.999%   38.43ms
100.000%   39.65ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.049     0.000000            5         1.00
       0.365     0.100000        34105         1.11
       0.610     0.200000        68121         1.25
       0.818     0.300000       102018         1.43
       0.962     0.400000       136025         1.67
       1.090     0.500000       170135         2.00
       1.154     0.550000       187087         2.22
       1.237     0.600000       204018         2.50
       1.344     0.650000       221095         2.86
       1.454     0.700000       238034         3.33
       1.568     0.750000       255076         4.00
       1.623     0.775000       263579         4.44
       1.680     0.800000       272062         5.00
       1.745     0.825000       280577         5.71
       1.812     0.850000       288968         6.67
       1.899     0.875000       297525         8.00
       1.952     0.887500       301761         8.89
       2.027     0.900000       306003        10.00
       2.163     0.912500       310249        11.43
       2.369     0.925000       314467        13.33
       2.869     0.937500       318717        16.00
       3.245     0.943750       320843        17.78
       3.669     0.950000       322965        20.00
       4.143     0.956250       325100        22.86
       4.671     0.962500       327213        26.67
       5.319     0.968750       329340        32.00
       5.683     0.971875       330410        35.56
       6.079     0.975000       331463        40.00
       6.527     0.978125       332535        45.71
       7.023     0.981250       333592        53.33
       7.599     0.984375       334658        64.00
       7.927     0.985938       335180        71.11
       8.319     0.987500       335714        80.00
       8.759     0.989062       336252        91.43
       9.231     0.990625       336776       106.67
       9.783     0.992188       337308       128.00
      10.071     0.992969       337570       142.22
      10.399     0.993750       337842       160.00
      10.759     0.994531       338106       182.86
      11.175     0.995313       338370       213.33
      11.623     0.996094       338636       256.00
      11.903     0.996484       338768       284.44
      12.215     0.996875       338900       320.00
      12.543     0.997266       339032       365.71
      12.959     0.997656       339164       426.67
      13.471     0.998047       339297       512.00
      13.791     0.998242       339364       568.89
      14.119     0.998437       339429       640.00
      14.511     0.998633       339496       731.43
      15.031     0.998828       339562       853.33
      15.663     0.999023       339629      1024.00
      16.055     0.999121       339663      1137.78
      16.495     0.999219       339697      1280.00
      16.975     0.999316       339728      1462.86
      17.711     0.999414       339761      1706.67
      18.607     0.999512       339795      2048.00
      19.071     0.999561       339812      2275.56
      19.679     0.999609       339828      2560.00
      20.175     0.999658       339844      2925.71
      21.055     0.999707       339861      3413.33
      22.207     0.999756       339879      4096.00
      23.295     0.999780       339886      4551.11
      24.431     0.999805       339894      5120.00
      25.743     0.999829       339903      5851.43
      26.895     0.999854       339911      6826.67
      28.399     0.999878       339919      8192.00
      29.263     0.999890       339923      9102.22
      29.935     0.999902       339927     10240.00
      30.783     0.999915       339931     11702.86
      31.727     0.999927       339936     13653.33
      32.175     0.999939       339940     16384.00
      32.447     0.999945       339942     18204.44
      32.623     0.999951       339944     20480.00
      33.503     0.999957       339946     23405.71
      34.399     0.999963       339948     27306.67
      35.295     0.999969       339950     32768.00
      35.743     0.999973       339951     36408.89
      36.191     0.999976       339952     40960.00
      36.639     0.999979       339953     46811.43
      37.087     0.999982       339954     54613.33
      37.535     0.999985       339955     65536.00
      37.983     0.999986       339956     72817.78
      37.983     0.999988       339956     81920.00
      38.431     0.999989       339957     93622.86
      38.431     0.999991       339957    109226.67
      38.879     0.999992       339958    131072.00
      38.879     0.999993       339958    145635.56
      38.879     0.999994       339958    163840.00
      39.327     0.999995       339959    187245.71
      39.327     0.999995       339959    218453.33
      39.327     0.999996       339959    262144.00
      39.327     0.999997       339959    291271.11
      39.327     0.999997       339959    327680.00
      39.647     0.999997       339960    374491.43
      39.647     1.000000       339960          inf
#[Mean    =        1.402, StdDeviation   =        1.585]
#[Max     =       39.616, Total count    =       339960]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  359967 requests in 3.00m, 24.37MB read
  Non-2xx or 3xx responses: 359967
Requests/sec:   1999.82
Transfer/sec:    138.66KB

```
Приведённая статистика свидетельствует о достижении серверной нагрузки на уровне 2000 запросов в секунду. Средняя длительность задержек в процессе обработки запроса и возврата данных клиенту установлена равной 1.4 ms при отклонении, достигающем 93%. Оценка времени ожидания ответа на основе квантильного распределения результатов позволяет утверждать, что в 99,99% случаев длительность поступления данных с локального сервера не превышала 30 мс. При этом оставшаяся доля операций (0.01%) приходится на запросы, формирование ответа на которые отмечено ощутимо большей задержкой, что находит отражение в оценке длительности обработки и отправки результата запроса в пределах 39 мс.  <br/>                    

![Screenshot from 2020-09-27 00-34-23](https://user-images.githubusercontent.com/55311053/94411885-c86e1a80-0181-11eb-8c58-06519574e2ce.png)
<p align="center">Рис.1. Визуализация эффектов нагрузки на ресурсы ЦПУ и RAM в интерфейсе VisualVM</p>

Данный рисунок иллюстрирует динамику использования ресурсов ЦПУ и RAM в ходе обработки запросов, поступающих в режиме симуляции нагрузки. Как следует из приведённых графиков, запуск wrk споровождался резким повышением амплитуды кривой, описывающей интенсивность операций на каждом из вышеназванных устройств. Реализация клиент-серверного взаимодействия на основе единственного соединения проявляется в регулярности всплесков и падений активности, связанной с выделением динамической памяти для обработки подаваемых на входной порт сервера запросов. Для графика флуктуаций захвата ЦПУ характерны локальные отклонения от медианного значения, близкого к 10% от агрегированного числа квантов процессорного времени.  <br/>        

Итоги профилирования вычислительных процессов на базе сервера ```(./profiler.sh -d 60 -e cpu -f  /tmp/flameoutput_cpu.svg <server_runner_pid>)``` в виде экземпляра <em>flamegraph:</em><br/>

![kindle1](https://user-images.githubusercontent.com/55311053/94413196-6ca49100-0183-11eb-9ab5-9a9bd68f951f.jpg)
<p align="center">Рис.2. Flamegraph выделения ресурса ЦПУ</p>

Анализируя полученный профиль, можно констатировать, что основными факторами, обеспечившими нагрузку на ЦПУ, оказались операции с буфером данных и сокетом на рабочем порту сервера. На поддержание функций селектора было затрачено около 88% процессорного времени, выделенного на осуществление операций локального сервера.<br/>           
  
Итоги профилирования процессов аллокации ```(/profiler.sh -d 60 -e alloc -f  /tmp/flameoutput_allocation.svg <server_runner_pid>)```

![flameoutput11](https://user-images.githubusercontent.com/55311053/95015349-ff966d00-0654-11eb-8068-b38304bf19d7.jpg)
<p align="center">Рис.3. Flamegraph выделения RAM</p>

Визуализация использования RAM подтверждает превалирирование операций с буфером среди факторов, оказывающих влияние на уровень и интенсивность нагрузки.   

### 2) <em>(PUT /v0/entity?id=<ID></em>) &nbsp; wrk -t1 -c1 -d5m -s src/profiling/wrk_scripts/put.lua -R2000 --latency http://127.0.0.1:8080/<br/>

Консольный вывод wrk:<br/>
```
max@max-Inspiron-15-3573:~/hackdht$ sudo wrk -t1 -c1 -d5m -s src/profiling/wrk_scripts/put.lua -R2000 --latency http://127.0.0.1:8080/
Running 5m test @ http://127.0.0.1:8080/
  1 threads and 1 connections
  Thread calibration: mean lat.: 2.696ms, rate sampling interval: 12ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.95ms    2.71ms  54.34ms   92.25%
    Req/Sec     2.10k   526.28     7.17k    83.01%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.25ms
 75.000%    1.90ms
 90.000%    3.88ms
 99.000%   13.61ms
 99.900%   31.82ms
 99.990%   43.46ms
 99.999%   53.09ms
100.000%   54.37ms

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.085     0.000000            4         1.00
       0.449     0.100000        58151         1.11
       0.714     0.200000       116191         1.25
       0.928     0.300000       174301         1.43
       1.086     0.400000       232254         1.67
       1.246     0.500000       290193         2.00
       1.350     0.550000       319044         2.22
       1.479     0.600000       348151         2.50
       1.609     0.650000       377041         2.86
       1.752     0.700000       406106         3.33
       1.900     0.750000       435112         4.00
       1.996     0.775000       449618         4.44
       2.137     0.800000       464112         5.00
       2.361     0.825000       478540         5.71
       2.675     0.850000       493020         6.67
       3.197     0.875000       507532         8.00
       3.513     0.887500       514765         8.89
       3.875     0.900000       522026        10.00
       4.291     0.912500       529305        11.43
       4.775     0.925000       536524        13.33
       5.387     0.937500       543783        16.00
       5.759     0.943750       547408        17.78
       6.179     0.950000       551013        20.00
       6.667     0.956250       554637        22.86
       7.271     0.962500       558270        26.67
       7.999     0.968750       561895        32.00
       8.455     0.971875       563710        35.56
       8.975     0.975000       565511        40.00
       9.591     0.978125       567339        45.71
      10.311     0.981250       569141        53.33
      11.183     0.984375       570955        64.00
      11.695     0.985938       571855        71.11
      12.327     0.987500       572765        80.00
      13.087     0.989062       573668        91.43
      14.007     0.990625       574572       106.67
      15.279     0.992188       575487       128.00
      16.023     0.992969       575930       142.22
      16.895     0.993750       576383       160.00
      17.967     0.994531       576842       182.86
      19.135     0.995313       577294       213.33
      20.479     0.996094       577748       256.00
      21.295     0.996484       577971       284.44
      22.175     0.996875       578199       320.00
      23.055     0.997266       578427       365.71
      24.239     0.997656       578650       426.67
      25.711     0.998047       578877       512.00
      26.511     0.998242       578989       568.89
      27.679     0.998437       579102       640.00
      28.943     0.998633       579217       731.43
      30.447     0.998828       579330       853.33
      32.015     0.999023       579442      1024.00
      32.799     0.999121       579499      1137.78
      33.439     0.999219       579555      1280.00
      34.143     0.999316       579612      1462.86
      34.847     0.999414       579670      1706.67
      35.839     0.999512       579726      2048.00
      36.415     0.999561       579755      2275.56
      36.991     0.999609       579784      2560.00
      37.663     0.999658       579812      2925.71
      38.335     0.999707       579840      3413.33
      39.679     0.999756       579867      4096.00
      40.223     0.999780       579881      4551.11
      40.703     0.999805       579896      5120.00
      41.023     0.999829       579910      5851.43
      41.663     0.999854       579924      6826.67
      42.687     0.999878       579938      8192.00
      43.167     0.999890       579945      9102.22
      43.583     0.999902       579952     10240.00
      44.383     0.999915       579959     11702.86
      45.759     0.999927       579966     13653.33
      46.591     0.999939       579973     16384.00
      47.071     0.999945       579978     18204.44
      47.231     0.999951       579980     20480.00
      47.327     0.999957       579984     23405.71
      47.455     0.999963       579987     27306.67
      48.639     0.999969       579991     32768.00
      49.439     0.999973       579993     36408.89
      49.855     0.999976       579994     40960.00
      50.687     0.999979       579996     46811.43
      51.487     0.999982       579998     54613.33
      52.287     0.999985       580000     65536.00
      52.703     0.999986       580001     72817.78
      52.703     0.999988       580001     81920.00
      53.087     0.999989       580002     93622.86
      53.247     0.999991       580003    109226.67
      53.439     0.999992       580004    131072.00
      53.471     0.999993       580005    145635.56
      53.471     0.999994       580005    163840.00
      53.471     0.999995       580005    187245.71
      53.823     0.999995       580006    218453.33
      53.823     0.999996       580006    262144.00
      54.175     0.999997       580007    291271.11
      54.175     0.999997       580007    327680.00
      54.175     0.999997       580007    374491.43
      54.175     0.999998       580007    436906.67
      54.175     0.999998       580007    524288.00
      54.367     0.999998       580008    582542.22
      54.367     1.000000       580008          inf
#[Mean    =        1.955, StdDeviation   =        2.713]
#[Max     =       54.336, Total count    =       580008]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  599997 requests in 5.00m, 38.34MB read
Requests/sec:   1999.99
Transfer/sec:    130.86KB

```
Согласно выходным данным текущей серии, для получения результата обработки 99,99% запросов, созданных с использованием <em>wrk</em> понадобилось около 43 мс, в то время как длительность ожидания оставшихся оказалась почти на 11 мс больше. В среднем задержка ответа составила приблизительно 2 нс при стандартном отклонении на уровне 92%.           

![flameoutput_putcpu_1](https://user-images.githubusercontent.com/55311053/95018467-a7696600-0668-11eb-9481-6943f104fddf.jpg)
<p align="center">Рис.4. Flamegraph выделения ресурса ЦПУ в рамках симуляции PUT-запросов</p>
 
![flameoutput_putalloc_1](https://user-images.githubusercontent.com/55311053/95018466-a6383900-0668-11eb-897a-7be0bf318df5.jpg)
<p align="center">Рис.5. Flamegraph выделения RAM в рамках симуляции PUT-запросов</p>

На основе анализа графического представления операций на уровне ЦПУ и ОЗУ можно отметить, что ключевой вклад в агрегированную нагрузку на процессор обеспечили операции считывания и преобразования данных, поступивших в буфер. В структуре аллокаций следует выделить как предоставление ресурса памяти под буфер, так и её расход на поддержку вычислений с элементами байтовых массивов<br/>.          


### 3) <em>(GET /v0/entity?id=<ID></em>) &nbsp; wrk -t1 -c1 -d5m -s src/profiling/wrk_scripts/get.lua -R2000 --latency http://127.0.0.1:8080/<br/>

Консольный вывод wrk:<br/>
```
max@max-Inspiron-15-3573:~/hackdht$ sudo wrk -t1 -c1 -d5m -s src/profiling/wrk_scripts/get.lua -R2000 --latency http://127.0.0.1:8080/
Running 5m test @ http://127.0.0.1:8080/
  1 threads and 1 connections
  Thread calibration: mean lat.: 1.814ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     7.82ms   61.97ms   1.00s    98.92%
    Req/Sec     2.15k   676.37    12.00k    80.91%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.31ms
 75.000%    2.15ms
 90.000%    4.47ms
 99.000%  146.69ms
 99.900%  918.53ms
 99.990%  993.28ms
 99.999%    1.00s 
100.000%    1.00s 

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.071     0.000000            2         1.00
       0.436     0.100000        58108         1.11
       0.737     0.200000       116061         1.25
       0.939     0.300000       174023         1.43
       1.110     0.400000       232167         1.67
       1.310     0.500000       290005         2.00
       1.442     0.550000       319017         2.22
       1.582     0.600000       348135         2.50
       1.720     0.650000       377046         2.86
       1.873     0.700000       406121         3.33
       2.149     0.750000       435065         4.00
       2.361     0.775000       449559         4.44
       2.641     0.800000       464038         5.00
       2.993     0.825000       478494         5.71
       3.407     0.850000       493006         6.67
       3.889     0.875000       507528         8.00
       4.167     0.887500       514781         8.89
       4.475     0.900000       522041        10.00
       4.843     0.912500       529266        11.43
       5.299     0.925000       536500        13.33
       5.927     0.937500       543766        16.00
       6.351     0.943750       547392        17.78
       6.887     0.950000       551010        20.00
       7.563     0.956250       554626        22.86
       8.407     0.962500       558252        26.67
       9.535     0.968750       561893        32.00
      10.247     0.971875       563682        35.56
      11.151     0.975000       565507        40.00
      12.319     0.978125       567310        45.71
      14.007     0.981250       569123        53.33
      17.151     0.984375       570928        64.00
      20.655     0.985938       571837        71.11
      26.415     0.987500       572741        80.00
      58.239     0.989062       573647        91.43
     194.815     0.990625       574553       106.67
     333.567     0.992188       575459       128.00
     398.591     0.992969       575913       142.22
     462.079     0.993750       576366       160.00
     527.871     0.994531       576822       182.86
     596.479     0.995313       577273       213.33
     665.599     0.996094       577726       256.00
     696.319     0.996484       577952       284.44
     730.623     0.996875       578179       320.00
     765.951     0.997266       578409       365.71
     798.207     0.997656       578632       426.67
     833.023     0.998047       578858       512.00
     851.455     0.998242       578973       568.89
     868.351     0.998437       579084       640.00
     886.271     0.998633       579199       731.43
     904.703     0.998828       579311       853.33
     921.599     0.999023       579428      1024.00
     928.767     0.999121       579483      1137.78
     937.471     0.999219       579540      1280.00
     944.639     0.999316       579597      1462.86
     953.343     0.999414       579653      1706.67
     960.511     0.999512       579711      2048.00
     963.583     0.999561       579737      2275.56
     968.703     0.999609       579764      2560.00
     973.311     0.999658       579795      2925.71
     976.895     0.999707       579823      3413.33
     980.991     0.999756       579851      4096.00
     983.039     0.999780       579863      4551.11
     985.599     0.999805       579877      5120.00
     988.671     0.999829       579893      5851.43
     990.207     0.999854       579908      6826.67
     991.743     0.999878       579920      8192.00
     992.767     0.999890       579928      9102.22
     993.791     0.999902       579934     10240.00
     995.327     0.999915       579943     11702.86
     996.351     0.999927       579948     13653.33
     997.375     0.999939       579955     16384.00
     998.399     0.999945       579961     18204.44
     998.911     0.999951       579963     20480.00
     999.423     0.999957       579967     23405.71
     999.935     0.999963       579970     27306.67
    1000.447     0.999969       579975     32768.00
    1000.447     0.999973       579975     36408.89
    1000.959     0.999976       579978     40960.00
    1000.959     0.999979       579978     46811.43
    1001.471     0.999982       579982     54613.33
    1001.471     0.999985       579982     65536.00
    1001.983     0.999986       579985     72817.78
    1001.983     0.999988       579985     81920.00
    1001.983     0.999989       579985     93622.86
    1001.983     0.999991       579985    109226.67
    1002.495     0.999992       579990    131072.00
    1002.495     1.000000       579990          inf
#[Mean    =        7.816, StdDeviation   =       61.975]
#[Max     =     1001.984, Total count    =       579990]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  599998 requests in 5.00m, 38.80MB read
  Non-2xx or 3xx responses: 1
Requests/sec:   1999.94
Transfer/sec:    132.45KB
```
Работа сервера в ходе последовательной обработки GET-запросов сопровождалась существенным увеличением периода ожидания: длительность средней задержки в сравнении с оценкой для запросов на вставку возросла приблизительно 4 раза при близком к 100% уровню стандартного отклонения<br/>. При этом получение ответа от сервера в 99,99% случаях заняло более 990 мс, а наименьшее быстродействие было зафиксировано в ходе обработки оставшихся 0,1% (с доведением времени ожидания данных клиентом до 1 с).  
![flameoutput_getcpu_1](https://user-images.githubusercontent.com/55311053/95018726-2c08b400-066a-11eb-87d4-d03f3dc5f155.jpg)
<p align="center">Рис.6. Flamegraph выделения ресурса ЦПУ в рамках симуляции GET-запросов</p>

![flameoutput_getalloc_2](https://user-images.githubusercontent.com/55311053/95018724-2ad78700-066a-11eb-8751-1829ccbc1c3a.jpg)
<p align="center">Рис.7. Flamegraph выделения RAM в рамках симуляции GET-запросов</p>
Графы выделения вычислительного ресурса, полученные для данной и предыдущей серий, демонстрируют предельное сходство. Ключевые отличия, обусловливающие кратное снижение производительности при выполнении операций на получение данных из БД, становятся очевидными при рассмотрении flame-графа на рис. 7. С его помощью удаётся установить повышение числа операций, связанных с выделением ОП. Так, наряду с расходом памяти на операции с байтовыми массивами часть ресурса выделяется на обеспечение конвертации байтов в строки для возврата значений по искомым ключам.     

  
### 4) <em>(DELETE /v0/entity?id=<ID></em>) &nbsp; wrk -t1 -c1 -d5m -s src/profiling/wrk_scripts/delete.lua -R2000 --latency http://127.0.0.1:8080/<br/>
  
Консольный вывод wrk:<br/> 
```
max@max-Inspiron-15-3573:~/hackdht$ sudo wrk -t1 -c1 -d5m -s src/profiling/wrk_scripts/delete.lua -R2000 --latency http://127.0.0.1:8080/
Running 5m test @ http://127.0.0.1:8080/
  1 threads and 1 connections
  Thread calibration: mean lat.: 1.708ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     5.47ms   48.07ms   1.00s    99.33%
    Req/Sec     2.13k   619.93    10.20k    81.95%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.26ms
 75.000%    1.95ms
 90.000%    4.14ms
 99.000%   23.82ms
 99.900%  861.18ms
 99.990%  988.16ms
 99.999%    1.00s 
100.000%    1.00s 

  Detailed Percentile spectrum:
       Value   Percentile   TotalCount 1/(1-Percentile)

       0.082     0.000000            3         1.00
       0.448     0.100000        58171         1.11
       0.722     0.200000       116051         1.25
       0.936     0.300000       174153         1.43
       1.094     0.400000       232075         1.67
       1.265     0.500000       290132         2.00
       1.380     0.550000       319180         2.22
       1.511     0.600000       348219         2.50
       1.646     0.650000       377195         2.86
       1.787     0.700000       406113         3.33
       1.945     0.750000       435070         4.00
       2.065     0.775000       449682         4.44
       2.251     0.800000       464074         5.00
       2.501     0.825000       478568         5.71
       2.881     0.850000       493021         6.67
       3.431     0.875000       507535         8.00
       3.761     0.887500       514747         8.89
       4.135     0.900000       522038        10.00
       4.571     0.912500       529267        11.43
       5.107     0.925000       536507        13.33
       5.819     0.937500       543739        16.00
       6.263     0.943750       547394        17.78
       6.787     0.950000       550997        20.00
       7.415     0.956250       554618        22.86
       8.223     0.962500       558243        26.67
       9.295     0.968750       561876        32.00
       9.951     0.971875       563680        35.56
      10.775     0.975000       565498        40.00
      11.799     0.978125       567312        45.71
      13.191     0.981250       569117        53.33
      15.239     0.984375       570929        64.00
      16.751     0.985938       571842        71.11
      18.655     0.987500       572744        80.00
      21.423     0.989062       573647        91.43
      25.823     0.990625       574551       106.67
      35.455     0.992188       575457       128.00
      45.919     0.992969       575910       142.22
      99.327     0.993750       576364       160.00
     210.687     0.994531       576819       182.86
     325.631     0.995313       577270       213.33
     439.551     0.996094       577724       256.00
     494.335     0.996484       577949       284.44
     552.959     0.996875       578176       320.00
     608.767     0.997266       578403       365.71
     663.039     0.997656       578630       426.67
     720.383     0.998047       578856       512.00
     749.055     0.998242       578969       568.89
     781.311     0.998437       579082       640.00
     808.447     0.998633       579196       731.43
     833.023     0.998828       579309       853.33
     864.255     0.999023       579422      1024.00
     877.055     0.999121       579479      1137.78
     891.391     0.999219       579535      1280.00
     903.679     0.999316       579592      1462.86
     918.527     0.999414       579649      1706.67
     931.839     0.999512       579705      2048.00
     939.007     0.999561       579734      2275.56
     946.687     0.999609       579764      2560.00
     954.879     0.999658       579790      2925.71
     963.071     0.999707       579821      3413.33
     970.239     0.999756       579847      4096.00
     972.799     0.999780       579861      4551.11
     974.847     0.999805       579877      5120.00
     978.943     0.999829       579889      5851.43
     982.015     0.999854       579906      6826.67
     985.087     0.999878       579918      8192.00
     987.135     0.999890       579926      9102.22
     988.671     0.999902       579932     10240.00
     990.719     0.999915       579939     11702.86
     992.255     0.999927       579946     13653.33
     994.303     0.999939       579955     16384.00
     995.327     0.999945       579957     18204.44
     996.351     0.999951       579962     20480.00
     996.863     0.999957       579966     23405.71
     997.375     0.999963       579967     27306.67
     998.399     0.999969       579971     32768.00
     998.911     0.999973       579974     36408.89
     998.911     0.999976       579974     40960.00
     999.935     0.999979       579977     46811.43
    1000.447     0.999982       579980     54613.33
    1000.447     0.999985       579980     65536.00
    1000.959     0.999986       579982     72817.78
    1000.959     0.999988       579982     81920.00
    1000.959     0.999989       579982     93622.86
    1001.471     0.999991       579983    109226.67
    1001.983     0.999992       579985    131072.00
    1001.983     0.999993       579985    145635.56
    1001.983     0.999994       579985    163840.00
    1001.983     0.999995       579985    187245.71
    1002.495     0.999995       579988    218453.33
    1002.495     1.000000       579988          inf
#[Mean    =        5.473, StdDeviation   =       48.068]
#[Max     =     1001.984, Total count    =       579988]
#[Buckets =           27, SubBuckets     =         2048]
----------------------------------------------------------
  599996 requests in 5.00m, 38.91MB read
Requests/sec:   1999.99
Transfer/sec:    132.81KB

```
Оценка средней задержки при обработке запросов на удаление незначительно отличается от значения того же параметра в серии с запросами на получение данных: для выполнения одной операции серверу требовалось около 5,5 мс (при 7,8 на чтение и возврат значения) с учётом близкого с идентичному отклонения. Сходство двух операций обнаруживается и в квантильном распределении времён ожидания, вплоть до установления равной максимальной длительности ответа (1 с).         
![flameoutput_deletecpu_1](https://user-images.githubusercontent.com/55311053/95018872-ebf60100-066a-11eb-936c-f0b9071eed4f.jpg)
<p align="center">Рис.8. Flamegraph выделения ресурса ЦПУ в рамках симуляции DELETE-запросов</p>

![flameoutput_deletealloc_1](https://user-images.githubusercontent.com/55311053/95018870-eb5d6a80-066a-11eb-852d-fa49531464ba.jpg)
<p align="center">Рис.9. Flamegraph выделения RAM в рамках симуляции DELETE-запросов</p>

Анализ с применением графовых представлений позволяет проследить аналогии как в нагрузке на ЦПУ, так и в процессах аллокациях памяти. Сравнение графов выделения RAM свидетельствует о релевантности конвертации в качестве одного из основных факторов создания ресурсной нагрузки в контекстах добавления (изменения) и удаления записей в БД.  
