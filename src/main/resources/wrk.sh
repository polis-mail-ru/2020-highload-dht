#!/bin/bash

TIME=10

if [[ "$1" != "" ]]; then
    TIME="$1"
else
    TIME=10
fi

rm -r output
mkdir output

wrk -s get.lua -c 1 -d $TIME -t 1 -R 10k -L http://localhost:8080
aprof start -i 99 -f output/1_get_empty.svg jps
wrk -s get.lua -c 1 -d $TIME -t 1 -R 10k -L http://localhost:8080 >> output/1_get_empty_wrk.txt
aprof stop jps

wrk -s put.lua -c 1 -d $TIME -t 1 -R 10k -L http://localhost:8080
aprof start -i 99 -f output/2_put.svg jps
wrk -s put.lua -c 1 -d $TIME -t 1 -R 10k -L http://localhost:8080 >> output/2_put_wrk.txt
aprof stop jps

wrk -s get.lua -c 1 -d $TIME -t 1 -R 10k -L http://localhost:8080
aprof start -i 99 -f output/3_get_full.svg jps
wrk -s get.lua -c 1 -d $TIME -t 1 -R 10k -L http://localhost:8080 >> output/3_get_full_wrk.txt
aprof stop jps

wrk -s delete.lua -c 1 -d $TIME -t 1 -R 10k -L http://localhost:8080
aprof start -i 99 -f output/4_delete.svg jps
wrk -s delete.lua -c 1 -d $TIME -t 1 -R 10k -L http://localhost:8080 >> output/4_delete_wrk.txt
aprof stop jps