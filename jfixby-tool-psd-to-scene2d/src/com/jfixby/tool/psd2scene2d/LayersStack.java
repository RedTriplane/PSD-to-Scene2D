
package com.jfixby.tool.psd2scene2d;

import com.jfixby.psd.unpacker.api.PSDLayer;
import com.jfixby.scarabei.api.collections.Collections;
import com.jfixby.scarabei.api.collections.List;

public class LayersStack {
	final List<PSDLayer> stack = Collections.newList();

	public void pop (final PSDLayer input) {
		this.stack.removeLast();
	}

	public void push (final PSDLayer input) {
		this.stack.add(input);
	}

// public void print () {
// this.stack.print("LayersStack");
// }

}
