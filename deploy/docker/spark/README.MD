# Spark + Hadoop + Hive + Yarn test environment

### Prepare

    Docker 1.13.1+
    Docker Compose 1.28.0+

1.Execution Docker compose build command

```shell
cd deploy/spark
docker-compose up -d
```
2.Start dfs and yarn
```shell
docker exec -it master
cd /opt
sh start-hadoop.sh
```
