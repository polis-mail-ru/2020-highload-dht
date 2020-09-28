# Оценка времени отклика сервера в режиме симулирования запросов (wrk + async-profiler)

Сведения о системе
| | |
|-|-|
| ОС Ubuntu | 18.04 LTS x64-bit |
| Процессор | Intel(R) Celeron(R) N4000 CPU @ 1.10GHz |
| Объём RAM | 8 ГБ |
| Количество ядер ЦПУ | 2 |


В ходе мониторинга производительности сервера (хост по адресу http://127.0.0.1:8080) с использованием инструмента <em>wrk<em> получены результирующие задержки обработки следующих запросов:
1) curl http://127.0.0.1:8080/v0/status    
2) GET /v0/entity?id=<ID>   
3) PUT /v0/entity?id=<ID>  
4) DELETE /v0/entity?id=<ID>

Основные параметры создания и передачи запросов через wrk (идентичны для всех исследуемых операций) 

wrk -t1 -c1 -d3m -R2000 http://127.0.0.1:8080 


## Этап 1. HTTP + storage (deadline 2020-09-30)
### Fork
[Форкните проект](https://help.github.com/articles/fork-a-repo/), склонируйте и добавьте `upstream`:
```
$ git clone git@github.com:<username>/2020-highload-dht.git
Cloning into '2020-highload-dht'...
...
$ git remote add upstream git@github.com:polis-mail-ru/2020-highload-dht.git
$ git fetch upstream
From github.com:polis-mail-ru/2020-highload-dht
 * [new branch]      master     -> upstream/master
```

### Make
Так можно запустить тесты:
```
$ ./gradlew test
```

А вот так -- сервер:
```
$ ./gradlew run
```

### Develop
Откройте в IDE -- [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/) нам будет достаточно.

**ВНИМАНИЕ!** При запуске тестов или сервера в IDE необходимо передавать Java опцию `-Xmx256m`.

В своём Java package `ru.mail.polis.service.<username>` реализуйте интерфейс [`Service`](src/main/java/ru/mail/polis/service/Service.java) и поддержите следующий HTTP REST API протокол:
* HTTP `GET /v0/entity?id=<ID>` -- получить данные по ключу `<ID>`. Возвращает `200 OK` и данные или `404 Not Found`.
* HTTP `PUT /v0/entity?id=<ID>` -- создать/перезаписать (upsert) данные по ключу `<ID>`. Возвращает `201 Created`.
* HTTP `DELETE /v0/entity?id=<ID>` -- удалить данные по ключу `<ID>`. Возвращает `202 Accepted`.

Возвращайте реализацию интерфейса в [`ServiceFactory`](src/main/java/ru/mail/polis/service/ServiceFactory.java).

Реализацию `DAO` берём из весеннего курса `2020-db-lsm`, либо запиливаем [adapter](https://en.wikipedia.org/wiki/Adapter_pattern) к уже готовой реализации LSM с биндингами на Java (например, RocksDB, LevelDB или любой другой).

Проведите нагрузочное тестирование с помощью [wrk](https://github.com/giltene/wrk2) в **одно соединение**.
Почему не `curl`/F5, можно узнать [здесь](http://highscalability.com/blog/2015/10/5/your-load-generator-is-probably-lying-to-you-take-the-red-pi.html) и [здесь](https://www.youtube.com/watch?v=lJ8ydIuPFeU).

Попрофилируйте (CPU и alloc) под нагрузкой с помощью [async-profiler](https://github.com/jvm-profiling-tools/async-profiler) и проанализируйте результаты.

Продолжайте запускать тесты и исправлять ошибки, не забывая [подтягивать новые тесты и фиксы из `upstream`](https://help.github.com/articles/syncing-a-fork/).
Если заметите ошибку в `upstream`, заводите баг и присылайте pull request ;)

### Report
Когда всё будет готово, присылайте pull request со своей реализацией и оптимизациями на review.
Не забывайте **отвечать на комментарии в PR** (в том числе автоматизированные) и **исправлять замечания**!
