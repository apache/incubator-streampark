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
替换 `docker-compose` 中的 `./workspace`  为绝对路径，挂载卷是为了把`streamx`配置目录外露出来
```
**PS docker-compose 挂载过的卷(挂载错误路径，必须先`docker volume`移除掉 )**

- 第四步

```
v_num=1.2.1 docker-compose up
```
**PS 这个版本号只需要 数字.数字.数字 即可，与Git Tag 数字保持一致**


