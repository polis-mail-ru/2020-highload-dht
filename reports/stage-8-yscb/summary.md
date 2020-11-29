The core workloads consist of six different workloads:

Workload A: Update heavy workload

This workload has a mix of 50/50 reads and writes. An application example is a session store recording recent actions.

Workload B: Read mostly workload
This workload has a 95/5 reads/write mix. Application example: photo tagging; add a tag is an update, but most operations are to read tags.

Workload C: Read only

This workload is 100% read. Application example: user profile cache, where profiles are constructed elsewhere (e.g., Hadoop).

Workload D: Read latest workload (Для тестирования REST в YCS!B не имплиментированно данное распределение.
Воспользовался распределением hotspot, которое симулирует внезапное повышение нагрузки)

In this workload, new records are inserted, and the most recently inserted records are the most popular. Application example: user status updates; people want to read the latest.

ВСЕГО ИСПОЛЬЗОВАЛОСЬ 1_000_000 ключей. 
Нагрузки накладываются последовательно от A до D. 

Резульаты для WORKLOAD A:

       [OVERALL], Throughput(ops/sec), 2306.613522291113
       [OVERALL], RunTime(ms), 433536
       [TOTAL_GCS_G1_Young_Generation], Count, 679
       [TOTAL_GC_TIME_G1_Young_Generation], Time(ms), 1271
       [TOTAL_GC_TIME_%_G1_Young_Generation], Time(%), 0.29317057868320046
       [TOTAL_GCS_G1_Old_Generation], Count, 0
       [TOTAL_GC_TIME_G1_Old_Generation], Time(ms), 0
       [TOTAL_GC_TIME_%_G1_Old_Generation], Time(%), 0.0
       [TOTAL_GCs], Count, 679
       [TOTAL_GC_TIME], Time(ms), 1271
       [TOTAL_GC_TIME_%], Time(%), 0.29317057868320046
       [READ], Operations, 131740
       [READ], AverageLatency(us), 835.1359571884014
       [READ], MinLatency(us), 392
       [READ], MaxLatency(us), 20143
       [READ], 50thPercentileLatency(us), 796
       [READ], 90thPercentileLatency(us), 1040
       [READ], 95thPercentileLatency(us), 1169
       [READ], 99thPercentileLatency(us), 1734
       [READ], Return=OK, 131740
       [READ], Return=NOT_FOUND, 368886
       [CLEANUP], Operations, 2
       [CLEANUP], AverageLatency(us), 0.5
       [CLEANUP], MinLatency(us), 0
       [CLEANUP], MaxLatency(us), 1
       [CLEANUP], 50thPercentileLatency(us), 0
       [CLEANUP], 90thPercentileLatency(us), 1
       [CLEANUP], 95thPercentileLatency(us), 1
       [CLEANUP], 99thPercentileLatency(us), 1
       [READ-FAILED], Operations, 368886
       [READ-FAILED], AverageLatency(us), 846.7396621178359
       [READ-FAILED], MinLatency(us), 372
       [READ-FAILED], MaxLatency(us), 56959
       [READ-FAILED], 50thPercentileLatency(us), 802
       [READ-FAILED], 90thPercentileLatency(us), 1072
       [READ-FAILED], 95thPercentileLatency(us), 1209
       [READ-FAILED], 99thPercentileLatency(us), 1801
       [UPDATE], Operations, 499374
       [UPDATE], AverageLatency(us), 878.1942752325912
       [UPDATE], MinLatency(us), 410
       [UPDATE], MaxLatency(us), 56607
       [UPDATE], 50thPercentileLatency(us), 840
       [UPDATE], 90thPercentileLatency(us), 1094
       [UPDATE], 95thPercentileLatency(us), 1243
       [UPDATE], 99thPercentileLatency(us), 1828
       [UPDATE], Return=OK, 499374

Резульаты для WORKLOAD B:

       [OVERALL], RunTime(ms), 616136
       [OVERALL], Throughput(ops/sec), 1623.0182946622174
       [TOTAL_GCS_G1_Young_Generation], Count, 587
       [TOTAL_GC_TIME_G1_Young_Generation], Time(ms), 896
       [TOTAL_GC_TIME_%_G1_Young_Generation], Time(%), 0.14542243920173467
       [TOTAL_GCS_G1_Old_Generation], Count, 0
       [TOTAL_GC_TIME_G1_Old_Generation], Time(ms), 0
       [TOTAL_GC_TIME_%_G1_Old_Generation], Time(%), 0.0
       [TOTAL_GCs], Count, 587
       [TOTAL_GC_TIME], Time(ms), 896
       [TOTAL_GC_TIME_%], Time(%), 0.14542243920173467
       [READ], Operations, 420928
       [READ], AverageLatency(us), 604.1385652653186
       [READ], MinLatency(us), 336
       [READ], MaxLatency(us), 48735
       [READ], 50thPercentileLatency(us), 586
       [READ], 90thPercentileLatency(us), 747
       [READ], 95thPercentileLatency(us), 824
       [READ], 99thPercentileLatency(us), 1037
       [READ], Return=OK, 420928
       [READ], Return=NOT_FOUND, 528787
       [CLEANUP], Operations, 1
       [CLEANUP], AverageLatency(us), 1.0
       [CLEANUP], MinLatency(us), 1
       [CLEANUP], MaxLatency(us), 1
       [CLEANUP], 50thPercentileLatency(us), 1
       [CLEANUP], 90thPercentileLatency(us), 1
       [CLEANUP], 95thPercentileLatency(us), 1
       [CLEANUP], 99thPercentileLatency(us), 1
       [READ-FAILED], Operations, 528787
       [READ-FAILED], AverageLatency(us), 610.9091448919886
       [READ-FAILED], MinLatency(us), 335
       [READ-FAILED], MaxLatency(us), 62015
       [READ-FAILED], 50thPercentileLatency(us), 592
       [READ-FAILED], 90thPercentileLatency(us), 756
       [READ-FAILED], 95thPercentileLatency(us), 837
       [READ-FAILED], 99thPercentileLatency(us), 1057
       [UPDATE], Operations, 50285
       [UPDATE], AverageLatency(us), 688.9685194391966
       [UPDATE], MinLatency(us), 388
       [UPDATE], MaxLatency(us), 7987
       [UPDATE], 50thPercentileLatency(us), 669
       [UPDATE], 90thPercentileLatency(us), 846
       [UPDATE], 95thPercentileLatency(us), 928
       [UPDATE], 99thPercentileLatency(us), 1180
       [UPDATE], Return=OK, 50285

Резульаты для WORKLOAD C:

       [OVERALL], RunTime(ms), 600542
       [OVERALL], Throughput(ops/sec), 1665.1624699021884
       [TOTAL_GCS_G1_Young_Generation], Count, 581
       [TOTAL_GC_TIME_G1_Young_Generation], Time(ms), 841
       [TOTAL_GC_TIME_%_G1_Young_Generation], Time(%), 0.14004016371877404
       [TOTAL_GCS_G1_Old_Generation], Count, 0
       [TOTAL_GC_TIME_G1_Old_Generation], Time(ms), 0
       [TOTAL_GC_TIME_%_G1_Old_Generation], Time(%), 0.0
       [TOTAL_GCs], Count, 581
       [TOTAL_GC_TIME], Time(ms), 841
       [TOTAL_GC_TIME_%], Time(%), 0.14004016371877404
       [READ], Operations, 829790
       [READ], AverageLatency(us), 593.6887152171031
       [READ], MinLatency(us), 327
       [READ], MaxLatency(us), 52543
       [READ], 50thPercentileLatency(us), 576
       [READ], 90thPercentileLatency(us), 730
       [READ], 95thPercentileLatency(us), 808
       [READ], 99thPercentileLatency(us), 1012
       [READ], Return=OK, 829790
       [READ], Return=NOT_FOUND, 170210
       [CLEANUP], Operations, 1
       [CLEANUP], AverageLatency(us), 1.0
       [CLEANUP], MinLatency(us), 1
       [CLEANUP], MaxLatency(us), 1
       [CLEANUP], 50thPercentileLatency(us), 1
       [CLEANUP], 90thPercentileLatency(us), 1
       [CLEANUP], 95thPercentileLatency(us), 1
       [CLEANUP], 99thPercentileLatency(us), 1
       [READ-FAILED], Operations, 170210
       [READ-FAILED], AverageLatency(us), 610.5770107514247
       [READ-FAILED], MinLatency(us), 341
       [READ-FAILED], MaxLatency(us), 52287
       [READ-FAILED], 50thPercentileLatency(us), 592
       [READ-FAILED], 90thPercentileLatency(us), 752
       [READ-FAILED], 95thPercentileLatency(us), 832
       [READ-FAILED], 99thPercentileLatency(us), 1041


Резульаты для WORKLOAD B:


       [OVERALL], RunTime(ms), 616089
       [OVERALL], Throughput(ops/sec), 1623.142110961241
       [TOTAL_GCS_G1_Young_Generation], Count, 650
       [TOTAL_GC_TIME_G1_Young_Generation], Time(ms), 994
       [TOTAL_GC_TIME_%_G1_Young_Generation], Time(%), 0.16134032582954735
       [TOTAL_GCS_G1_Old_Generation], Count, 0
       [TOTAL_GC_TIME_G1_Old_Generation], Time(ms), 0
       [TOTAL_GC_TIME_%_G1_Old_Generation], Time(%), 0.0
       [TOTAL_GCs], Count, 650
       [TOTAL_GC_TIME], Time(ms), 994
       [TOTAL_GC_TIME_%], Time(%), 0.16134032582954735
       [READ], Operations, 566046
       [READ], AverageLatency(us), 604.7350445016837
       [READ], MinLatency(us), 332
       [READ], MaxLatency(us), 46655
       [READ], 50thPercentileLatency(us), 589
       [READ], 90thPercentileLatency(us), 741
       [READ], 95thPercentileLatency(us), 812
       [READ], 99thPercentileLatency(us), 1013
       [READ], Return=OK, 566046
       [READ], Return=NOT_FOUND, 383876
       [CLEANUP], Operations, 1
       [CLEANUP], AverageLatency(us), 1.0
       [CLEANUP], MinLatency(us), 1
       [CLEANUP], MaxLatency(us), 1
       [CLEANUP], 50thPercentileLatency(us), 1
       [CLEANUP], 90thPercentileLatency(us), 1
       [CLEANUP], 95thPercentileLatency(us), 1
       [CLEANUP], 99thPercentileLatency(us), 1
       [READ-FAILED], Operations, 383876
       [READ-FAILED], AverageLatency(us), 612.1330064916796
       [READ-FAILED], MinLatency(us), 342
       [READ-FAILED], MaxLatency(us), 8991
       [READ-FAILED], 50thPercentileLatency(us), 596
       [READ-FAILED], 90thPercentileLatency(us), 752
       [READ-FAILED], 95thPercentileLatency(us), 826
       [READ-FAILED], 99thPercentileLatency(us), 1030
       [UPDATE], Operations, 50078
       [UPDATE], AverageLatency(us), 687.71536403211
       [UPDATE], MinLatency(us), 376
       [UPDATE], MaxLatency(us), 4291
       [UPDATE], 50thPercentileLatency(us), 670
       [UPDATE], 90thPercentileLatency(us), 837
       [UPDATE], 95thPercentileLatency(us), 917
       [UPDATE], 99thPercentileLatency(us), 1143
       [UPDATE], Return=OK, 50078

