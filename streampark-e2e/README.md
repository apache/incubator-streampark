<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~
  -->
# StreamPark End-to-End Test

## Page Object Model

StreamPark End-to-End test respects
the [Page Object Model (POM)](https://www.selenium.dev/documentation/guidelines/page_object_models/) design pattern.
Every page of StreamPark is abstracted into a class for better maintainability.

### Example

The login page is abstracted
as [`LoginPage`](streampark-e2e-case/src/test/java/org/apache/streampark/e2e/pages/LoginPage.java), with the
following fields,

```java
public final class LoginPage {
    @FindBy(id = "form_item_account")
    private WebElement inputUsername;

    @FindBy(id = "form_item_password")
    private WebElement inputPassword;

    @FindBy(xpath = "//button[contains(@classnames, 'login-button')]")
    private WebElement buttonLogin;
}
```

where `inputUsername`, `inputPassword` and `buttonLogin` are the main elements on UI that we are interested in. They are
annotated with `FindBy` so that the test framework knows how to locate the elements on UI. You can locate the elements
by `id`, `className`, `css` selector, `tagName`, or even `xpath`, please refer
to [the JavaDoc](https://www.selenium.dev/selenium/docs/api/java/org/openqa/selenium/support/FindBy.html).

**Note:** for better maintainability, it's essential to add some convenient `id` or `class` on UI for the wanted
elements if needed, avoid using too complex `xpath` selector or `css` selector that is not maintainable when UI have
styles changes.

With those fields declared, we should also initialize them with a web driver. Here we pass the web driver into the
constructor and invoke `PageFactory.initElements` to initialize those fields,

```java
public final class LoginPage {
    // ...
    public LoginPage(RemoteWebDriver driver) {
        this.driver = driver;

        PageFactory.initElements(driver, this);
    }
}
```

then, all those UI elements are properly filled in.

## Test Environment Setup

StreamPark End-to-End test uses [testcontainers](https://www.testcontainers.org) to set up the testing
environment, with docker compose.

Typically, every test case needs one or more `docker-compose.yaml` files to set up all needed components, and expose the
StreamPark UI port for testing. You can use `@StreamPark(composeFiles = "")` and pass
the `docker-compose.yaml` files to automatically set up the environment in the test class.

```java

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
class UserManagementTest {
}
```

You can get the web driver that is ready for testing in the class by adding a field of type `RemoteWebDriver`, which
will be automatically injected via the testing framework.

```java

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
class UserManagementTest {
    private RemoteWebDriver browser;
}
```

Then the field `browser` can be used in the test methods.

```java

@StreamPark(composeFiles = "docker/basic/docker-compose.yaml")
class UserManagementTest {
    private RemoteWebDriver browser;

    @Test
    @Order(10)
    void testCreateUser() {
        final UserManagementPage userManagementPage = new UserManagementPage(browser);
        userManagementPage.createUser(newUserName, "test", password, newUserEmail, UserManagementUserType.ADMIN);

        Awaitility.await().untilAsserted(() -> assertThat(userManagementPage.userList())
            .as("User list should contain newly-created user")
            .extracting(WebElement::getText)
            .anyMatch(it -> it.contains(newUserName)));
    }
}
```

## Notes

- For UI tests, it's common that the pages might need some time to load, or the operations might need some time to
  complete, we can use `await().untilAsserted(() -> {})` to wait for the assertions. And use `new webDriverWait(driver, ExpectedConditions)` to wait for the elements to be present or clickable.
- For better maintainability, it's recommended to abstract the pages into classes, and use the Page Object Model design
  pattern.


## Local development

### Mac M1
Add VM options to the test configuration in IntelliJ IDEA:
```
# In this mode you need to install docker desktop for mac and run it with locally
-Dm1_chip=true
```

### Running locally(without Docker)
```
# In this mode you need to start frontend and backend services locally
-Dlocal=true
```

### Running locally(with Docker)
```
# In this mode you only need to install docker locally
```

- To run the tests locally, you need to have the StreamPark running locally. You should add `streampark-e2e/pom.xml` to the maven project
Since it does not participate in project compilation, it is not in the main project.
- Running run test class `org.apache.streampark.e2e.cases.UserManagementTest` in the IDE. After execution, the test video will be saved as mp4 in a local temporary directory. Such as
`/var/folders/hf/123/T/record-3123/PASSED-[engine:junit-jupiter]/[class:org.apache.streampark.e2e.cases.UserManagementTest]-20240606-152333.mp4`
