# Deploy StreamPark on k8s

### 1. create template

```shell
helm template streampark/ -n default -f streampark/values.yaml --output-dir ./result
```

### 2. apply 

```shell
kubectl apply -f result/streampark/templates
```

### 3. open WebUI

http://${host}:10000

#### [more detail](streampark/templates/NOTES.txt)
