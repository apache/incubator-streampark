mvn clean package -DskipTests -Dcheckstyle.skip  -Denv=prod 
cp streamx-console/streamx-console-service/target/streamx-console-service-1.2.1-bin.tar.gz ~/
cd ~/
tar zxvf streamx-console-service-1.2.1-bin.tar.gz
