
public class Client {
	public static void main(String[] args) {
		// Schedule a job for the event dispatching thread:
		Gui.runDeferred(() -> Gui.createAndShowGUI());
	}
}
