
package com.jfixby.tool.psd2scene2d;

import java.awt.image.BufferedImage;
import java.io.IOException;

import com.jfixby.cmns.api.assets.AssetID;
import com.jfixby.cmns.api.assets.Names;
import com.jfixby.cmns.api.collections.Collection;
import com.jfixby.cmns.api.collections.Collections;
import com.jfixby.cmns.api.collections.List;
import com.jfixby.cmns.api.collections.Map;
import com.jfixby.cmns.api.collections.Set;
import com.jfixby.cmns.api.debug.Debug;
import com.jfixby.cmns.api.desktop.ImageAWT;
import com.jfixby.cmns.api.file.File;
import com.jfixby.cmns.api.file.FileSystem;
import com.jfixby.cmns.api.file.LocalFileSystem;
import com.jfixby.cmns.api.file.cache.FileCache;
import com.jfixby.cmns.api.file.cache.TempFolder;
import com.jfixby.cmns.api.io.IO;
import com.jfixby.cmns.api.java.ByteArray;
import com.jfixby.cmns.api.log.L;
import com.jfixby.cmns.api.math.FloatMath;
import com.jfixby.cmns.api.math.IntegerMath;
import com.jfixby.psd.unpacker.api.PSDFileContent;
import com.jfixby.psd.unpacker.api.PSDLayer;
import com.jfixby.psd.unpacker.api.PSDRaster;
import com.jfixby.psd.unpacker.api.PSDRasterDimentions;
import com.jfixby.psd.unpacker.api.PSDRasterPosition;
import com.jfixby.psd.unpacker.api.PSDRootLayer;
import com.jfixby.psd.unpacker.api.PSDUnpacker;
import com.jfixby.psd.unpacker.api.PSDUnpackingParameters;
import com.jfixby.r3.api.resources.StandardPackageFormats;
import com.jfixby.r3.ext.api.scene2d.srlz.Scene2DPackage;
import com.jfixby.rana.api.pkg.fs.PackageDescriptor;
import com.jfixby.red.engine.core.resources.PackageUtils;
import com.jfixby.texture.slicer.api.SlicesCompositionInfo;
import com.jfixby.texture.slicer.api.SlicesCompositionsContainer;
import com.jfixby.texture.slicer.api.TextureSlicer;
import com.jfixby.texture.slicer.api.TextureSlicerSpecs;
import com.jfixby.texture.slicer.api.TextureSlicingResult;
import com.jfixby.tools.bleed.api.TextureBleed;
import com.jfixby.tools.bleed.api.TextureBleedResult;
import com.jfixby.tools.bleed.api.TextureBleedSpecs;
import com.jfixby.tools.gdx.texturepacker.api.AtlasPackingResult;
import com.jfixby.tools.gdx.texturepacker.api.Packer;
import com.jfixby.tools.gdx.texturepacker.api.TexturePacker;
import com.jfixby.tools.gdx.texturepacker.api.TexturePackingSpecs;

public class PSDRepacker {

	public static PSDRepackerResult repackPSD (final PSDRepackSettings settings, final PSDRepackingStatus handler)
		throws IOException {
		final PSDRepackerResult result = new PSDRepackerResult();
		final File psd_file = settings.getPSDFile();
		final AssetID package_name = settings.getPackageName();
		final File repacking_output = settings.getOutputFolder();
		final int max_texture_size = settings.getMaxTextureSize();
		final int margin = settings.getMargin();
		final float imageQuality = settings.getImageQuality();
// int padding = margin;
		final int max_page_size = settings.getAtlasMaxPageSize();
		final List<File> related_folders = Collections.newList();
		handler.setRelatedFolders(related_folders);
		final boolean ignore_atlas = settings.getIgnoreAtlasFlag();
		final int gemserk = settings.getGemserkPadding();
		final int padding = settings.getPadding();
		final boolean forceRasterDecomposition = settings.forceRasterDecomposition();

		final FileSystem FS = psd_file.getFileSystem();

		// File tmp_mount = repacking_output.parent().child("temp");
// final File tmp_mount = repacking_output;
// tmp_mount.makeFolder();
		final TempFolder temp_folder_handler = FileCache.createTempFolder(LocalFileSystem.ApplicationHome().child("tmp"));
		final File temp_folder = temp_folder_handler.getRoot();
		temp_folder.makeFolder();
		related_folders.add(temp_folder);
		// L.d("temp_folder", temp_folder);

		final File raster_folder = temp_folder.child("psd-raster");

		// String package_name = package_root_file.getName();

		File atlas_output;
		File scene2d_output;

		scene2d_output = repacking_output.child(package_name.child(Scene2DPackage.SCENE2D_PACKAGE_FILE_EXTENSION).toString());
		scene2d_output.makeFolder();
		related_folders.add(scene2d_output);
		scene2d_output = scene2d_output.child(PackageDescriptor.PACKAGE_CONTENT_FOLDER);
		scene2d_output.makeFolder();
		scene2d_output.clearFolder();

		final Map<PSDLayer, AssetID> raster_names = Collections.newMap();

		final PSDFileContent layers_structure = extractLayerStructures(psd_file, raster_names, package_name);

		L.d("---[Packing Layers Structure]--------------------------------------------");
		final ConversionResult pack_result = packLayers(layers_structure, package_name, scene2d_output, raster_names);

		final Collection<AssetID> used_raster = pack_result.listAllRequredAssets();

		L.d("---[Saving Raster]--------------------------------------------");
		boolean raster_produced = false;
		final boolean save_raster = true;
		final Map<PSDLayer, File> layer_to_file_mapping = Collections.newMap();
		raster_produced = saveRaster(psd_file, raster_names, used_raster, raster_folder, package_name, save_raster,
			layer_to_file_mapping, pack_result);

		if (!ignore_atlas && raster_produced) {
			L.d("---[Decomposing Raster]--------------------------------------------");
			final File tiling_folder = temp_folder.child("tiling");
			tiling_folder.makeFolder();
			final Collection<TextureSlicingResult> structures = decomposeRaster(layer_to_file_mapping, tiling_folder,
				max_texture_size, margin, forceRasterDecomposition, imageQuality);
			raster_folder.delete();

			final SlicesCompositionsContainer container = new SlicesCompositionsContainer();
			final List<AssetID> packed_structures = Collections.newList();
			final Set<AssetID> requred_rasters = Collections.newSet();

			for (final TextureSlicingResult combo : structures) {
				final SlicesCompositionInfo composition = combo.getTilesComposition();
				container.content.addElement(composition);
				packed_structures.add(Names.newAssetID(composition.composition_asset_id_string));
				requred_rasters.addAll(combo.listProducedTiles());
			}

			if (container.content.size() > 0) {
				final AssetID sctruct_package_name = package_name.child("psd").child(TextureSlicerSpecs.TILE_MAP_FILE_EXTENSION);

				final String struct_pkg_name = sctruct_package_name.toString();
				File container_file = repacking_output.child(struct_pkg_name);
				related_folders.add(container_file);
				container_file = container_file.child(PackageDescriptor.PACKAGE_CONTENT_FOLDER);
				container_file.makeFolder();

				container_file = container_file.child(sctruct_package_name.toString());

				final ByteArray data = IO.serialize(container);
				container_file.writeBytes(data);

				// used_raster.print("used_raster");
				// packed_structures.print("packed_structures");
				// Sys.exit();
				PackageUtils.producePackageDescriptor(container_file.parent().parent(),
					StandardPackageFormats.RedTriplane.TiledRaster, "1.0", packed_structures, requred_rasters,
					container_file.getName());

			}

			L.d("---[Packing Atlas]--------------------------------------------");
			final File atlas_folder = temp_folder.child("atlas");
			atlas_folder.makeFolder();
			final AtlasPackingResult atlas_result = packAtlas(atlas_folder, tiling_folder,
				package_name.child("psd").child("raster").toString(), max_page_size, gemserk, padding);

			atlas_result.print();

			File altas_file = atlas_result.getAtlasOutputFile();
			final String atlas_name = altas_file.getName();

			tiling_folder.delete();

			atlas_output = repacking_output.child(atlas_name).child(PackageDescriptor.PACKAGE_CONTENT_FOLDER);
			atlas_output.makeFolder();
			related_folders.add(atlas_output);
			atlas_output.clearFolder();
			FS.copyFolderContentsToFolder(atlas_folder, atlas_output);
			// Collection<AssetID> packed_rasters = atlas_result
			// .listPackedAssets();

			final Set<AssetID> packed_rasters = Collections.newSet();
			packed_rasters.addAll(requred_rasters);
			packed_rasters.addAll(atlas_result.listPackedAssets());

			final File atlasPackageFolder = atlas_output.parent();
			PackageUtils.producePackageDescriptor(atlasPackageFolder, StandardPackageFormats.libGDX.Atlas_GWT, "1.0", packed_rasters,
				Collections.newList(), atlas_name);

			// requred_rasters.print("requred_rasters");
			// packed_rasters.print("packed_rasters");
			// List<AssetID> diff = JUtils.newList();
			// diff.addAll(requred_rasters);
			// diff.removeAll(packed_rasters);
			// diff.print("diff");

			altas_file = atlas_output.child(atlas_name);

			atlas_folder.delete();
			result.addPackedAtlasPackageFolder(atlasPackageFolder);

		} else {
			L.d("   ignore_atlas", ignore_atlas);
			L.d("raster_produced", raster_produced);

		}

		temp_folder.delete();
		// L.d("atlas is ready", altas_file);
		return result;

	}

	static private PSDFileContent extractLayerStructures (final File psd_file, final Map<PSDLayer, AssetID> raster_names,
		final AssetID package_name) throws IOException {
		int k = 0;

		final PSDUnpackingParameters specs = PSDUnpacker.newUnpackingSpecs();
		specs.setPSDFile(psd_file);
		final PSDFileContent result = PSDUnpacker.unpack(specs);
		result.print();

		final Collection<PSDLayer> rasters = result.getRasterLayers();

		for (int i = 0; i < rasters.size(); i++) {
			final PSDLayer element = rasters.getElementAt(i);
			final AssetID raster_name = Names.newAssetID(package_name + ".psd.raster_" + k);
			raster_names.put(element, raster_name);
			k++;
		}
		return result;
	}

	static private ConversionResult packLayers (final PSDFileContent layers_structure, final AssetID package_name,
		final File final_output, final Map<PSDLayer, AssetID> raster_names) throws IOException {

		final AssetID package_prefix = package_name;

		final Scene2DPackage container = new Scene2DPackage();
		final PSDRootLayer root = layers_structure.getRootlayer();
		final ConversionResult result = PSDtoScene2DConverter.convert(container, package_prefix, root, raster_names);

		// AssetID asset_id = Names.newAssetID(package_prefix);

		// SceneStructure structure = container.structures.get(0);
		// structure.structure_name = package_prefix;
		final String root_file_name = package_prefix.child(Scene2DPackage.SCENE2D_PACKAGE_FILE_EXTENSION).toString();
		final ByteArray data = IO.serialize(container);
		final File file = final_output.child(root_file_name);
		file.writeBytes(data);

		final File descriptor = file.parent().parent();

		final List<AssetID> provisions = Collections.newList();

		for (int i = 0; i < container.structures.size(); i++) {
			final AssetID element_id = Names.newAssetID(container.structures.get(i).structure_name);
			provisions.add(element_id);
		}

		final Collection<AssetID> requred_assets = result.listAllRequredAssets();
		PackageUtils.producePackageDescriptor(descriptor, Scene2DPackage.SCENE2D_PACKAGE_FORMAT, "1.0", provisions, requred_assets,
			root_file_name);

		return result;

	}

	static private AtlasPackingResult packAtlas (final File atlas_folder, final File sprites, final String atlas_file_name,
		final int max_page_size, final int gemserk, final int padding) throws IOException {

		final TexturePackingSpecs specs = TexturePacker.newPackingSpecs();

		specs.setOutputAtlasFileName(atlas_file_name);
		specs.setOutputAtlasFolder(atlas_folder);
		specs.setInputRasterFolder(sprites);
		specs.setDebugMode(!true);
		specs.setMaxPageSize(max_page_size);
		specs.setPadding(padding);

		final Packer packer = TexturePacker.newPacker(specs);

		final AtlasPackingResult result = packer.pack();

		if (gemserk > 0) {
			final TextureBleedSpecs bleedSpecs = TextureBleed.newSpecs();
			bleedSpecs.setDebugMode(!true);
			bleedSpecs.setPaddingSize(gemserk);
			final File pagesFolder = result.getAtlasOutputFile().parent();
			L.d("Gemserking", pagesFolder);
			bleedSpecs.setInputFolder(pagesFolder);
			final TextureBleedResult gemserk_result = TextureBleed.process(bleedSpecs);
			gemserk_result.print();
		}

		return result;
	}

	static private List<TextureSlicingResult> decomposeRaster (final Map<PSDLayer, File> layer_to_file_mapping,
		final File tiling_folder, final int max_texture_size, final int margin, final boolean forceRasterDecomposition,
		final float imageQuality) throws IOException {
		final List<TextureSlicingResult> results = Collections.newList();
		for (int i = 0; i < layer_to_file_mapping.size(); i++) {
			final PSDLayer layer_info = layer_to_file_mapping.getKeyAt(i);
			final PSDRasterPosition position = layer_info.getRaster().getPosition();
			final PSDRasterDimentions dim = layer_info.getRaster().getDimentions();
			final double width = dim.getWidth();
			final double height = dim.getHeight();
			final double diag = FloatMath.max(width, height);
			final File png_file = layer_to_file_mapping.get(layer_info);
			if (forceRasterDecomposition || diag > max_texture_size) {
				// decompose
				final TextureSlicingResult result = decomposeSprite(png_file, tiling_folder, margin, max_texture_size, imageQuality);
				results.add(result);
			} else {
				// copy as is
				final File file_to_copy = png_file;
				copyFile(file_to_copy, tiling_folder, imageQuality);
			}
		}
		return results;
	}

	private static void copyFile (final File file_to_copy, final File tiling_folder, final float imageQuality) throws IOException {
		if (imageQuality == 1) {
			tiling_folder.getFileSystem().copyFileToFolder(file_to_copy, tiling_folder);
		} else {
			final BufferedImage image = ImageAWT.readFromFile(file_to_copy);
			final File restoredFile = tiling_folder.child(file_to_copy.getName());
			ImageAWT.writeToFile(ImageAWT.toBufferedImage(ImageAWT.awtScale(image, imageQuality)), restoredFile, "PNG");
		}
	}

	static private TextureSlicingResult decomposeSprite (final File png_file_path, final File tiling_folder, final int margin,
		final int max_texture_size, final float imageQuality) throws IOException {

		final File output_folder = tiling_folder;
		output_folder.makeFolder();

		final TextureSlicerSpecs specs = TextureSlicer.newDecompositionSpecs();
		specs.setInputFile(png_file_path);

		specs.setTileWidth(max_texture_size - 2 * margin);
		specs.setTileHeight(max_texture_size - 2 * margin);
		specs.setMargin(margin);

		final String asset_name = png_file_path.getName().substring(0, png_file_path.getName().length() - ".png".length());

		specs.setNameSpacePrefix(Names.newAssetID(asset_name));
		specs.setOutputFolder(tiling_folder);
		specs.setImageQuality(imageQuality);
		final TextureSlicingResult result = TextureSlicer.decompose(specs);
		return result;
	}

	static private boolean saveRaster (final File package_root_file, final Map<PSDLayer, AssetID> raster_names,
		final Collection<AssetID> used_raster, final File output_folder, final AssetID package_name, final boolean save_raster,
		final Map<PSDLayer, File> layer_to_file_mapping, final ConversionResult pack_result) throws IOException {
		boolean raster_produced = false;
		for (int i = 0; i < raster_names.size(); i++) {

			final PSDLayer layer = raster_names.getKeyAt(i);
			final AssetID raster_name = raster_names.getValueAt(i);
			if (!used_raster.contains(raster_name)) {
				continue;
			}

			if (!raster_produced) {
				output_folder.makeFolder();
				output_folder.clearFolder();
			}
			raster_produced = true;

			final String png_file_name = raster_name + ".png";

			saveRaster(png_file_name, layer, output_folder, save_raster, layer_to_file_mapping, pack_result);

		}
		return raster_produced;
	}

	static private void saveRaster (final String png_file_name, final PSDLayer layer, final File output_folder,
		final boolean save_raster, final Map<PSDLayer, File> layer_to_file_mapping, final ConversionResult pack_result)
		throws IOException {

		final File output_file = output_folder.child(png_file_name);

		layer_to_file_mapping.put(layer, output_file);

		final PSDRaster raster = layer.getRaster();
		final BufferedImage java_image = raster.getBufferedImage();

		Debug.checkNull("java_image", java_image);

		final SceneStructurePackingResult result = pack_result.getStrucutreResultByLayer(layer);

		final float scale_factor = result.getScaleFactor();

		BufferedImage out;
		if (scale_factor != 1) {
// final Image tmp = java_image.getScaledInstance((int)(java_image.getWidth() * scale_factor),
// (int)(java_image.getHeight() * scale_factor), BufferedImage.SCALE_SMOOTH);
// final Image tmp = ;
			out = ImageAWT.toBufferedImage(ImageAWT.awtScale(java_image, scale_factor));
		} else {
			out = java_image;
		}

		if (save_raster) {
			L.d("writing: " + output_file);
			ImageAWT.writeToFile(out, output_file, "png");
		}

	}

	public static PSDRepackSettings newSettings () {
		return new PSDRepackSettings();
	}

	public static int regressiveInt (final int target_value, final int threshold) {
		final double power_of_2 = FloatMath.log(2, target_value);
		if (!FloatMath.isInteger(power_of_2)) {
			throw new Error("Is not power of two: 2^" + power_of_2 + "=" + target_value);
		}
		int result = 1;
		int add = target_value;
		for (int i = (int)FloatMath.round(power_of_2 - 1); add > threshold; i--) {
			add = (int)IntegerMath.power(2, i);

			result = result + add;
		}

		return result;

	}
}
