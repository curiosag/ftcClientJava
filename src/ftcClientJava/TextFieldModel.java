package ftcClientJava;

import javax.swing.text.AbstractDocument;
import javax.swing.text.Element;

public class TextFieldModel extends AbstractDocument {

	public TextFieldModel(Content data) {
		super(data);
		// TODO Auto-generated constructor stub
	}

	public TextFieldModel(Content data, AttributeContext context) {
		super(data, context);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Element getDefaultRootElement() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Element getParagraphElement(int pos) {
		// TODO Auto-generated method stub
		return null;
	}

}
