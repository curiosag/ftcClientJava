package ftcQueryEditor;

import java.util.List;

import interfaces.SyntaxElement;


public interface ExternalRSTATokenProvider {
	public List<SyntaxElement> getTokens(String query);
}
