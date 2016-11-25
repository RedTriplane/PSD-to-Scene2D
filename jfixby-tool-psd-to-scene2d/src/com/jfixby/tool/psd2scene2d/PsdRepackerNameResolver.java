
package com.jfixby.tool.psd2scene2d;

import com.jfixby.cmns.api.assets.ID;
import com.jfixby.cmns.api.collections.Map;
import com.jfixby.psd.unpacker.api.PSDLayer;

public class PsdRepackerNameResolver implements ChildAssetsNameResolver {

	private final ID package_name;
	private final Map<PSDLayer, ID> raster_names;

	public PsdRepackerNameResolver (final ID package_name, final Map<PSDLayer, ID> raster_names) {
		this.package_name = package_name;
		this.raster_names = raster_names;
	}

	@Override
	public ID getPSDLayerName (final PSDLayer input) {
		return this.raster_names.get(input);
	}

	@Override
	public ID childAnimation (final String child_id) {
		return this.package_name.child(child_id);
	}

	@Override
	public ID childEvent (final String child_id) {
		return this.package_name.child(child_id);
	}

	@Override
	public ID childInput (final String child_id) {
		return this.package_name.child(child_id);
	}

	@Override
	public ID childText (final String child_id) {
		return this.package_name.child("text").child(child_id);
	}

}
