
package com.jfixby.tool.psd2scene2d;

import com.jfixby.cmns.api.assets.AssetID;
import com.jfixby.cmns.api.collections.Map;
import com.jfixby.psd.unpacker.api.PSDLayer;

public class PsdRepackerNameResolver implements ChildAssetsNameResolver {

	private final AssetID package_name;
	private final Map<PSDLayer, AssetID> raster_names;

	public PsdRepackerNameResolver (final AssetID package_name, final Map<PSDLayer, AssetID> raster_names) {
		this.package_name = package_name;
		this.raster_names = raster_names;
	}

	@Override
	public AssetID getPSDLayerName (final PSDLayer input) {
		return this.raster_names.get(input);
	}

	@Override
	public AssetID childAnimation (final String child_id) {
		return this.package_name.child(child_id);
	}

	@Override
	public AssetID childEvent (final String child_id) {
		return this.package_name.child(child_id);
	}

	@Override
	public AssetID childInput (final String child_id) {
		return this.package_name.child(child_id);
	}

	@Override
	public AssetID childText (final String child_id) {
		return this.package_name.child("text").child(child_id);
	}

}
