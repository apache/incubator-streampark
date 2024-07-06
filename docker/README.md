
### 1. Confirm release version
When the new version is released, the release manager will verify the image tag and push the image to the image repository. The latest image tag will be written in [docker-compose.yaml](./docker-compose.yaml) file. Users can also independently verify whether the version of the StreamPark image in [docker-compose.yaml](./docker-compose.yaml) file is correct. If the current branch has not been released, the image tag will be the last release image tag.

```yaml
version: '3.8'
services:
    streampark-console:
        image: apache/streampark:2.2.0
```

### 2. docker-compose up

```shell
docker-compose up -d
```

### 3. open in browser

http://localhost:10000

#### [more detail](https://streampark.apache.org/docs/get-started/docker-deployment)
