Для замеров был взят 7 этап.
## Подготовка тестового окружения
1. Для профилирования производительности, был склонирован yccb. 
2. Далее было необходимо написать workload файлы, которые представляют собой смесь workload_rest и шаблонов workload A-E.
   Большинство настроек можно указать как в командной строке так и в workload файле. Ключевым было задание trace файла
   (файл в котором находится значения id для записи), url адрес до сервиса, а также количество чтений, процентное соотношение
   CRUD операций и количество исполняемых потоков. Все workload-файлы можно посмотреть [тут](workloads).
3. Для выполнения нагрузки были сделаны 2 trace файла, первый - [trace_rest](traces/trace_rest.txt)содержит 1 миллион ключей,
   второй - [trace_rest_range](traces/trace_rest_range.txt) содержит диапазоны ключей используемы для нагрузки range запросов.
4. Также небольшие изменения перетерпел restclient. В был убран chunked протокол для операции вставки, а также изменено значение
   вставляемое в тело запроса при операции put.
5. Последним этапом был написан простой скрипт дял выполнения команд и записи результатов в файлы - [run-loads.PS1](run-loads.PS1) 

## Характеристики тестового стенда
    ```CsNumberOfLogicalProcessors  : 16
    CsProcessors                 : {AMD Ryzen 7 4800HS with Radeon }
    CsSystemType                 : x64-based PC
    OsManufacturer               : Microsoft Corporation
    GPU                          : GeForce GTX 1660 Ti GDDR6
    SSD speed (read/write)       : 3230/1625 (mb/sec) 
    RAM                          : 16 gb (2 x 3200 Mhz) 

## Результаты замеров
Все замеры проводились с выделением 4-х потоков на нагрузку.
Сервис на момент старта тестов работал 3 часа, и прогревался 1 миллионом записей и 1 миллионов чтений.

Формат результатов следующий:
1. Что делает workload-X
2. Что мы получили
3. Анализ результата

### Workload-A - Update heavy workload 
#### Описание
Эта рабочая нагрузка состоит из 50/50 операций чтения и записи. 
Примером приложения является хранилище сеансов, в котором записываются последние действия.

#### Результат
       Still waiting for thread Thread-4 to complete. Workload status: true
       [OVERALL], RunTime(ms), 722993
       [OVERALL], Throughput(ops/sec), 11.30716341651994
       [TOTAL_GCS_G1_Young_Generation], Count, 20
       [TOTAL_GC_TIME_G1_Young_Generation], Time(ms), 167
       [TOTAL_GC_TIME_%_G1_Young_Generation], Time(%), 0.023098425572585072
       [TOTAL_GCS_G1_Old_Generation], Count, 0
       [TOTAL_GC_TIME_G1_Old_Generation], Time(ms), 0
       [TOTAL_GC_TIME_%_G1_Old_Generation], Time(%), 0.0
       [TOTAL_GCs], Count, 20
       [TOTAL_GC_TIME], Time(ms), 167
       [TOTAL_GC_TIME_%], Time(%), 0.023098425572585072
       [READ], Operations, 4073
       [READ], AverageLatency(us), 1114.3908666830346
       [READ], MinLatency(us), 683
       [READ], MaxLatency(us), 30575
       [READ], 50thPercentileLatency(us), 1022
       [READ], 90thPercentileLatency(us), 1402
       [READ], 95thPercentileLatency(us), 1660
       [READ], 99thPercentileLatency(us), 2201
       [READ], Return=OK, 4073
       [READ], Return=ERROR, 21
       [UPDATE-FAILED], Operations, 27
       [UPDATE-FAILED], AverageLatency(us), 6.0014592E7
       [UPDATE-FAILED], MinLatency(us), 59998208
       [UPDATE-FAILED], MaxLatency(us), 60030975
       [UPDATE-FAILED], 50thPercentileLatency(us), 60030975
       [UPDATE-FAILED], 90thPercentileLatency(us), 60030975
       [UPDATE-FAILED], 95thPercentileLatency(us), 60030975
       [UPDATE-FAILED], 99thPercentileLatency(us), 60030975
       [CLEANUP], Operations, 4
       [CLEANUP], AverageLatency(us), 0.5
       [CLEANUP], MinLatency(us), 0
       [CLEANUP], MaxLatency(us), 1
       [CLEANUP], 50thPercentileLatency(us), 0
       [CLEANUP], 90thPercentileLatency(us), 1
       [CLEANUP], 95thPercentileLatency(us), 1
       [CLEANUP], 99thPercentileLatency(us), 1
       [UPDATE], Operations, 4054
       [UPDATE], AverageLatency(us), 1129.1714356191417
       [UPDATE], MinLatency(us), 700
       [UPDATE], MaxLatency(us), 30127
       [UPDATE], 50thPercentileLatency(us), 1031
       [UPDATE], 90thPercentileLatency(us), 1403
       [UPDATE], 95thPercentileLatency(us), 1630
       [UPDATE], 99thPercentileLatency(us), 2181
       [UPDATE], Return=OK, 4054
       [UPDATE], Return=ERROR, 27
       [READ-FAILED], Operations, 21
       [READ-FAILED], AverageLatency(us), 6.0014592E7
       [READ-FAILED], MinLatency(us), 59998208
       [READ-FAILED], MaxLatency(us), 60030975
       [READ-FAILED], 50thPercentileLatency(us), 60030975
       [READ-FAILED], 90thPercentileLatency(us), 60030975
       [READ-FAILED], 95thPercentileLatency(us), 60030975
       [READ-FAILED], 99thPercentileLatency(us), 60030975

#### Анализ
Операции чтения в среднем занимали 1.1 мс, а максимальная задержка доходила до 30 мс, при этом 99 процентилей укладываются
в 2 мс. Операции записи значений в среднем выполнялись за 1.1 мс, а с максимальной задержкой в 30.1 мс, при этом 99
процентилей пользователей испытают задержку в 2.2 мс. 27 раз нам не удалось записать значение, что может быть связанно
как со сбоем сервиса, например, если генератор положил в тело запроса null. 

Ошибочные ситуации обрабатываются безобразно долго, как запись так и чтение, возможно это связанно с Socket Timout Exception,
который в windows ожидает 60 секунд (в linux задержка будет подстать рядовым ситуациям). 
  
Все на сборку мусора было потраченно 167 ms.

### Workload-B - Read mostly workload
#### Описание
Эта рабочая нагрузка имеет 95/5 операций чтения/записи. 
Пример приложения: тегирование фотографий; добавление тега-это обновление, но большинство операций заключается в чтении тегов.

#### Результат

    Still waiting for thread Thread-4 to complete. Workload status: true
    [OVERALL], RunTime(ms), 724432
    [OVERALL], Throughput(ops/sec), 13.572012279965545
    [TOTAL_GCS_G1_Young_Generation], Count, 21
    [TOTAL_GC_TIME_G1_Young_Generation], Time(ms), 177
    [TOTAL_GC_TIME_%_G1_Young_Generation], Time(%), 0.02443293504428297
    [TOTAL_GCS_G1_Old_Generation], Count, 0
    [TOTAL_GC_TIME_G1_Old_Generation], Time(ms), 0
    [TOTAL_GC_TIME_%_G1_Old_Generation], Time(%), 0.0
    [TOTAL_GCs], Count, 21
    [TOTAL_GC_TIME], Time(ms), 177
    [TOTAL_GC_TIME_%], Time(%), 0.02443293504428297
    [READ], Operations, 9304
    [READ], AverageLatency(us), 1066.6483233018057
    [READ], MinLatency(us), 642
    [READ], MaxLatency(us), 30863
    [READ], 50thPercentileLatency(us), 985
    [READ], 90thPercentileLatency(us), 1378
    [READ], 95thPercentileLatency(us), 1545
    [READ], 99thPercentileLatency(us), 2007
    [READ], Return=OK, 9304
    [READ], Return=ERROR, 45
    [UPDATE-FAILED], Operations, 3
    [UPDATE-FAILED], AverageLatency(us), 6.0014592E7
    [UPDATE-FAILED], MinLatency(us), 59998208
    [UPDATE-FAILED], MaxLatency(us), 60030975
    [UPDATE-FAILED], 50thPercentileLatency(us), 60030975
    [UPDATE-FAILED], 90thPercentileLatency(us), 60030975
    [UPDATE-FAILED], 95thPercentileLatency(us), 60030975
    [UPDATE-FAILED], 99thPercentileLatency(us), 60030975
    [CLEANUP], Operations, 4
    [CLEANUP], AverageLatency(us), 0.5
    [CLEANUP], MinLatency(us), 0
    [CLEANUP], MaxLatency(us), 1
    [CLEANUP], 50thPercentileLatency(us), 0
    [CLEANUP], 90thPercentileLatency(us), 1
    [CLEANUP], 95thPercentileLatency(us), 1
    [CLEANUP], 99thPercentileLatency(us), 1
    [UPDATE], Operations, 480
    [UPDATE], AverageLatency(us), 1136.5604166666667
    [UPDATE], MinLatency(us), 765
    [UPDATE], MaxLatency(us), 4187
    [UPDATE], 50thPercentileLatency(us), 1050
    [UPDATE], 90thPercentileLatency(us), 1447
    [UPDATE], 95thPercentileLatency(us), 1648
    [UPDATE], 99thPercentileLatency(us), 2317
    [UPDATE], Return=OK, 480
    [UPDATE], Return=ERROR, 3
    [READ-FAILED], Operations, 45
    [READ-FAILED], AverageLatency(us), 6.0014592E7
    [READ-FAILED], MinLatency(us), 59998208
    [READ-FAILED], MaxLatency(us), 60030975
    [READ-FAILED], 50thPercentileLatency(us), 60030975
    [READ-FAILED], 90thPercentileLatency(us), 60030975
    [READ-FAILED], 95thPercentileLatency(us), 60030975
    [READ-FAILED], 99thPercentileLatency(us), 60030975

#### Анализ

Миллион чтений показал, что в среднем мы получаем значение по ключу за 1мс, а максимальная задержка составлят 30,9 мс.
99-й процентиль похож на предидущий результат. Что похоже на предидущую конфигурацию по результатам. 
Записываем мы в среднем за 1.1 мс, а максимальный результат задержки составляет 4.2 мс, что достаточно мало,
и скорей всего обусловленно тем что записей в данной конфгурации мало, несмотря на 1 млн операций. 99 процентиль при этом
2 мс.  

### Workload-C - Read only
#### Описание
Эта рабочая нагрузка читается на 100%. 
Пример приложения: кэш профилей пользователей, где профили создаются в другом месте (например, Hadoop).

#### Результат

    Still waiting for thread Thread-4 to complete. Workload status: true
    [OVERALL], RunTime(ms), 723724
    [OVERALL], Throughput(ops/sec), 13.53831018454549
    [TOTAL_GCS_G1_Young_Generation], Count, 16
    [TOTAL_GC_TIME_G1_Young_Generation], Time(ms), 211
    [TOTAL_GC_TIME_%_G1_Young_Generation], Time(%), 0.029154760654614188
    [TOTAL_GCS_G1_Old_Generation], Count, 0
    [TOTAL_GC_TIME_G1_Old_Generation], Time(ms), 0
    [TOTAL_GC_TIME_%_G1_Old_Generation], Time(%), 0.0
    [TOTAL_GCs], Count, 16
    [TOTAL_GC_TIME], Time(ms), 211
    [TOTAL_GC_TIME_%], Time(%), 0.029154760654614188
    [READ], Operations, 9750
    [READ], AverageLatency(us), 1025.6148717948718
    [READ], MinLatency(us), 571
    [READ], MaxLatency(us), 30367
    [READ], 50thPercentileLatency(us), 939
    [READ], 90thPercentileLatency(us), 1417
    [READ], 95thPercentileLatency(us), 1588
    [READ], 99thPercentileLatency(us), 2099
    [READ], Return=OK, 9750
    [READ], Return=ERROR, 48
    [CLEANUP], Operations, 4
    [CLEANUP], AverageLatency(us), 1.0
    [CLEANUP], MinLatency(us), 1
    [CLEANUP], MaxLatency(us), 1
    [CLEANUP], 50thPercentileLatency(us), 1
    [CLEANUP], 90thPercentileLatency(us), 1
    [CLEANUP], 95thPercentileLatency(us), 1
    [CLEANUP], 99thPercentileLatency(us), 1
    [READ-FAILED], Operations, 48
    [READ-FAILED], AverageLatency(us), 6.0014592E7
    [READ-FAILED], MinLatency(us), 59998208
    [READ-FAILED], MaxLatency(us), 60030975
    [READ-FAILED], 50thPercentileLatency(us), 60030975
    [READ-FAILED], 90thPercentileLatency(us), 60030975
    [READ-FAILED], 95thPercentileLatency(us), 60030975
    [READ-FAILED], 99thPercentileLatency(us), 60030975

#### Анализ
В данном случае мы видим что у нас на 40 мс выросла сборка мусора. В случае с записью мы видим что максимально значение
находится в пределах 30 мс, а  среднее в пределах 1 мс, 99 процентилей соответствует 2.1 мс. В данном случае 
напрашивается вывод, что можно уменшить количество замеров, в угоду скорости прохождения тестирования. Но в любом случае,
мы видим, что 1 миллион операций дают стабильные результаты.

### Workload-D - Read latest workload
#### Описание
В этой рабочей нагрузке вставляются новые записи, и самые последние вставленные записи являются наиболее популярными. 
Пример приложения: обновления статуса пользователя; люди хотят читать последние новости

#### Результат

    Still waiting for thread Thread-3 to complete. Workload status: true
    [OVERALL], RunTime(ms), 724002
    [OVERALL], Throughput(ops/sec), 16.31487205836448
    [TOTAL_GCS_G1_Young_Generation], Count, 16
    [TOTAL_GC_TIME_G1_Young_Generation], Time(ms), 170
    [TOTAL_GC_TIME_%_G1_Young_Generation], Time(%), 0.023480598119894697
    [TOTAL_GCS_G1_Old_Generation], Count, 0
    [TOTAL_GC_TIME_G1_Old_Generation], Time(ms), 0
    [TOTAL_GC_TIME_%_G1_Old_Generation], Time(%), 0.0
    [TOTAL_GCs], Count, 16
    [TOTAL_GC_TIME], Time(ms), 170
    [TOTAL_GC_TIME_%], Time(%), 0.023480598119894697
    [READ], Operations, 11136
    [READ], AverageLatency(us), 913.4885057471264
    [READ], MinLatency(us), 577
    [READ], MaxLatency(us), 28991
    [READ], 50thPercentileLatency(us), 821
    [READ], 90thPercentileLatency(us), 1232
    [READ], 95thPercentileLatency(us), 1392
    [READ], 99thPercentileLatency(us), 1853
    [READ], Return=OK, 11136
    [READ], Return=ERROR, 44
    [UPDATE-FAILED], Operations, 4
    [UPDATE-FAILED], AverageLatency(us), 6.0014592E7
    [UPDATE-FAILED], MinLatency(us), 59998208
    [UPDATE-FAILED], MaxLatency(us), 60030975
    [UPDATE-FAILED], 50thPercentileLatency(us), 60030975
    [UPDATE-FAILED], 90thPercentileLatency(us), 60030975
    [UPDATE-FAILED], 95thPercentileLatency(us), 60030975
    [UPDATE-FAILED], 99thPercentileLatency(us), 60030975
    [CLEANUP], Operations, 4
    [CLEANUP], AverageLatency(us), 1.0
    [CLEANUP], MinLatency(us), 0
    [CLEANUP], MaxLatency(us), 2
    [CLEANUP], 50thPercentileLatency(us), 1
    [CLEANUP], 90thPercentileLatency(us), 2
    [CLEANUP], 95thPercentileLatency(us), 2
    [CLEANUP], 99thPercentileLatency(us), 2
    [UPDATE], Operations, 628
    [UPDATE], AverageLatency(us), 951.8073248407643
    [UPDATE], MinLatency(us), 652
    [UPDATE], MaxLatency(us), 3815
    [UPDATE], 50thPercentileLatency(us), 871
    [UPDATE], 90thPercentileLatency(us), 1285
    [UPDATE], 95thPercentileLatency(us), 1431
    [UPDATE], 99thPercentileLatency(us), 1975
    [UPDATE], Return=OK, 628
    [UPDATE], Return=ERROR, 4
    [READ-FAILED], Operations, 44
    [READ-FAILED], AverageLatency(us), 6.0014592E7
    [READ-FAILED], MinLatency(us), 59998208
    [READ-FAILED], MaxLatency(us), 60030975
    [READ-FAILED], 50thPercentileLatency(us), 60030975
    [READ-FAILED], 90thPercentileLatency(us), 60030975
    [READ-FAILED], 95thPercentileLatency(us), 60030975
    [READ-FAILED], 99thPercentileLatency(us), 60030975


#### Анализ
Чтение в среднем за 0.9 мс. А 99 процентилей читают последние ключи за 1.8 мс. Максимум при этом 28 мс.
Запись в среднем выполняется за 0.9 А максимум составляет 3.8 мс, что говорит либо о малом количестве операций записи
либо о хорошей производительности сервиса (скорей всего второе). Но в любом случае операция записи это то что должно 
делать LSM хранилище лучше всего.

### Workload-E - Short ranges
#### Описание
В этой рабочей нагрузке запрашиваются короткие диапазоны записей, а не отдельные записи. 
Пример приложения: потоковые разговоры, где каждое сканирование выполняется для записей в данном потоке 
(предполагается, что они кластеризованы по идентификатору потока).

#### Результат

    [OVERALL], RunTime(ms), 13224
    [OVERALL], Throughput(ops/sec), 7562.008469449485
    [TOTAL_GCS_G1_Young_Generation], Count, 196
    [TOTAL_GC_TIME_G1_Young_Generation], Time(ms), 104
    [TOTAL_GC_TIME_%_G1_Young_Generation], Time(%), 0.7864488808227466
    [TOTAL_GCS_G1_Old_Generation], Count, 0
    [TOTAL_GC_TIME_G1_Old_Generation], Time(ms), 0
    [TOTAL_GC_TIME_%_G1_Old_Generation], Time(%), 0.0
    [TOTAL_GCs], Count, 196
    [TOTAL_GC_TIME], Time(ms), 104
    [TOTAL_GC_TIME_%], Time(%), 0.7864488808227466
    [READ], Operations, 100000
    [READ], AverageLatency(us), 509.16801
    [READ], MinLatency(us), 199
    [READ], MaxLatency(us), 39679
    [READ], 95thPercentileLatency(us), 803
    [READ], 99thPercentileLatency(us), 2379
    [READ], Return=OK, 100000
    [CLEANUP], Operations, 4
    [CLEANUP], AverageLatency(us), 1.0
    [CLEANUP], MinLatency(us), 0
    [CLEANUP], MaxLatency(us), 4
    [CLEANUP], 95thPercentileLatency(us), 4
    [CLEANUP], 99thPercentileLatency(us), 4

#### Анализ
Для range запросов не проводились замеры ошибочных ситуаций. И мы не проводили никаких операций записи, так как 
записи range запросов не реализовывались. Таким образом мы получаем максимальную задержку в 39 мс, а 99 процентилей 
2.3 мс.

В целом, данная нагрузка была применена к уже заполненной базе, с диапазоном от 1 до 100 ключей. И результат 
выглядит спорно, так как если мы берём среднее (50 ключей), а операция get у нас в среднем выполняется за 1мс, 
то легко посчитать что мы должны иметь задержку в среднем 50 мс, а максимально большую (100 ключей и 30мс) 3 секунды.

Был проанализирован client и ядро нагрузочного генератора, а также исходный workloadE, но документация ничег не говорит
на счёт создания rest workload-ов для range запросов. Но очевидно, что клиент не ждёт пока ему вернутся все чанки.

Если вероятность что оставшихся 12 потоков хватает на то, чтобы успевать обрабатывать без повышения задержки, на 
не ждущего клиента.  

## Выводы
Резюмируя полученные результаты мы видим следующее:
- Все сценарии нагрузки (A-D) показали себя примерно одинаково.
- Задержки которые мы получали, очень похожи на результаты полученные wrk (по крайней мере среднее и максимально)
- Среда тестирования - Windows, показала себя не лучшей стороны в плане обработки неверных запросов. 
Сокет шёл спать 60 секунд, что сказывалось на вермени тестирования, и результатах в READ-FAIL и UPDATE-FAIL.
- Тем не менее, отсутствие прослойки в виде WSL2 + Virtual box и ограниченных ресурсах в виртуальной среде, оказала 
положительный результат. В идеале, стоило бы тестировать сервис на удаленной ноде, с linux хостом.
- Режим range-запросов не работает корректно для rest вариации ycsb, или же я не смог его адаптировать.

Впечатления от использования ycsb:
- Утилита проста в установке
- Кроссплатформенная
- Учитывает coordinated omissions
- Даёт возможность гибкой настройки workload-файлов
- Документация понятная, но не достаточная для того чтобы просто взять и использовать
- Мало информации в интернете по использованию с rest
- Требует небольшой модификаций клиента, для нормальной работы
- Требует генерации трейс-файлов
- не хватает привычных девяток 99.999...
 
Подводя итог хочется сказать, что это утилита отлична для автоматизации профилирования, и даёт гибкость в
профилировании в том случае если ты опытный пользователь и у тебя есть свои собственные workload наработки.
Но на мой взгляд профилирование ycsb rest сервиса, скорей бонус от ycsb, но не основной случай использования.
Как говорят в документации, с ycsb хорошо взять 3-5 разных баз и посмотреть какая из них лучше справится с 
нужными задачами, конкретными примерами запросов и операций. По этому, скорей всего я бы не выбрал данное средство
для профилирования rest api. 