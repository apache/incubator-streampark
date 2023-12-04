
### 1. linux Creates Python virtual environments

#### 1.1. Prepare the 'setup-pyflink-virtual-env.sh script'. The default version of 'apache-flink' is '1.16.2'. You can change the version as required.The content is as follows

```shell
set -e
# Download the Python 3.7 miniconda.sh script.
wget "https://repo.continuum.io/miniconda/Miniconda3-py37_4.9.2-Linux-x86_64.sh" -O "miniconda.sh"

# Add execute permissions to the Python 3.7 miniconda.sh script.
chmod +x miniconda.sh

# Create a Python virtual environment.
./miniconda.sh -b -p venv

# Activate the Conda Python virtual environment.
source venv/bin/activate ""

# Install PyFlink dependencies.
pip install "apache-flink==1.16.2"

# Exit the Conda Python virtual environment.
conda deactivate

# Delete the cached packages.
rm -rf venv/pkgs

# Package the prepared Conda Python virtual environment.
zip -r venv.zip venv
```

#### 1.2. Prepare the 'build.sh script'. The content is as follows

The '/build' directory needs to be created by itself. You can also change it to another directory

```shell
#!/bin/bash
set -e -x
yum install -y zip wget

cd /root/
bash /build/setup-pyflink-virtual-env.sh
mv venv.zip /build/
```

#### 1.3. Execute the following command

```shell
docker run -it --rm -v $PWD:/build  -w /build quay.io/pypa/manylinux2014_x86_64 ./build.sh
```
After this command is executed, a file named venv.zip will be generated, which is the virtual environment of Python 3.7. You can also modify the above script, install another version of the Python virtual environment, or install the required third-party Python packages in the virtual environment.

### 2. Upload venv.zip to hdfs

venv.zip is about 539M in size and needs to be uploaded to hdfs by itself

```shell
hadoop fs -put ./venv.zip /streampark/python
```

### 3. Copy venv.zip to $WORKSPACE/python

```shell
copy ./venv.zip $WORKSPACE/python
```

### 4. Copy Python dependencies to $FLINK_HOME/lib

```shell
cp -r $FLINK_HOME/opt/python  $FLINK_HOME/lib

cp  $FLINK_HOME/opt/flink-python-* $FLINK_HOME/lib
```

### 5. If you use a flink connector dependency in your pyflink job, you need to put it in $FLINK_HOME/lib

### 6. Reference document
```text
https://help.aliyun.com/document_detail/413966.html#:~:text=.%2Fsetup-pyflink-%20virtual%20-env.sh,%E8%AF%A5%E5%91%BD%E4%BB%A4%E6%89%A7%E8%A1%8C%E5%AE%8C%E6%88%90%E5%90%8E%EF%BC%8C%E4%BC%9A%E7%94%9F%E6%88%90%E4%B8%80%E4%B8%AA%E5%90%8D%E4%B8%BA%20venv%20%E7%9A%84%E7%9B%AE%E5%BD%95%EF%BC%8C%E5%8D%B3%E4%B8%BAPython%203.6%E7%9A%84%E8%99%9A%E6%8B%9F%E7%8E%AF%E5%A2%83%E3%80%82

https://nightlies.apache.org/flink/flink-docs-release-1.16/zh/docs/deployment/cli/#submitting-pyflink-jobs

https://nightlies.apache.org/flink/flink-docs-release-1.16/zh/docs/dev/python/python_config/
```


