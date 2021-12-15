mvn package -DskipTests -Dskip.npm  -Denv=prod 
cp streamx-console/streamx-console-service/target/streamx-console-service-1.2.0-bin.tar.gz ~/
cd ~/
tar zxvf streamx-console-service-1.2.0-bin.tar.gz
