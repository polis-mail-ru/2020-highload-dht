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
