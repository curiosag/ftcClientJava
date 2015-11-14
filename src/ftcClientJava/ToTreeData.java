package ftcClientJava;
import java.util.List;


import javax.swing.tree.DefaultMutableTreeNode;

import interfacing.ColumnInfo;
import interfacing.Completion;
import interfacing.Completions;
import interfacing.TableInfo;

public class ToTreeData {

	private static String maybeQuoted(String value)
	{
		if (value.contains(" "))
			return String.format("'%s'", value);
		else
			return value;
	}
	
	public static DefaultMutableTreeNode fromContinuationList(String caption, Completions values) {
		if (values.size() == 0)
			return null;

		DefaultMutableTreeNode root = new DefaultMutableTreeNode(caption);
		for (Completion c : values.getAll()) 
			root.add(new DefaultMutableTreeNode(c));
		
		return root;
	}

	public static DefaultMutableTreeNode fromTableInfo(String caption, List<TableInfo> tables) {
		if (tables.size() == 0)
			return null;

		DefaultMutableTreeNode root = new DefaultMutableTreeNode(caption);
		for (TableInfo t : tables)
			append(root, t);

		return root;
	}

	private static void append(DefaultMutableTreeNode root, TableInfo t) {
		DefaultMutableTreeNode table = new DefaultMutableTreeNode(maybeQuoted(t.name));
		
		for (ColumnInfo c : t.columns)
			table.add(new DefaultMutableTreeNode(maybeQuoted(c.name)));
		
		root.add(table);
	}

}
