package design.jobx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamxhub.flink.monitor.core.entity.Application;
import com.streamxhub.flink.monitor.core.metrics.flink.JobsOverview;
import com.streamxhub.flink.monitor.core.metrics.yarn.AppInfo;
import org.dom4j.*;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Iterator;

public class RestJsonTest {

    @Test
    public void jobOverview() throws IOException {
        String json = "{\"jobs\":[{\"jid\":\"3c18eecc9aa1d7e2062d0beb7c31fac0\",\"name\":\"Flink_Park_Order_App\",\"state\":\"RUNNING\",\"start-time\":1597075299228,\"end-time\":-1,\"duration\":135989825,\"last-modification\":1597075315920,\"tasks\":{\"total\":2,\"created\":0,\"scheduled\":0,\"deploying\":0,\"running\":2,\"finished\":0,\"canceling\":0,\"canceled\":0,\"failed\":0,\"reconciling\":0}},{\"jid\":\"f66fe14e7510211cb4bf5bd2dbb8f589\",\"name\":\"Flink_BJSH_TJDEas_App\",\"state\":\"RUNNING\",\"start-time\":1597075223116,\"end-time\":-1,\"duration\":136065937,\"last-modification\":1597207717267,\"tasks\":{\"total\":63,\"created\":0,\"scheduled\":0,\"deploying\":0,\"running\":63,\"finished\":0,\"canceling\":0,\"canceled\":0,\"failed\":0,\"reconciling\":0}},{\"jid\":\"ecd5495ab73262441a488b1c6d8170cd\",\"name\":\"Flink_JieSun_Eas_App\",\"state\":\"RUNNING\",\"start-time\":1597075263069,\"end-time\":-1,\"duration\":136025984,\"last-modification\":1597075277350,\"tasks\":{\"total\":3,\"created\":0,\"scheduled\":0,\"deploying\":0,\"running\":3,\"finished\":0,\"canceling\":0,\"canceled\":0,\"failed\":0,\"reconciling\":0}},{\"jid\":\"5da741e6fc6d9ecdb667dcf3bf7c4a5b\",\"name\":\"Flink_Gmv_FLow_App\",\"state\":\"RUNNING\",\"start-time\":1596853431410,\"end-time\":-1,\"duration\":357857643,\"last-modification\":1596853445630,\"tasks\":{\"total\":8,\"created\":0,\"scheduled\":0,\"deploying\":0,\"running\":8,\"finished\":0,\"canceling\":0,\"canceled\":0,\"failed\":0,\"reconciling\":0}},{\"jid\":\"77c1939604e7cc19af231c0826535347\",\"name\":\"Flink_GZHZ_TJDEas_App\",\"state\":\"RUNNING\",\"start-time\":1597075178200,\"end-time\":-1,\"duration\":136110853,\"last-modification\":1597159721033,\"tasks\":{\"total\":54,\"created\":0,\"scheduled\":0,\"deploying\":0,\"running\":54,\"finished\":0,\"canceling\":0,\"canceled\":0,\"failed\":0,\"reconciling\":0}},{\"jid\":\"a58d63649009120a76784198ad866bea\",\"name\":\"Flink_TJD_Source\",\"state\":\"RUNNING\",\"start-time\":1597137907279,\"end-time\":-1,\"duration\":73381774,\"last-modification\":1597137909326,\"tasks\":{\"total\":56,\"created\":0,\"scheduled\":0,\"deploying\":0,\"running\":56,\"finished\":0,\"canceling\":0,\"canceled\":0,\"failed\":0,\"reconciling\":0}},{\"jid\":\"abfdc82b5945567090d44e40b7eae7ea\",\"name\":\"Flink_TJD_Carin_App\",\"state\":\"RUNNING\",\"start-time\":1596345358641,\"end-time\":-1,\"duration\":865930412,\"last-modification\":1596345368369,\"tasks\":{\"total\":39,\"created\":0,\"scheduled\":0,\"deploying\":0,\"running\":39,\"finished\":0,\"canceling\":0,\"canceled\":0,\"failed\":0,\"reconciling\":0}},{\"jid\":\"538bc2dfa2b569dae5614ebbc906f79d\",\"name\":\"Flink_JSGMV_Source\",\"state\":\"RUNNING\",\"start-time\":1597137929192,\"end-time\":-1,\"duration\":73359861,\"last-modification\":1597137941782,\"tasks\":{\"total\":58,\"created\":0,\"scheduled\":0,\"deploying\":0,\"running\":58,\"finished\":0,\"canceling\":0,\"canceled\":0,\"failed\":0,\"reconciling\":0}}]}";
        ObjectMapper mapper = new ObjectMapper();
        Reader reader = new StringReader(json);
        JobsOverview jobsOverview = mapper.readValue(reader, JobsOverview.class);
        System.out.println(jobsOverview);
    }

    @Test
    public void em() throws Exception {

        String xml = "<app>\n" +
                "<id>application_1585401674210_281973</id>\n" +
                "<user>hst</user>\n" +
                "<name>HopsonFlink</name>\n" +
                "<queue>root.users.hst</queue>\n" +
                "<state>RUNNING</state>\n" +
                "<finalStatus>UNDEFINED</finalStatus>\n" +
                "<progress>100.0</progress>\n" +
                "<trackingUI>ApplicationMaster</trackingUI>\n" +
                "<trackingUrl>http://pro-hadoop-2:8088/proxy/application_1585401674210_281973/</trackingUrl>\n" +
                "<diagnostics/>\n" +
                "<clusterId>1585401674210</clusterId>\n" +
                "<applicationType>Apache Flink</applicationType>\n" +
                "<applicationTags/>\n" +
                "<startedTime>1596298971576</startedTime>\n" +
                "<finishedTime>0</finishedTime>\n" +
                "<elapsedTime>911638708</elapsedTime>\n" +
                "<amContainerLogs>http://pro-hadoop-7:8042/node/containerlogs/container_e06_1585401674210_281973_01_000001/hst</amContainerLogs>\n" +
                "<amHostHttpAddress>pro-hadoop-7:8042</amHostHttpAddress>\n" +
                "<allocatedMB>26624</allocatedMB>\n" +
                "<allocatedVCores>13</allocatedVCores>\n" +
                "<reservedMB>0</reservedMB>\n" +
                "<reservedVCores>0</reservedVCores>\n" +
                "<runningContainers>13</runningContainers>\n" +
                "<memorySeconds>21856904069</memorySeconds>\n" +
                "<vcoreSeconds>10672156</vcoreSeconds>\n" +
                "<preemptedResourceMB>0</preemptedResourceMB>\n" +
                "<preemptedResourceVCores>0</preemptedResourceVCores>\n" +
                "<numNonAMContainerPreempted>0</numNonAMContainerPreempted>\n" +
                "<numAMContainerPreempted>0</numAMContainerPreempted>\n" +
                "<logAggregationStatus>NOT_START</logAggregationStatus>\n" +
                "</app>";

        Document document = DocumentHelper.parseText(xml);

        //3.获取根节点
        Element rootElement = document.getRootElement();
        Iterator iterator = rootElement.elementIterator();

        AppInfo appInfo = new AppInfo();
        while (iterator.hasNext()) {
            Element element = (Element) iterator.next();
            Object value = element.getData();
            String name = element.getName();
            if (value != null && value.toString().trim().length() > 0) {
                Field field = AppInfo.class.getDeclaredField(name);
                field.setAccessible(true);
                Object v = null;
                Class<?> clazz = field.getType();
                if (clazz.equals(Float.class)) {
                    v = Float.parseFloat(value.toString());
                } else if (clazz.equals(Long.class)) {
                    v = Long.parseLong(value.toString());
                } else if (clazz.equals(Integer.class)) {
                    v = Integer.parseInt(value.toString());
                } else {
                    v = value.toString();
                }
                field.set(appInfo, v);
            }
        }
        System.out.println(appInfo);


    }

    @Test
    public void yarnAppInfo() throws Exception {
        Application application = new Application();
        application.setAppId("application_1587978117869_87565");
        AppInfo appInfo =  application.getYarnAppInfo();
        System.out.println(appInfo);

    }

}
