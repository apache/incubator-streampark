import com.streamxhub.streamx.console.StreamXConsole;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.service.ApplicationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StreamXConsole.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StreamXConsoleTest {

    @Autowired
    private ApplicationService applicationService;

    @Test
    public void start() throws Exception {
        Application application = new Application();
        application.setId(1304056220683497473L);
        application.setFlameGraph(false);
        application.setRestart(false);
        application.setSavePointed(false);
        application.setAllowNonRestored(false);

        boolean status = applicationService.start(application);
        System.out.println(status);
    }

}
