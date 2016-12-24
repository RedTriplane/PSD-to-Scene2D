
package com.jfixby.tool.psd2scene2d;

import com.jfixby.psd.unpacker.api.PSDLayer;
import com.jfixby.r3.ext.api.scene2d.srlz.SceneStructure;
import com.jfixby.scarabei.api.assets.ID;
import com.jfixby.scarabei.api.collections.Collection;
import com.jfixby.scarabei.api.collections.Collections;
import com.jfixby.scarabei.api.collections.List;
import com.jfixby.scarabei.api.collections.Set;
import com.jfixby.scarabei.api.debug.Debug;

public class SceneStructurePackingResult {

	List<ID> lit = Collections.newList();
	private float scale_factor;
	private final SceneStructure structure;
	private final Set<PSDLayer> ancestors = Collections.newSet();
	private float imageQuality;

	public SceneStructurePackingResult (final SceneStructure structure) {
		this.structure = structure;
	}

	public void addRequiredAsset (final ID child_scene_asset_id, final List<PSDLayer> list) {
		Debug.checkNull(child_scene_asset_id);
		this.lit.add(child_scene_asset_id);
		this.ancestors.addAll(list);
	}

	public List<ID> listRequiredAssets () {
		return this.lit;
	}

	public void setScaleFactor (final float scale_factor) {
		this.scale_factor = scale_factor;
	}

	public float getImageQuality () {
		return this.imageQuality;
	}

	public float getScaleFactor () {
		return this.scale_factor;
	}

	public Collection<PSDLayer> getAncestors () {
		return this.ancestors;
	}

	public SceneStructure getStructure () {
		return this.structure;
	}

	public void setImageQuality (final float imageQuality) {
		this.imageQuality = imageQuality;
	}

}
