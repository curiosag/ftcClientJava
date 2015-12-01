package ftcQueryEditor;

import java.util.List;

import interfacing.SyntaxElement;


public interface ExternalRSTATokenProvider {
	public List<SyntaxElement> getTokens(String query);
}
