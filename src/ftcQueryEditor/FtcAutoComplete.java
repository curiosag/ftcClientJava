package ftcQueryEditor;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.JTextComponent;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ParameterChoicesProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.autocomplete.TemplateCompletion;
import org.fife.ui.autocomplete.ParameterizedCompletion.Parameter;

import interfacing.SyntaxElementSource;

public class FtcAutoComplete {

	private final CompletionProvider provider;
	private final AutoCompletion ac;
	private final SyntaxElementSource syntaxElementSource;
	
	public FtcAutoComplete(SyntaxElementSource syntaxElementSource){
		this.syntaxElementSource = syntaxElementSource;
		
		provider = new FtcCompletionProvider(syntaxElementSource); //createCompletionProvider();
		
		ac = new AutoCompletion(provider);
		ac.setParameterAssistanceEnabled(false);
		
	}
	
	public void install(JTextComponent c) {
		ac.install(c);
	}

	private CompletionProvider createCompletionProvider() {

		final DefaultCompletionProvider provider = new FtcCompletionProvider(syntaxElementSource);
		ParameterChoicesProvider pcp = new ParameterChoicesProvider(){

			@Override
			public List<Completion> getParameterChoices(JTextComponent tc, Parameter param) {
				ArrayList<Completion> p = new ArrayList<Completion>();
				p.add(new BasicCompletion(provider, "p1"));
				p.add(new BasicCompletion(provider, "p2"));
				p.add(new BasicCompletion(provider, "p3"));
				return p;
			}};
			
		provider.setParameterChoicesProvider(pcp);
		
		ArrayList<Completion> subs = new ArrayList<Completion>();
		
		
		subs.add(new TemplateCompletion(provider, "x", "forlp", "for (int ${i} = 0; ${i} &lt; ${array}.length; ${i}++)"));	
		subs.add(new BasicCompletion(provider, "b"));
		subs.add(new BasicCompletion(provider, "s2"));
		subs.add(new BasicCompletion(provider, "s3"));
		provider.addCompletion(new BasicCompletion(provider, "s", subs));
		
		// Add completions for all Java keywords. A BasicCompletion is just
		// a straightforward word completion.
		TemplateCompletion tc = new TemplateCompletion(provider, "x", "x", "for (int ${i} = 0; ${i} &lt; ${array}.length; ${i}++)");
		
		provider.addCompletion(tc);
		
		
		
		provider.addCompletion(new BasicCompletion(provider, "assert"));
		provider.addCompletion(new BasicCompletion(provider, "break"));
		provider.addCompletion(new BasicCompletion(provider, "case"));
		// ... etc ...
		provider.addCompletion(new BasicCompletion(provider, "transient"));
		provider.addCompletion(new BasicCompletion(provider, "try"));
		provider.addCompletion(new BasicCompletion(provider, "void"));
		provider.addCompletion(new BasicCompletion(provider, "volatile"));
		provider.addCompletion(new BasicCompletion(provider, "while"));

		// Add a couple of "shorthand" completions. These completions don't
		// require the input text to be the same thing as the replacement text.
		provider.addCompletion(
				new ShorthandCompletion(provider, "sysout", "System.out.println(", "System.out.println("));
		provider.addCompletion(
				new ShorthandCompletion(provider, "syserr", "System.err.println(", "System.err.println("));

		return provider;

	}

	
}
