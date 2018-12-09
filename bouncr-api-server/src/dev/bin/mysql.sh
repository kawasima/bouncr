#!/bin/sh

docker run --name mysql-docker \
    -e MYSQL_RANDOM_ROOT_PASSWORD=yes \
    -e MYSQL_DATABASE=bouncr \
    -e MYSQL_USER=bouncr \
    -e MYSQL_PASSWORD=bouncr \
    -p 3306:3306 -d mysql/mysql-server:5.7 \
    --character-set-server=utf8 --collation-server=utf8_general_ci \
    --lower_case_table_names=1
