package ftcClientJava;

import java.util.List;

import manipulations.QueryPatching;
import structures.TableInfo;

public interface DataEngine {
	
	public List<TableInfo> getTableList(boolean addDetails);
	
	public QueryPatching getPatcher(String query, int cursorPos);
	
}
