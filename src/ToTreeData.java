import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import interfeces.ColumnInfo;
import interfeces.TableInfo;

public class ToTreeData {

	private static String maybeQuoted(String value)
	{
		if (value.contains(" "))
			return String.format("'%s'", value);
		else
			return value;
	}

	public static DefaultMutableTreeNode fromContinuationList(String caption, String[] values) {
		if (values.length == 0)
			return null;

		DefaultMutableTreeNode root = new DefaultMutableTreeNode(caption);
		for (String s : values) 
			root.add(new DefaultMutableTreeNode(s + " "));
		
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
