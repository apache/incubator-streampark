# Docker 版本

## 快速开始

- Requirement
    1. docker 环境
    2. docker-compose 环境

- 第一步（stream server 的配置外挂需要，可以照着这个样例，把其他部分挂载到宿主机）

```
mkdir workspace
```

- 第二步

```
sh build.sh v1.2.1-beta.1
```

**PS 这个版本号必须跟 Git 上面的 Git Tag 一致**

- 第三步

```
docker-compose up
```



