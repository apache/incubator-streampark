# StreamX
let't spark|flink easy
    
### BUILD和使用方法
#### 1.构建方法

```
    $git clone https://github.com/streamxhub/StreamX.git
    $cd StreamX
    $mvn clean install

    maven构建时已默认在pom中跳过了所有的test代码的编译和测试
    构建完成之后会将jar安装到m2的对应路径下，使用时在自己的项目的pom.xml文件里添加
    
    <dependency>
        <groupId>com.streamxhub</groupId>
        <artifactId>streamx-flink-core</artifactId>
    	<version>1.0.0</version>
    </dependency>
```

