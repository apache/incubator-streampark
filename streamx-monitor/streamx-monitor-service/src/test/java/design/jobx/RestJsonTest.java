package design.jobx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamxhub.flink.monitor.core.enums.AppState;
import com.streamxhub.flink.monitor.core.metrics.flink.JobsOverview;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

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
    public void em() {

        System.out.println(AppState.valueOf("RUNNING").getValue());

    }

}
