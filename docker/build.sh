# Any Error will stop build.
set -e
if [ ! $1 ]; then
  echo "Please input Build version"
  exit 0
else
  version=$1
  v_num=`echo $version | sed -r "s/.*([0-9]+\.[0-9]+\.[0-9]+).*/\1/g"`
  echo $v_num
fi
echo "Build version is: $1";

# mysql
wget  https://raw.githubusercontent.com/streamxhub/streamx/$version/streamx-console/streamx-console-service/src/assembly/script/final/v$v_num.sql
mv v$v_num.sql streamx_init.sql
sed -i '1i\use streamx;' streamx_init.sql
sed -i '1i\CREATE DATABASE IF NOT EXISTS streamx;' streamx_init.sql
mv -f streamx_init.sql ./mysql/
docker build -t streamx/mysql:$v_num mysql
wget -O jdk-8.tar.gz --no-check-certificate https://mirrors.tuna.tsinghua.edu.cn/AdoptOpenJDK/8/jdk/x64/linux/OpenJDK8U-jdk_x64_linux_openj9_linuxXL_8u282b08_openj9-0.24.0.tar.gz
mv -f jdk-8.tar.gz ./streamx/
wget  --no-check-certificate  https://github.com/streamxhub/streamx/releases/download/$1/streamx-console-service-${v_num}-bin.tar.gz
mv -f streamx-console-service-$v_num-bin.tar.gz ./streamx/
docker build --build-arg streamx_version=$v_num -t streamx/streamx-console-service:$v_num streamx

