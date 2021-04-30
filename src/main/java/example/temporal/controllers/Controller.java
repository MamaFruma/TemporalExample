package example.temporal.controllers;

import example.temporal.workflows.GreetingWorkflow;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;

import java.util.List;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.toList;

@Slf4j
@RestController
@RequestMapping("/test/workflow")
public class Controller {

  private final WorkflowClient workflowClient;
  private final WorkflowOptions helloOptions;
  private final Random random = new Random();

  public Controller(
          WorkflowClient workflowClient,
          WorkflowOptions helloOptions
  ) {
    this.workflowClient = workflowClient;
    this.helloOptions = helloOptions;
  }

  @GetMapping("/hello")
  @ResponseStatus(value = HttpStatus.OK)
  public void message(@RequestParam String name) {
    GreetingWorkflow greetingWorkflow = workflowClient.newWorkflowStub(GreetingWorkflow.class, helloOptions);
    greetingWorkflow.getGreeting(name);
  }
}
