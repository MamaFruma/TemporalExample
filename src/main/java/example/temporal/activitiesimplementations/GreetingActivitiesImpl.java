package example.temporal.activitiesimplementations;

import example.temporal.activities.GreetingActivities;
import org.springframework.stereotype.Component;

@Component
public class GreetingActivitiesImpl implements GreetingActivities {

  @Override
  public String composeGreeting(String greeting, String name) {
    return greeting + " " + name + "!";
  }
}
