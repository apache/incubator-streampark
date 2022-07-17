-- select ----------------------------------------------------------------------------------------------------------------
select id, name, age, fetch_millisecond() as millisecond
from source_kafka1
;





-- create ----------------------------------------------------------------------------------------------------------------
CREATE TABLE Orders (
    `user` BIGINT,
    product STRING,
    order_time TIMESTAMP(3)
) WITH (
    'connector' = 'kafka',
    'scan.startup.mode' = 'earliest-offset'
);

create catalog hive with (
    'type' = 'hive',
    'hadoop-conf-dir' = 'D:\IDEAWorkspace\work\streamx\streamx-flink\streamx-flink-shims\streamx-flink-shims-test\src\test\resources',
    'hive-conf-dir' = 'D:\IDEAWorkspace\work\streamx\streamx-flink\streamx-flink-shims\streamx-flink-shims-test\src\test\resources'
)
;
create database test;

create view test_view as
select id, name, age, fetch_millisecond() as millisecond
from source_kafka1
;

create function fetch_millisecond as 'com.streamxhub.streamx.flink.core.test.FetchMillisecond' language java;
create temporary function fetch_millisecond as 'com.streamxhub.streamx.flink.core.test.FetchMillisecond' language java;
create TEMPORARY SYSTEM function fetch_millisecond as 'com.streamxhub.streamx.flink.core.test.FetchMillisecond' language java;





-- drop ----------------------------------------------------------------------------------------------------------------
drop catalog hive;
drop catalog IF EXISTS hive;

drop table test;
drop TEMPORARY table test;
drop table IF EXISTS test;
drop TEMPORARY table IF EXISTS test;
drop TEMPORARY table IF EXISTS test.test;
drop TEMPORARY table IF EXISTS hive.test.test;

drop database test;
drop database if exists test;
drop database if exists hive.test;
drop database if exists hive.test RESTRICT;
drop database if exists hive.test CASCADE;

drop view test_view;
drop TEMPORARY view test_view;
drop view IF EXISTS test_view;
drop TEMPORARY view IF EXISTS test_view;
drop view test.test_view;

drop function test_function;
drop TEMPORARY function test_function;
drop TEMPORARY SYSTEM function test_function;
drop function IF EXISTS test_function;



-- alter ----------------------------------------------------------------------------------------------------------------
alter table test rename to test1;
alter table test set ('k' = 'v');

alter view test_view rename to test_view2;
alter view test_view as
select id, name, age, fetch_millisecond() as millisecond
from source_kafka1
;

alter temporary function fetch_millisecond as 'com.streamxhub.streamx.flink.core.test.FetchMillisecond' language java;
alter TEMPORARY SYSTEM function fetch_millisecond as 'com.streamxhub.streamx.flink.core.test.FetchMillisecond' language java;





-- insert ----------------------------------------------------------------------------------------------------------------
insert into sink_kafka1
select id, name, age, fetch_millisecond() as millisecond
from source_kafka1
;

insert overwrite sink_kafka1
select id, name, age, fetch_millisecond() as millisecond
from source_kafka1
;




-- desc ----------------------------------------------------------------------------------------------------------------
DESCRIBE test;
desc test;
desc test.test;





-- explain ----------------------------------------------------------------------------------------------------------------
EXPLAIN PLAN FOR
select id, name, age, fetch_millisecond() as millisecond
from source_kafka1
;

EXPLAIN ESTIMATED_COST
select id, name, age, fetch_millisecond() as millisecond
from source_kafka1
;




-- use ----------------------------------------------------------------------------------------------------------------
use catalog hive;

use modules hive;
use modules hive, core;

use test;
use hive.test;



-- show ----------------------------------------------------------------------------------------------------------------
show catalogs;

show current catalog;

show databases;

show current database;

show tables;
show tables from db1;
show tables from db1 like '%n';
show tables from db1 not like '%n';

show create table test;

show columns from orders;
show columns from database1.orders;
show columns from catalog1.database1.orders;
show columns from orders like '%r';
show columns from orders not like '%_r';

show views;

show create view test_view;

show functions;
show user functions;

show modules;
show full modules;





-- load ----------------------------------------------------------------------------------------------------------------
load module hive;
load module hive with ('hive-version' = '2.3.6');





-- load ----------------------------------------------------------------------------------------------------------------
unload module hive;




-- set ----------------------------------------------------------------------------------------------------------------
set 'table.local-time-zone' = 'GMT+08:00';





-- reset ----------------------------------------------------------------------------------------------------------------
reset 'table.local-time-zone';
reset;
