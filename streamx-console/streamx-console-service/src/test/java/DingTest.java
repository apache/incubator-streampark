import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.streamxhub.streamx.console.StreamXConsole;
import com.streamxhub.streamx.console.base.properties.DingdingProperties;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.enums.FlinkAppState;
import com.streamxhub.streamx.console.core.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Date;

@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
public class DingTest {

    private DingdingProperties dingdingProperties;

    @Before
    public void initDingding(){
        dingdingProperties = new DingdingProperties();
        dingdingProperties.setEnabled(true);
        dingdingProperties.setSecret("");
        dingdingProperties.setAccessToken("");
        dingdingProperties.setUrl("https://oapi.dingtalk.com/robot/send?access_token=%s&timestamp=%s&sign=%s");
    }

    @Test
    public void test() {
        Application application = new Application();
        application.setId(234234L);
        application.setStartTime(new Date());
        application.setJobName("Test My Job");
        application.setAppId("1234567890");
        application.setAlertEmail("******");

        application.setRestartCount(5);
        application.setRestartSize(100);

        application.setCpFailureAction(1);
        application.setCpFailureRateInterval(30);
        application.setCpMaxFailureInterval(5);
        sendDing(application);
    }

    private void sendDing(Application application){
        try {
            if (dingdingProperties.isEnabled()) {
                String msg = "StreamX >>>>>>>>> ID: %s,JOB NAME: %s FAIL,SavePointed: %s ,SavePoint: %s";
                String content = String.format(msg, application.getId(),application.getJobName(),application.getSavePointed(),application.getSavePoint());
                Long timestamp = System.currentTimeMillis();
                String secret = dingdingProperties.getSecret();
                String stringToSign = timestamp + "\n" + secret;
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
                byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
                String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), "UTF-8");
                String url = String.format(dingdingProperties.getUrl(), dingdingProperties.getAccessToken(), timestamp, sign);
                DingTalkClient client = new DefaultDingTalkClient(url);
                OapiRobotSendRequest request = new OapiRobotSendRequest();
                request.setMsgtype("text");
                OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
                text.setContent(content);
                request.setText(text);
                OapiRobotSendResponse response = client.execute(request);
                if (response.isSuccess()) {
                    log.info("Send dingding success, msg = {}", content);
                } else {
                    log.error("Send dingding fail , errorMsg = {},content = {}", response.getErrmsg(), content);
                }
            } else {
                log.info("Send dingding  enabled is false,Please set streamx.dingding.enabled is true if you want to send dingding alert msg !");
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }
    }
}
