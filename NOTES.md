# My test's notes

```bash
# Get sources
git clone https://github.com/EyeSeeTea/d2-docker
cd d2-docker
git checkout development

# "Install" according to README
sudo python3 setup.py install

# Build "latest v40 core image", using latest tag matching 40 https://github.com/dhis2/dhis2-core/releases/tag/2.40.5
d2-docker create core docker.eyeseetea.com/eyeseetea/dhis2-core:2.40.5 --version=2.40.5

# Get Sierra Leone database, link found at https://dhis2.org/downloads/ bottom of the page, adapted version
wget https://databases.dhis2.org/sierra-leone/2.40.5/dhis2-db-sierra-leone.sql.gz -O sierra-db.sql.gz

# Build "latest v40 data image"
d2-docker create data docker.eyeseetea.com/eyeseetea/dhis2-data:2.40.5-sierra --sql=sierra-db.sql.gz

# Start
d2-docker start docker.eyeseetea.com/eyeseetea/dhis2-data:2.40.5-sierra

# Head to http://localhost:8080
```

The error's log

```log
gateway-1  | nginx.1     | 172.19.0.1 - - [17/Oct/2024:17:28:31 +0200] "GET /dhis-web-tracker-capture/views/modal.html HTTP/1.1" 200 213 "http://localhost:8080/dhis-web-tracker-capture/index.html" "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36"
core-1     | * ERROR 2024-10-17T17:28:35,785 java.lang.Exception (CrudControllerAdvice.java [http-nio-8080-exec-1]) IDd9bcK7639shA+LDaKS6SkILplBPcAbcR8bjZgTnreCw= 
core-1     | java.lang.NullPointerException: null
core-1     |    at org.hisp.dhis.dxf2.events.importer.shared.postprocess.EventFileResourcePostProcessor.process(EventFileResourcePostProcessor.java:54) ~[dhis-service-dxf2-2.40.5.jar:?]
core-1     |    at org.hisp.dhis.dxf2.events.importer.EventProcessorExecutor.lambda$execute$0(EventProcessorExecutor.java:50) ~[dhis-service-dxf2-2.40.5.jar:?]
core-1     |    at java.base/java.util.ArrayList.forEach(Unknown Source) ~[?:?]
core-1     |    at org.hisp.dhis.dxf2.events.importer.EventProcessorExecutor.lambda$execute$1(EventProcessorExecutor.java:50) ~[dhis-service-dxf2-2.40.5.jar:?]
core-1     |    at java.base/java.lang.Iterable.forEach(Unknown Source) ~[?:?]
core-1     |    at org.hisp.dhis.dxf2.events.importer.EventProcessorExecutor.execute(EventProcessorExecutor.java:49) ~[dhis-service-dxf2-2.40.5.jar:?]
core-1     |    at org.hisp.dhis.dxf2.events.importer.EventManager.updateEventDataValues(EventManager.java:118) ~[dhis-service-dxf2-2.40.5.jar:?]
core-1     |    at org.hisp.dhis.dxf2.events.event.AbstractEventService.updateEventDataValues(AbstractEventService.java:769) ~[dhis-service-dxf2-2.40.5.jar:?]
```

Reproduce as curl, taken from my browser's network console

```bash
curl 'http://localhost:8080/api/events/OXEx1mAXxvB/uf3svrmp8Oj' \
  -X 'PUT' \
  -H 'Accept: application/json, text/plain, */*' \
  -H 'Accept-Language: fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7' \
  -H 'Cache-Control: no-cache' \
  -H 'Connection: keep-alive' \
  -H 'Content-Type: application/json;charset=UTF-8' \
  -H 'Cookie: JSESSIONID=04E3BCFB44159EE1983EEFB94476DCCC' \
  -H 'Origin: http://localhost:8080' \
  -H 'Pragma: no-cache' \
  -H 'Referer: http://localhost:8080/dhis-web-tracker-capture/index.html' \
  -H 'Sec-Fetch-Dest: empty' \
  -H 'Sec-Fetch-Mode: cors' \
  -H 'Sec-Fetch-Site: same-origin' \
  -H 'User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36' \
  -H 'sec-ch-ua: "Chromium";v="129", "Not=A?Brand";v="8"' \
  -H 'sec-ch-ua-mobile: ?0' \
  -H 'sec-ch-ua-platform: "Linux"' \
  --data-raw '{"event":"OXEx1mAXxvB","orgUnit":"DiszpKrYNg8","program":"IpHINAT79UW","programStage":"A03MvHHogjR","status":"ACTIVE","trackedEntityInstance":"r2FEXpX6ize","dataValues":[{"dataElement":"uf3svrmp8Oj","value":null,"providedElsewhere":false}]}'
```

Browse the sources

```bash
git clone https://github.com/dhis2/dhis2-core
cd dhis2-core
git checkout v2.40.5
find . -name EventFileResourcePostProcessor.java
vim ./dhis-2/dhis-services/dhis-service-dxf2/src/main/java/org/hisp/dhis/dxf2/events/importer/shared/postprocess/EventFileResourcePostProcessor.java
```

Error's come from the if clause, can only be because fileResource is null.

```java
      if (dataElement.isFileType()) {
        FileResource fileResource = fileResourceService.getFileResource(dataValue.getValue());

        if (!fileResource.isAssigned() || fileResource.getFileResourceOwner() == null) {  // <=====
          fileResource.setAssigned(true);
          fileResource.setFileResourceOwner(event.getEvent());
          fileResourceService.updateFileResource(fileResource);
        }
      }
```

Fix

```java
        if (fileResource != null && (!fileResource.isAssigned() || fileResource.getFileResourceOwner() == null)) {
```

Build

```bash
./dhis-2/build-dev.sh

docker images | grep dhis
# docker.eyeseetea.com/eyeseetea/dhis2-data                                2.40.5-sierra         a3ad1bceac12   16 hours ago    92.7MB
# docker.eyeseetea.com/eyeseetea/dhis2-core                                2.40.5                82125fc3ae7d   16 hours ago    606MB
# dhis2/core-dev                                                           local                 a1bb45a5dace   54 years ago    889MB
```

Check

```bash
# Stop previous one
d2-docker stop docker.eyeseetea.com/eyeseetea/dhis2-data:2.40.5-sierra

# Start the same data image, using localy built fix
d2-docker start docker.eyeseetea.com/eyeseetea/dhis2-data:2.40.5-sierra --core-image dhis2/core-dev:local

# Doesn't work somehow, stuck starting

# Try starting it another way 
DHIS2_IMAGE=dhis2/core-dev:local DHIS2_DB_DUMP_URL=https://databases.dhis2.org/sierra-leone/2.40.5/dhis2-db-sierra-leone.sql.gz docker compose up

# Head to http://localhost:8080, it works now
```

Using the API, with inspiration from https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-240/introduction.html


```bash
curl 'http://localhost:8080/api/events/OXEx1mAXxvB/uf3svrmp8Oj' \
  -X 'PUT' \
  -H "Authorization: Basic $(echo -n admin:district | base64)" \
  -H 'Content-Type: application/json' \
  --data-raw '{"event":"OXEx1mAXxvB","orgUnit":"DiszpKrYNg8","program":"IpHINAT79UW","programStage":"A03MvHHogjR","status":"ACTIVE","trackedEntityInstance":"r2FEXpX6ize","dataValues":[{"dataElement":"uf3svrmp8Oj","value":null,"providedElsewhere":false}]}'

# {"httpStatus":"OK","httpStatusCode":200,"status":"OK","message":"Import was successful.","response":{"responseType":"ImportSummary","status":"SUCCESS","importCount":{"imported":0,"updated":1,"ignored":0,"deleted":0},"conflicts":[],"rejectedIndexes":[],"reference":"OXEx1mAXxvB"}}
```


Push the image to harbor

```bash
docker login https://docker.eyeseetea.com
# username: remy-garrigue 
# password: Remy-garrigue1234
docker tag dhis2/core-dev:local docker.eyeseetea.com/remy-garrigue/core-dev:local
docker push docker.eyeseetea.com/remy-garrigue/core-dev:local
```