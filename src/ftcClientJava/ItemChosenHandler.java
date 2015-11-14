package ftcClientJava;
import com.google.common.base.Optional;

public interface ItemChosenHandler {

	public void onItemChosen(Optional<String> parentItem, Optional<Object> item);

}
