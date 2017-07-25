
package com.jfixby.tool.psd2scene2d;

import com.jfixby.r3.io.scene2d.LayerElement;
import com.jfixby.r3.io.scene2d.LayerElementFactory;
import com.jfixby.r3.io.scene2d.SceneStructure;
import com.jfixby.scarabei.api.collections.Collections;
import com.jfixby.scarabei.api.collections.List;
import com.jfixby.scarabei.api.floatn.Float2;
import com.jfixby.scarabei.api.geometry.Geometry;

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

	final List<Float2> offsetStack = Collections.newList();
	final Float2 currentOffset = Geometry.newFloat2();

	public Float2 getCurrentOffset () {
		return this.currentOffset;
	}

	public void addOffset (final Float2 layerOffset) {
		this.offsetStack.add(layerOffset);
		this.currentOffset.add(layerOffset);
	}

	public Float2 removeOffset () {
		final Float2 last = this.offsetStack.removeLast();
		this.currentOffset.subtract(last);
		return last;
	}

}
