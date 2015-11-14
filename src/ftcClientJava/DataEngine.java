package ftcClientJava;

import java.util.List;

import interfacing.TableInfo;
import manipulations.QueryPatching;

public interface DataEngine {
	
	public List<TableInfo> getTableList(boolean addDetails);
	
	public QueryPatching getPatcher(String query, int cursorPos);
	
}
