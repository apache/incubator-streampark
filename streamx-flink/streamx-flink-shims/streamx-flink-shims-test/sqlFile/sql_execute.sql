
-- set ----------------------------------------------------------------------------------------------------------------
set 'table.local-time-zone' = 'GMT+08:00';





-- reset ----------------------------------------------------------------------------------------------------------------
reset 'table.local-time-zone';
reset;




CREATE temporary TABLE source_kafka1(
    `id` int COMMENT '',
    `name` string COMMENT '',
    `age` int COMMENT '',
    proc_time as PROCTIME()
) WITH (
    'connector' = 'kafka',
    'topic' = 'source1',
    'properties.bootstrap.servers' = 'node01:9092,node02:9092,node03:9092',
    'properties.group.id' = 'test',
    'scan.startup.mode' = 'latest-offset',
    'format' = 'csv',
    'csv.field-delimiter' = ' ',
    'csv.ignore-parse-errors' = 'true',
    'csv.allow-comments' = 'true'
)
;

create table sink_kafka1(
    `id` int COMMENT '',
    `name` string COMMENT '',
    `age` int COMMENT ''
) with (
    'connector' = 'kafka',
    'topic' = 'sink1',
    'properties.bootstrap.servers' = 'node01:9092,node02:9092,node03:9092',
    'format' = 'csv'
)
;

insert into sink_kafka1
select id, name, age
from source_kafka1
;