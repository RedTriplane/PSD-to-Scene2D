
package com.jfixby.tool.psd2scene2d;

import com.jfixby.cmns.api.assets.AssetID;
import com.jfixby.cmns.api.file.File;

public class PSDRepackSettings {

	private File psd_file;
	private AssetID package_name;
	private File output_folder;
	private int max_texture_size;
	private int margin;
	private boolean ignore_atlas;
	private int altas_max_page_size = 1024;
	private int gemserkPadding = -1;
	private int texturePadding;
	private boolean forceRasterDecomposition;

	public void setPSDFile (final File psd_file) {
		this.psd_file = psd_file;
	}

	public void setPackageName (final AssetID package_name) {
		this.package_name = package_name;
	}

	public void setOutputFolder (final File output_folder) {
		this.output_folder = output_folder;
	}

	public void setMaxTextureSize (final int max_texture_size) {
		this.max_texture_size = max_texture_size;
	}

	public void setMargin (final int margin) {
		this.margin = margin;
	}

	public void setIgonreAtlasFlag (final boolean ignore_atlas) {
		this.ignore_atlas = ignore_atlas;
	}

	public AssetID getPackageName () {
		return this.package_name;
	}

	public File getPSDFile () {
		return this.psd_file;
	}

	public boolean getIgnoreAtlasFlag () {
		return this.ignore_atlas;
	}

	public File getOutputFolder () {
		return this.output_folder;
	}

	public int getMaxTextureSize () {
		return this.max_texture_size;
	}

	public int getMargin () {
		return this.margin;
	}

	public void setAtlasMaxPageSize (final int altas_max_page_size) {
		this.altas_max_page_size = altas_max_page_size;
	}

	public int getAtlasMaxPageSize () {
		return this.altas_max_page_size;
	}

	public int getGemserkPadding () {
		return this.gemserkPadding;
	}

	public void setGemserkPadding (final int gemserkPadding) {
		this.gemserkPadding = gemserkPadding;
	}

	public void setPadding (final int texturePadding) {
		this.texturePadding = texturePadding;
	}

	public int getPadding () {
		return this.texturePadding;
	}

	public void setForceRasterDecomposition (final boolean forceRasterDecomposition) {
		this.forceRasterDecomposition = forceRasterDecomposition;
	}

	public boolean forceRasterDecomposition () {
		return this.forceRasterDecomposition;
	}

}
