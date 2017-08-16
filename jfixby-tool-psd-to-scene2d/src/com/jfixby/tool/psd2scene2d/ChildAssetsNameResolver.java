
package com.jfixby.tool.psd2scene2d;

import com.jfixby.psd.unpacker.api.PSDLayer;
import com.jfixby.scarabei.api.names.ID;

public interface ChildAssetsNameResolver {

	ID getPSDLayerName (PSDLayer input);

	ID childAnimation (String child_id);

	ID childEvent (String child_id);

	ID childInput (String child_id);

	ID childText (String child_id);

}
