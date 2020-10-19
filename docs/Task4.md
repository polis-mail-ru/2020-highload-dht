# Шардирование

На данном этапе было реализовано горизонтальное масштабирование через поддержку кластерных конфигураций, состоящих из нескольких узлов, взаимодействующих друг с другом через реализованный HTTP API.

### Обстрел PUT-ами

Параметры запуска wrk
- 4 потока (worker'ы) отправляющие запросы
- 64 открытых соединения
- 20 000 запросов в секунду
- длительность 30 секунд

```text
Running 30s test @ http://127.0.0.1:8080/
  4 threads and 64 connections
  Thread calibration: mean lat.: 2380.018ms, rate sampling interval: 8024ms
  Thread calibration: mean lat.: 2471.412ms, rate sampling interval: 7606ms
  Thread calibration: mean lat.: 2371.558ms, rate sampling interval: 8032ms
  Thread calibration: mean lat.: 2379.675ms, rate sampling interval: 8028ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.90s     1.33s    4.16s    58.43%
    Req/Sec     6.26k   297.04     6.57k    62.50%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.90s 
 75.000%    3.07s 
 90.000%    3.74s 
 99.000%    4.09s 
 99.900%    4.13s 
 99.990%    4.15s 
 99.999%    4.16s 
100.000%    4.16s 
```

### Результаты с async profiler-а (CPU)
![Результаты с async profiler-а (CPU)](assets/stage_4/async_cpu_put.svg)

По сравнению с предыдущим этапом «башенок» на Flame Graph стало в три раза больше: теперь селекторы и потоки визуально разделились на три части — по числу нод в кластере.

### Результаты с async profiler-а (ALLOC)
![Результаты с async profiler-а (ALLOC)](assets/stage_4/async_alloc_put.svg)

Здесь аналогичная ситуация — элементов на графике также стало в три раза больше. У более толстых «башенок» селекторов отчетливо видно перенаправление запросов `ru/mail/polis/service/ServiceAsyncImpl.proxy`.

### Результаты с async profiler-a (LOCK)

![Результаты с async profiler-a (LOCK)](assets/stage_4/async_lock_put.svg)

На графе также, как и в предыдущем этапе, присутствуют блокировки потоков работы HTTP-сервера. Однако столбики из-за общения между нодами кластера теперь стали неоднородные.

### Обстрел GET-ами

Параметры запуска wrk
- 4 потока (worker'ы) отправляющие запросы
- 64 открытых соединений
- 20 000 запросов в секунду
- длительность 30 секунд

```text
Running 30s test @ http://127.0.0.1:8080/
  4 threads and 64 connections
  Thread calibration: mean lat.: 2.783ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.613ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.596ms, rate sampling interval: 10ms
  Thread calibration: mean lat.: 2.792ms, rate sampling interval: 10ms
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     4.07ms   11.42ms 128.90ms   94.62%
    Req/Sec     5.36k     1.18k   19.78k    90.47%
  Latency Distribution (HdrHistogram - Recorded Latency)
 50.000%    1.50ms
 75.000%    2.18ms
 90.000%    4.06ms
 99.000%   71.10ms
 99.900%  112.32ms
 99.990%  123.52ms
 99.999%  128.13ms
100.000%  128.96ms
```

### Результаты с async profiler-а (CPU)
![Результаты с async profiler-а (CPU)](assets/stage_4/async_cpu_get.svg)

Столбиков тоже больше; также видны пересылки между нодами. В целом внешний вид графика (не считая увеличение количества элементов) мало изменился.

### Результаты с async profiler-а (ALLOC)
![Результаты с async profiler-а (ALLOC)](assets/stage_4/async_alloc_get.svg)

Аналогично CPU GET — внешний вид графика +- такой же. Видна пересылка запросов между нодами.

### Результаты с async profiler-a (LOCK)

![Результаты с async profiler-a (LOCK)](assets/stage_4/async_lock_get.svg)

Здесь как и в LOCK PUT — присутствуют блокировки потоков; «башенки» выглядят неоднородными в отличие от предыдущего этапа.

## Вывод

Что касается быстродействия, для PUT оно почти не уменьшилось: 100% запросов обрабатываются за 4.16s. А вот в случае GET мы имеем 128.96ms против 11.11ms на предыдущем этапе.

