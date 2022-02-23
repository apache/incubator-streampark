#Getting Started with Streamx using Docker

English | [中文](https://github.com/streamxhub/streamx/tree/dev/docker/README_CN.md)

## Quick start
> It does not guarantee that all the latest released versions are available in the repository. The latest released versions can be implemented by **User compile way**.

-  Step 1.(The configuration plug-in of the stream server is required, you can follow this example to mount other parts to the host.)

````
mkdir workspace
````

- Step 2. 

````
Replace `./workspace` in `docker-compose-quick-start.yml` with absolute path, mount volume is to expose `streamx` configuration directory.
````
**PS docker-compose mounted volume (the wrong path is mounted, you must remove `docker volume` first).**

- Step 3.

````
v_num=1.2.1 docker-compose -f docker-compose-quick-start.yml up
````

## User compile way
> **ps**
> 1. The `streamx jar` used to build the image by yourself is the `jar` released in `git`.
> 2. Users can replace the script in `start.sh` and replace the `jar` pulled by `git` with the jar compiled by the user.
>
-Requirement
    1. docker environment
    2. docker-compose environment

- Step 1. (the configuration plug-in of the stream server is required, you can follow this example to mount other parts to the host.)

````
mkdir workspace
````

- Step 2.

````
sh build.sh v1.2.1
````

**PS This version number must be the same as the Git Tag on Git**

- Step 3.

````
Replace `./workspace` in `docker-compose` with an absolute path, the volume is mounted to expose the `streamx` configuration directory.
````
**PS docker-compose mounted volume (the wrong path is mounted, you must remove `docker volume` first).**

- Step 4.

````
v_num=1.2.1 docker-compose up
````
**PS: This version number only needs number.number.number, which is consistent with the Git Tag number**
