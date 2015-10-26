import com.google.common.base.Optional;

public interface ItemChosenHandler {

	public void onItemChosen(Optional<String> tableName, Optional<String> columnName);

}
