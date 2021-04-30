import com.streamxhub.streamx.console.core.entity.Project;
import com.streamxhub.streamx.console.core.enums.GitAuthorizedError;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class GitTest {

    private Project project = new Project();

    @Before
    public void before() {
        project.setUrl("https://github.com/streamxhub/streamx-quickstart");
    }

    @Test
    public void getBranchs() {
        List<String> branches =  project.getAllBranches();
        branches.forEach(System.out::println);
    }

    @Test
    public void auth() {
        GitAuthorizedError error =  project.gitCheck();
        System.out.println(error);
    }

}
