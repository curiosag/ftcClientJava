package ftcClientCodeCompletionExperiments;

import com.google.common.eventbus.Subscribe;

public class EventBusSimpleListener {
    @Subscribe
    public void task(String s) {
		System.out.println("simply heard: " +  s);
    }
}
