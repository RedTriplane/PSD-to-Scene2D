
package com.jfixby.tool.psd2scene2d;

import com.jfixby.r3.ext.api.scene2d.srlz.LayerElement;
import com.jfixby.r3.ext.api.scene2d.srlz.LayerElementFactory;
import com.jfixby.r3.ext.api.scene2d.srlz.SceneStructure;

public class ConvertionSettings {

	public ConvertionSettings (final SceneStructure structure) {
		this.factory = new LayerElementFactory(structure);
	}

	LayerElementFactory factory;
	private float scale_factor;
	private SceneStructurePackingResult result_i;
	private PsdRepackerNameResolver naming;

	public LayerElement newLayerElement () {
		final LayerElement element = this.factory.newLayerElement();
		return element;
	}

	public void setScaleFactor (final float scale_factor) {
		this.scale_factor = scale_factor;
	}

	public void setResult (final SceneStructurePackingResult result_i) {
		this.result_i = result_i;
	}

	public void setNaming (final PsdRepackerNameResolver naming) {
		this.naming = naming;
	}

	public double getScaleFactor () {
		return this.scale_factor;
	}

	public SceneStructurePackingResult getResult () {
		return this.result_i;
	}

	public PsdRepackerNameResolver getNaming () {
		return this.naming;
	}

	public SceneStructure getStructure () {
		return this.factory.getStructure();
	}

}
