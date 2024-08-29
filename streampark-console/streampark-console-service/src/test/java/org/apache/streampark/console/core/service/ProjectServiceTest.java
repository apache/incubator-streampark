package org.apache.streampark.console.core.service;

import org.apache.streampark.console.SpringTestBase;
import org.apache.streampark.console.core.entity.Project;
import org.apache.streampark.console.core.enums.GitAuthorizedError;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ProjectServiceTest extends SpringTestBase {

  private final Project project = new Project();

  @Autowired private ProjectService projectService;

  @BeforeEach
  void before() {
    project.setUrl("git@github.com:apache/incubator-streampark.git");
  }

  @Disabled("This test case can't be runnable due to external service is not available.")
  @Test
  void testGitBranches() {
    List<String> branches = projectService.getAllBranches(project);
    branches.forEach(System.out::println);
  }

  @Disabled("This test case can't be runnable due to external service is not available.")
  @Test
  void testGitCheckAuth() {
    GitAuthorizedError error = projectService.gitCheck(project);
    System.out.println(error);
  }
}
