package example.temporal.workflowimplementations;

import example.temporal.activities.GreetingActivities;
import example.temporal.workflows.GreetingWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Workflow;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Scope("REQUEST")
public class GreetingWorkflowImpl implements GreetingWorkflow {

    private final GreetingActivities greetingActivities;

    public GreetingWorkflowImpl() {
        ActivityOptions activityOptions = ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(10)).build();
        this.greetingActivities = Workflow.newActivityStub(GreetingActivities.class, activityOptions);
    }
    @Override
    public void getGreeting(String name) {
        //Async.function(greetingActivities::composeGreeting,"Hello", name);
    }
}
