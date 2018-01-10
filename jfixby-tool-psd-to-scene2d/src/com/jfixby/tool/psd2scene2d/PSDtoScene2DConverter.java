
package com.jfixby.tool.psd2scene2d;

import java.util.ArrayList;

import com.jfixby.psd.unpacker.api.PSDLayer;
import com.jfixby.psd.unpacker.api.PSDRaster;
import com.jfixby.psd.unpacker.api.PSDRasterDimentions;
import com.jfixby.psd.unpacker.api.PSDRasterPosition;
import com.jfixby.r3.fokker.assets.api.shader.io.SHADER_PARAMETER;
import com.jfixby.r3.io.scene2d.Action;
import com.jfixby.r3.io.scene2d.ActionsGroup;
import com.jfixby.r3.io.scene2d.Anchor;
import com.jfixby.r3.io.scene2d.AnimationSettings;
import com.jfixby.r3.io.scene2d.CameraSettings;
import com.jfixby.r3.io.scene2d.CameraSettings.MODE;
import com.jfixby.r3.io.scene2d.ChildSceneSettings;
import com.jfixby.r3.io.scene2d.InputSettings;
import com.jfixby.r3.io.scene2d.LayerElement;
import com.jfixby.r3.io.scene2d.NinePatchSettings;
import com.jfixby.r3.io.scene2d.ParallaxSettings;
import com.jfixby.r3.io.scene2d.ProgressSettings;
import com.jfixby.r3.io.scene2d.RASTER_BLEND_MODE;
import com.jfixby.r3.io.scene2d.Scene2DPackage;
import com.jfixby.r3.io.scene2d.SceneStructure;
import com.jfixby.r3.io.scene2d.ShaderParameterType;
import com.jfixby.r3.io.scene2d.ShaderParameterValue;
import com.jfixby.r3.io.scene2d.ShaderSettings;
import com.jfixby.r3.io.scene2d.TextSettings;
import com.jfixby.scarabei.api.collections.Collections;
import com.jfixby.scarabei.api.collections.List;
import com.jfixby.scarabei.api.collections.Map;
import com.jfixby.scarabei.api.color.Colors;
import com.jfixby.scarabei.api.debug.Debug;
import com.jfixby.scarabei.api.err.Err;
import com.jfixby.scarabei.api.floatn.Float2;
import com.jfixby.scarabei.api.geometry.Geometry;
import com.jfixby.scarabei.api.log.L;
import com.jfixby.scarabei.api.log.Logger;
import com.jfixby.scarabei.api.math.FloatMath;
import com.jfixby.scarabei.api.names.ID;
import com.jfixby.scarabei.api.names.Names;

public class PSDtoScene2DConverter {

	private static void convert (final LayersStack stack, final PSDLayer input, final LayerElement output,
		final ConvertionSettings settings) {

		if (input.isFolder()) {
			stack.push(input);
			final String name = input.getName();
			L.d("convert layer", name);

			final PSDLayer animation_node = input.findChildByNamePrefix(TAGS.ANIMATION);
			final PSDLayer childscene_node = input.findChildByNamePrefix(TAGS.CHILD_SCENE);
			final PSDLayer text_node = input.findChildByNamePrefix(TAGS.R3_TEXT);
			final PSDLayer shader_node = input.findChildByNamePrefix(TAGS.R3_SHADER);
			final PSDLayer user_input = input.findChildByNamePrefix(TAGS.INPUT);
			final PSDLayer progress = input.findChildByNamePrefix(TAGS.PROGRESS);
			final PSDLayer parallax = input.findChildByNamePrefix(TAGS.PARALLAX);
			final PSDLayer ninePatch = input.findChildByNamePrefix(TAGS.NINE_PATCH);
			// PSDLayer events_node = input.findChild(EVENT);
			if (animation_node != null) {
				if (input.numberOfChildren() != 1) {
					Err.reportError("Annotation problem (only one child allowed). This is not an animation node: " + input);
				}
				PSDtoScene2DConverter.convertAnimation(stack, input, output, settings);
			} else if (childscene_node != null) {
				if (input.numberOfChildren() != 1) {
					Err.reportError("Annotation problem (only one child allowed). This is not an child scene node: " + input);
				}
				PSDtoScene2DConverter.convertChildScene(stack, input, output, settings);
			} else if (shader_node != null) {
				if (input.numberOfChildren() != 1) {
					Err.reportError("Annotation problem (only one child allowed). This is not an	 child scene node: " + input);
				}
				PSDtoScene2DConverter.convertShader(input, output, settings);
			} else if (ninePatch != null) {
				if (input.numberOfChildren() != 1) {
					Err.reportError("Annotation problem (only one child allowed). This is not an	 child scene node: " + input);
				}
				PSDtoScene2DConverter.convert9Patch(stack, input, output, settings);
			} else if (text_node != null) {
				if (input.numberOfChildren() != 1) {
					Err.reportError("Annotation problem (only one child allowed). This is not an child scene node: " + input);
				}
				PSDtoScene2DConverter.convertText(stack, input, output, settings);
			} else if (user_input != null) {
				if (input.numberOfChildren() != 1) {
					Err.reportError("Annotation problem (only one child allowed). This is not an child scene node: " + input);
				}
				PSDtoScene2DConverter.convertInput(stack, input, output, settings);
			} else if (progress != null) {
				if (input.numberOfChildren() != 1) {
					Err.reportError("Annotation problem (only one child allowed). This is not an child scene node: " + input);
				}
				PSDtoScene2DConverter.convertProgress(stack, input, output, settings);
			} else if (parallax != null) {
				if (input.numberOfChildren() != 1) {
					Err.reportError("Annotation problem (only one child allowed). This is not an child scene node: " + input);
				}
				PSDtoScene2DConverter.convertParallax(stack, input, output, settings);
			} else if (false) {
				if (input.numberOfChildren() != 1) {
					Err.reportError("Annotation problem (only one child allowed). This is not an child scene node: " + input);
				}
				// convertEventsSequence(input, output, naming, result,
				// scale_factor);
			} else {

				if (name.startsWith("@") && !name.startsWith(TAGS.CONTENT)) {
					L.d(stack);
					Err.reportError("Bad layer name: " + name);
				}

				PSDtoScene2DConverter.convertFolder(stack, input, output, settings);
			}
			stack.pop(input);
		} else if (input.isRaster()) {
			PSDtoScene2DConverter.convertRaster(input, output, settings);
		}

	}

	public static ConversionResult convert (final Scene2DPackage container, final ID package_prefix, final PSDLayer root,
		final Map<PSDLayer, ID> raster_names) {
		final ConversionResult results = new ConversionResult();

		// naming.print("naming");

		final LayersStack stack = new LayersStack();
		for (int i = 0; i < root.numberOfChildren(); i++) {
			final PSDLayer candidate = root.getChild(i);
			final String candidate_name = candidate.getName();
			if (candidate_name.equalsIgnoreCase(TAGS.R3_SCENE)) {
				final PSDLayer content_layer = candidate.findChildByNamePrefix(TAGS.CONTENT);
				if (content_layer == null) {
					continue;
				}
				final PSDLayer name_layer = candidate.findChildByNamePrefix(TAGS.STRUCTURE_NAME);
				if (name_layer == null) {
					Err.reportError("missing NAME tag");
					continue;
				}

				final PSDLayer camera_layer = candidate.findChildByNamePrefix(TAGS.CAMERA);

				float scale_factor = 1f;
				float imageQuality = 1f;
				{
					final PSDLayer divisor = candidate.findChildByNamePrefix(TAGS.SCALE_DIVISOR);
					if (divisor != null) {
						final String divisor_string = PSDtoScene2DConverter.readParameter(divisor, TAGS.SCALE_DIVISOR);
						scale_factor = 1f / Float.parseFloat(divisor_string);
					}
				}
				{
					final PSDLayer quality = candidate.findChildByNamePrefix(TAGS.IMAGE_QUALITY);
					if (quality != null) {
						final String quality_string = PSDtoScene2DConverter.readParameter(quality, TAGS.IMAGE_QUALITY);
						imageQuality = Float.parseFloat(quality_string);
					}
				}
				final SceneStructure structure = new SceneStructure();
				final ConvertionSettings settings = new ConvertionSettings(structure);
				structure.root = settings.newLayerElement();
				settings.setScaleFactor(scale_factor);

				final SceneStructurePackingResult result_i = new SceneStructurePackingResult(structure);

				settings.setResult(result_i);
				result_i.setScaleFactor(scale_factor);
				result_i.setImageQuality(imageQuality);

				container.structures.add(structure);
				structure.structure_name = PSDtoScene2DConverter.readParameter(name_layer.getName(), TAGS.STRUCTURE_NAME);
				structure.structure_name = package_prefix.child(structure.structure_name).toString();
				final LayerElement element = structure.root;

				final PsdRepackerNameResolver naming = new PsdRepackerNameResolver(Names.newID(structure.structure_name),
					raster_names);
				settings.setNaming(naming);
				PSDtoScene2DConverter.convert(stack, content_layer, element, settings);

				element.name = structure.structure_name;

				PSDtoScene2DConverter.setupCamera(stack, camera_layer, element, scale_factor);
				if (element.camera_settings != null) {
					structure.original_width = element.camera_settings.width;
					structure.original_height = element.camera_settings.height;
				}

				Logger.d("structure found", structure.structure_name);

				results.putResult(structure, result_i);
			}
		}

		return results;
	}

	private static void convertAnimation (final LayersStack stack, final PSDLayer input_parent, final LayerElement output,

		final ConvertionSettings settings) {

		final SceneStructure structure = settings.getStructure();

		final String name = input_parent.getName();
		output.is_hidden = !input_parent.isVisible();
		output.is_animation = true;
		output.name = name;

		final PSDLayer input = input_parent.findChildByNamePrefix(TAGS.ANIMATION);

		final AnimationSettings animation_settings = new AnimationSettings();
		output.animation_settings = animation_settings;

		{
			final PSDLayer looped = PSDtoScene2DConverter.findChild(TAGS.IS_LOOPED, input);

			if (looped == null) {
				animation_settings.is_looped = true;
			} else {
				final String is_looped = PSDtoScene2DConverter.readParameter(looped, TAGS.IS_LOOPED);
				animation_settings.is_looped = Boolean.parseBoolean(is_looped);
			}
		}

		{
			final PSDLayer spline = PSDtoScene2DConverter.findChild(TAGS.USE_SPLINE, input);

			if (spline == null) {
				animation_settings.use_spline = true;
			} else {
				final String use_spline = PSDtoScene2DConverter.readParameter(spline, TAGS.USE_SPLINE);
				animation_settings.use_spline = Boolean.parseBoolean(use_spline);
			}
		}

		{
			final PSDLayer debug = PSDtoScene2DConverter.findChild(TAGS.DEBUG, input);

			if (debug == null) {
				output.debug_mode = false;
			} else {
				final String debug_mode = PSDtoScene2DConverter.readParameter(debug.getName(), TAGS.DEBUG);
				output.debug_mode = Boolean.parseBoolean(debug_mode);
			}
		}

		{
			final PSDLayer autostart = PSDtoScene2DConverter.findChild(TAGS.AUTOSTART, input);

			if (autostart == null) {
				animation_settings.autostart = false;
			} else {
				final String autostart_string = PSDtoScene2DConverter.readParameter(autostart.getName(), TAGS.AUTOSTART);
				animation_settings.autostart = Boolean.parseBoolean(autostart_string);
				Logger.e("", input);
				Debug.checkTrue("animation_settings.autostart", !animation_settings.autostart);
			}
		}

		{
			final PSDLayer id = PSDtoScene2DConverter.findChild(TAGS.ID, input);

			if (id == null) {
				Err.reportError("Animation ID tag not found: " + input);
			} else {
				output.animation_id = PSDtoScene2DConverter.readParameter(id.getName(), TAGS.ID);
				final PsdRepackerNameResolver naming = settings.getNaming();
				output.animation_id = naming.childAnimation(output.animation_id).toString();
			}
		}
		// PSDLayer type = findChild(ANIMATION_TYPE, input);
		{
			final PSDLayer type = PSDtoScene2DConverter.findChild(TAGS.TYPE, input);
			if (type == null) {
				animation_settings.is_positions_modifyer_animation = false;
				animation_settings.is_simple_animation = true;
			} else {
				final String type_value = PSDtoScene2DConverter.readParameter(type.getName(), TAGS.TYPE);
				animation_settings.is_positions_modifyer_animation = TAGS.ANIMATION_TYPE_POSITION_MODIFIER
					.equalsIgnoreCase(type_value);

			}

			if (!(animation_settings.is_positions_modifyer_animation || animation_settings.is_simple_animation)) {
				Err.reportError("Unknown animation type: " + type);
			}
		}

		if (animation_settings.is_simple_animation) {
			Long frame_time = null;
			final PSDLayer frame = PSDtoScene2DConverter.findChild(TAGS.FRAME_TIME, input);
			if (frame == null) {

			} else {
				final String type_value = PSDtoScene2DConverter.readParameter(frame.getName(), TAGS.FRAME_TIME);
				frame_time = (long)(Double.parseDouble(type_value) * 1000);
				if (frame_time > 10000) {
					Err.reportError("Frame time is too big: " + frame_time);
				}
			}

			{
				final PSDLayer frames = PSDtoScene2DConverter.findChild(TAGS.ANIMATION_FRAMES, input);
				if (frames == null) {
					Logger.d("Missing <frames> folder in node: " + input);
				}
				Debug.checkNull("frames", frames);
				for (int i = 0; i < frames.numberOfChildren(); i++) {
					final PSDLayer child = frames.getChild(i);
					final LayerElement element = settings.newLayerElement();
					element.animation_settings = new AnimationSettings();
					if (frame_time != null) {
						element.animation_settings.frame_time = frame_time + "";
					} else {
						element.animation_settings.frame_time = "" + (long)(Double.parseDouble(child.getName()) * 1000d);
// L.d("element.animation_settings.frame_time", element.animation_settings.frame_time);
					}
					output.children.addElement(element, structure);
					PSDtoScene2DConverter.convert(stack, child, element, settings);
				}
				if (frames.numberOfChildren() == 0) {
					Err.reportError("No frames found for " + output.animation_id);
				}
			}

			return;
		}

		if (animation_settings.is_positions_modifyer_animation) {
			final PSDLayer scene = PSDtoScene2DConverter.findChild(TAGS.ANIMATION_SCENE, input);
			final PSDLayer origin_layer = PSDtoScene2DConverter.findChild(TAGS.ORIGIN, scene);
			final Float2 origin = Geometry.newFloat2();
			final double scale_factor = settings.getScaleFactor();
			if (origin_layer != null) {
				final PSDRaster raster = origin_layer.getRaster();
				origin.setXY(raster.getPosition().getX() * scale_factor, raster.getPosition().getY() * scale_factor);
				origin.scaleXY(-1);
			}

			final PSDLayer anchors = PSDtoScene2DConverter.findChild(TAGS.ANIMATION_ANCHORS, input);
			Debug.checkNull("frames", anchors);
			animation_settings.anchors = new ArrayList<>();

			for (int i = 0; i < anchors.numberOfChildren(); i++) {
				final PSDLayer anchor_layer = anchors.getChild(i);
				final String anchor_time_string = anchor_layer.getName();
				final PSDRasterPosition position = anchor_layer.getRaster().getPosition();
				final Anchor anchor = new Anchor();

				anchor.time = "" + PSDtoScene2DConverter.getTime(anchor_time_string);
				anchor.position_x = position.getX() * scale_factor;
				anchor.position_y = position.getY() * scale_factor;
				animation_settings.anchors.add(anchor);
			}

			{
				// LayerElement element = new LayerElement();
				// output.children.addElement(element);
				// convert(scene, element, naming, result);
				if (origin_layer != null) {
					settings.addOffset(origin);
				}
				final LayerElement subLayer = settings.newLayerElement();
				subLayer.is_sublayer = true;
				subLayer.name = "frames_container";
				output.children.addElement(subLayer, structure);
				for (int i = 0; i < scene.numberOfChildren(); i++) {
					final PSDLayer child = scene.getChild(i);
					Debug.checkNull("child", child);
					if (child == origin_layer) {
						continue;
					}
					final LayerElement element = settings.newLayerElement();
					subLayer.children.addElement(element, structure);
					PSDtoScene2DConverter.convert(stack, child, element, settings);

					if (origin_layer == null) {
						element.position_x = 0;
						element.position_y = 0;
					}

				}
				if (origin_layer != null) {
					settings.removeOffset();
				}
			}

			return;
		}

	}

	private static void convertChildScene (final LayersStack stack, final PSDLayer input_parent, final LayerElement output,
		final ConvertionSettings settings) {

		final String name = input_parent.getName();
		output.is_hidden = !input_parent.isVisible();
		output.is_child_scene = true;
		output.name = name;

		output.child_scene_settings = new ChildSceneSettings();

		final PSDLayer input = input_parent.findChildByNamePrefix(TAGS.CHILD_SCENE);

		final PSDLayer frame = input.findChildByNamePrefix(TAGS.FRAME);
		{
			if (frame != null) {
				Err.reportError("Unsupported tag: " + TAGS.FRAME);
			}
		}

		final PSDLayer origin = input.findChildByNamePrefix(TAGS.ORIGIN);
		if (origin != null) {
// stack.print();
			Err.reportError("Tag deprecated: <" + TAGS.ORIGIN + ">");
// final double scale_factor = settings.getScaleFactor();
// output.child_scene_settings.frame_position_x = origin.getRaster().getPosition().getX() * scale_factor;
// output.child_scene_settings.frame_position_y = origin.getRaster().getPosition().getY() * scale_factor;
//
// output.child_scene_settings.frame_width = origin.getRaster().getDimentions().getWidth();
// output.child_scene_settings.frame_height = origin.getRaster().getDimentions().getHeight();
		}
		{
			final PSDLayer id = PSDtoScene2DConverter.findChild(TAGS.ID, input);

			if (id == null) {
				Err.reportError("Missing tag <@" + TAGS.ID + ">");
			} else {
				final String child_id = PSDtoScene2DConverter.readParameter(id, TAGS.ID);

				final ID child_scene_asset_id = Names.newID(child_id);

				output.child_scene_settings.child_scene_id = child_scene_asset_id.toString();

				// L.e("!!!!!!");
				final SceneStructurePackingResult result = settings.getResult();
				result.addRequiredAsset(child_scene_asset_id, Collections.newList(input_parent, input, origin));
			}
		}

	}

	private static void convert9Patch (final LayersStack stack, final PSDLayer input_parent, final LayerElement output,
		final ConvertionSettings settings) {

		final String name = input_parent.getName();
		output.is_hidden = !input_parent.isVisible();
		output.is_9_patch = true;
		output.name = name;

		final PSDLayer ninePatch = input_parent.findChildByNamePrefix(TAGS.NINE_PATCH);
		Debug.checkNull("ninePatch", ninePatch);

		final PSDLayer insideArea = ninePatch.findChildByName(TAGS.AREA);
		Debug.checkNull("insideArea", insideArea);

		output.nine_patch_settings = new NinePatchSettings();

		output.nine_patch_settings.TL.x = insideArea.getRaster().getPosition().getX();
		output.nine_patch_settings.TL.y = insideArea.getRaster().getPosition().getY();

		output.nine_patch_settings.BR.x = output.nine_patch_settings.TL.x + insideArea.getRaster().getPosition().getWidth();
		output.nine_patch_settings.BR.y = output.nine_patch_settings.TL.y + insideArea.getRaster().getPosition().getHeight();

		output.nine_patch_settings.TR.x = output.nine_patch_settings.BR.x;
		output.nine_patch_settings.TR.y = output.nine_patch_settings.TL.y;

		output.nine_patch_settings.BL.x = output.nine_patch_settings.TL.x;
		output.nine_patch_settings.BL.y = output.nine_patch_settings.BR.y;

// L.d();
	}

	private static void convertFolder (final LayersStack stack, final PSDLayer input, final LayerElement coutput,
		final ConvertionSettings settings) {

		{
			final LayerElement output = coutput;
			// output.shader_settings = shader_settings;
			output.is_hidden = !input.isVisible();
			output.name = input.getName();

			output.is_sublayer = true;

			for (int i = 0; i < input.numberOfChildren(); i++) {
				final PSDLayer child = input.getChild(i);
				final LayerElement element = settings.newLayerElement();
				final SceneStructure structure = settings.getStructure();
				output.children.addElement(element, structure);

				PSDtoScene2DConverter.convert(stack, child, element, settings);

				if (element.name.startsWith("@")) {
					L.d(stack);
					Err.reportError("Bad layer name: " + element.name);
				}

			}
		}
	}

	private static void convertInput (final LayersStack stack, final PSDLayer input_parent, final LayerElement output,
		final ConvertionSettings settings) {

		final String name = input_parent.getName();
		output.is_hidden = !input_parent.isVisible();
		output.is_user_input = true;
		output.name = name;

		final PSDLayer input = input_parent.findChildByNamePrefix(TAGS.INPUT);

		final InputSettings input_settings = new InputSettings();
		output.input_settings = input_settings;

		final PSDLayer id = PSDtoScene2DConverter.findChild(TAGS.ID, input);
		if (id != null) {
// stack.print();
// Err.reportError("Tag depricated: <" + TAGS.ID + ">");
		}

		{
			final PSDLayer debug = PSDtoScene2DConverter.findChild(TAGS.DEBUG, input);

			if (debug == null) {
				output.debug_mode = false;
			} else {
				final String debug_mode = PSDtoScene2DConverter.readParameter(debug, TAGS.DEBUG);
				output.debug_mode = Boolean.parseBoolean(debug_mode);
			}
		}

		final double scale_factor = settings.getScaleFactor();
		final PSDLayer origin_layer = PSDtoScene2DConverter.findChild(TAGS.ORIGIN, input);
		final Float2 origin = Geometry.newFloat2();
		if (origin_layer != null) {
			final PSDRaster raster = origin_layer.getChild(0).getRaster();
			origin.setXY(raster.getPosition().getX() * scale_factor, raster.getPosition().getY() * scale_factor);
		}

		output.position_x = origin.getX();
		output.position_y = origin.getY();
// PSDLayer type = findChild(ANIMATION_TYPE, input);
		{
			final PSDLayer type = PSDtoScene2DConverter.findChild(TAGS.TYPE, input);
			if (type == null) {

			} else {
				final String type_value = PSDtoScene2DConverter.readParameter(type.getName(), TAGS.TYPE);

				output.input_settings.is_button = TAGS.VALUE_BUTTON.equalsIgnoreCase(type_value);
				output.input_settings.is_switch = TAGS.VALUE_SWITCH.equalsIgnoreCase(type_value);
				output.input_settings.is_custom = TAGS.VALUE_CUSTOM.equalsIgnoreCase(type_value);
				output.input_settings.is_touch_area = TAGS.VALUE_TOUCH.equalsIgnoreCase(type_value);

				// animation_settings.is_positions_modifyer_animation =
				// ANIMATION_TYPE_POSITION_MODIFIER
				// c;

			}

			final PSDLayer raster = PSDtoScene2DConverter.findChild(TAGS.RASTER, input);
			if (output.input_settings.is_button) {
				PSDtoScene2DConverter.extractButtonRaster(stack, raster, output, settings, origin);
			} else if (output.input_settings.is_switch) {
				PSDtoScene2DConverter.extractButtonOptions(stack, raster, output, settings, origin);
			} else if (output.input_settings.is_custom) {
				PSDtoScene2DConverter.extractButtonOptions(stack, raster, output, settings, origin);
			} else if (output.input_settings.is_touch_area) {
				Logger.d(output.input_settings);
// final PSDLayer area = findChild(TAGS.AREA, input);
// final PSDLayer dimentions = area.getChild(0);
// extractTouchArea(stack, dimentions, output, settings, origin);
			} else {
// stack.print();
				Err.reportError("Unknown input type: " + type);
			}

		}

		{
			final PSDLayer touch_area = PSDtoScene2DConverter.findChild(TAGS.AREA, input);
			// output.input_settings.areas = new Vector<TouchArea>();
			if (touch_area != null) {
				final LayerElement touch_areas = settings.newLayerElement();
				;
				output.input_settings.touch_area = touch_areas;

				for (int i = 0; i < touch_area.numberOfChildren(); i++) {
					final PSDLayer child = touch_area.getChild(i);
					if (child.isFolder()) {
						Err.reportError("Touch area has no dimentions: " + child);
					} else {
						final PSDRaster raster = child.getRaster();
						Debug.checkNull("raster", raster);

						final LayerElement area = settings.newLayerElement();
						;
						final SceneStructure structure = settings.getStructure();
						touch_areas.children.addElement(area, structure);
						area.position_x = (raster.getPosition().getX() * scale_factor) - origin.getX();
						area.position_y = (raster.getPosition().getY() * scale_factor) - origin.getY();
						area.width = raster.getDimentions().getWidth() * scale_factor;
						area.height = raster.getDimentions().getHeight() * scale_factor;
						area.name = child.getName();

						// TouchArea area = new TouchArea();
						// area.position_x = raster.getPosition().getX();
						// area.position_y = raster.getPosition().getY();
						// area.width = raster.getDimentions().getWidth();
						// area.height = raster.getDimentions().getHeight();
						//
						// output.input_settings.areas.add(area);
					}

				}
			}

		}

	}

	private static void convertParallax (final LayersStack stack, final PSDLayer input_parent, final LayerElement output,
		final ConvertionSettings settings) {

		final String name = input_parent.getName();
		output.is_hidden = !input_parent.isVisible();
		output.name = name;

		output.is_parallax = true;

		final PSDLayer content = input_parent.findChildByNamePrefix(TAGS.PARALLAX);

		final double scale_factor = settings.getScaleFactor();
		final PSDLayer origin_layer = PSDtoScene2DConverter.findChild(TAGS.ORIGIN, content);
		final Float2 origin = Geometry.newFloat2();
		if (origin_layer != null) {
			final PSDRaster raster = origin_layer.getRaster();
			origin.setXY(raster.getPosition().getX() * scale_factor, raster.getPosition().getY() * scale_factor);
		}
		final PSDLayer frame_layer = PSDtoScene2DConverter.findChild(TAGS.FRAME, content);

		output.position_x = origin.getX();
		output.position_y = origin.getY();

		final Float2 parallaxOffset = Geometry.newFloat2();
		parallaxOffset.subtract(origin);
		settings.addOffset(parallaxOffset);

		if (frame_layer != null) {
			final PSDRaster frame = frame_layer.getRaster();
			final PSDRasterPosition position = frame.getPosition();
			final PSDRasterDimentions dim = frame.getDimentions();
			output.origin_relative_x = -(position.getX() - origin.getX()) / dim.getWidth();
			output.origin_relative_y = -(position.getY() - origin.getY()) / dim.getHeight();

			output.width = dim.getWidth() * scale_factor;
			output.height = dim.getHeight() * scale_factor;

		}

		final SceneStructure structure = settings.getStructure();
		for (int i = 0; i < content.numberOfChildren(); i++) {
			final PSDLayer child = content.getChild(i);
			if (child == origin_layer) {
				continue;
			}
			if (child == frame_layer) {
				continue;
			}
			final LayerElement layer = settings.newLayerElement();
			PSDtoScene2DConverter.convertParallaxLayer(stack, child, layer, settings, frame_layer);
			output.children.addElement(layer, structure);
		}
		settings.removeOffset();
	}

	private static void convertParallaxLayer (final LayersStack stack, final PSDLayer layer, final LayerElement output,
		final ConvertionSettings settings, final PSDLayer frame_layer) {
		output.parallax_settings = new ParallaxSettings();
		output.name = layer.getName();
		output.is_sublayer = true;

		final PSDLayer settingsLayer = layer.findChildByName(TAGS.PARALLAX_SETTINGS);
		final Float2 layerOffset = Geometry.newFloat2();
		{
			if (settingsLayer == null) {
				Logger.d(layer);
				layer.printChildren();
				Err.reportError("missing tag " + TAGS.PARALLAX_SETTINGS);
			}
			PSDtoScene2DConverter.readParallaxSettings(output.parallax_settings, settingsLayer, frame_layer, layerOffset, settings);
			settings.addOffset(layerOffset);
		}

		for (int i = 0; i < layer.numberOfChildren(); i++) {
			final PSDLayer child = layer.getChild(i);
			final String childName = child.getName();
			if (childName.startsWith(TAGS.PARALLAX_SETTINGS)) {

			} else {
				final LayerElement childElement = settings.newLayerElement();
				final SceneStructure structure = settings.getStructure();

				PSDtoScene2DConverter.convert(stack, child, childElement, settings);
				output.children.addElement(childElement, structure);
			}
		}
		final Float2 checkLayerOffset = settings.removeOffset();
		Debug.checkTrue(checkLayerOffset == layerOffset);
	}

	private static void convertProgress (final LayersStack stack, final PSDLayer input_parent, final LayerElement output,
		final ConvertionSettings settings) {
		Err.reportError("Not supported");
		final String name = input_parent.getName();
		output.is_hidden = !input_parent.isVisible();

		output.name = name;

		final PSDLayer progress = input_parent.findChildByNamePrefix(TAGS.PROGRESS);

		output.progress_settings = new ProgressSettings();
		{
			final PSDLayer type = progress.findChildByNamePrefix(TAGS.TYPE);

			if (type == null) {
// stack.print();
				input_parent.printChildren();
				Err.reportError("Missing tag <" + TAGS.TYPE + ">");
			} else {
				final String typevalue = PSDtoScene2DConverter.readParameter(type, TAGS.TYPE).toUpperCase();
				output.progress_settings.type = ProgressSettings.TYPE.valueOf(typevalue);
			}
		}
		{
			final SceneStructure structure = settings.getStructure();
			final PSDLayer raster = progress.findChildByNamePrefix(TAGS.RASTER);

			if (raster == null) {
// stack.print();
				Err.reportError("Missing tag <@" + TAGS.RASTER + ">");
			} else {
				final LayerElement rasterNode = settings.newLayerElement();
				PSDtoScene2DConverter.convert(stack, raster.getChild(0), rasterNode, settings);
				output.children.addElement(rasterNode, structure);
			}
		}

	}

	private static void convertRaster (final PSDLayer input, final LayerElement output, final ConvertionSettings settings) {
		final PSDRasterPosition position = input.getRaster().getPosition();
		output.is_hidden = !input.isVisible();
		output.name = input.getName();

		if (output.name.startsWith("@")) {
			Err.reportError("Bad layer name: " + output.name);
		}

		// if (input.getName().startsWith("area_touch1")) {
		// L.d();
		// }
		final Float2 offset = settings.getCurrentOffset();

		final double scale_factor = settings.getScaleFactor();
		output.is_raster = true;
		output.blend_mode = RASTER_BLEND_MODE.valueOf(input.getMode().toString());
		output.position_x = (position.getX() * scale_factor) + offset.getX();
		output.position_y = (position.getY() * scale_factor) + offset.getY();
		output.width = position.getWidth() * scale_factor;
		output.opacity = input.getOpacity();
		output.height = position.getHeight() * scale_factor;
		final PsdRepackerNameResolver naming = settings.getNaming();
		final SceneStructurePackingResult result = settings.getResult();
		final String raster_name = naming.getPSDLayerName(input).toString();
		output.raster_id = raster_name;
		result.addRequiredAsset(Names.newID(output.raster_id), Collections.newList(input));
	}

	private static void convertShader (final PSDLayer input, final LayerElement output, final ConvertionSettings settings) {

		final PSDLayer shader_node = input.findChildByNamePrefix(TAGS.R3_SHADER);
		ShaderSettings shader_settings = null;
		Debug.checkNull("shader_node", shader_node);

		shader_settings = new ShaderSettings();

		output.is_hidden = !input.isVisible();
		output.is_shader = true;
		output.shader_settings = shader_settings;

		{
			final PSDLayer id_layer = PSDtoScene2DConverter.findChild(TAGS.ID, shader_node);
			if (id_layer == null) {
				Err.reportError("Missing tag <" + TAGS.ID + ">");
			} else {
				final String id_string = PSDtoScene2DConverter.readParameter(id_layer, TAGS.ID);
				output.shader_id = id_string;
				output.name = input.getName();
				final SceneStructurePackingResult result = settings.getResult();
				result.addRequiredAsset(Names.newID(id_string), Collections.newList(shader_node));
			}
		}

		{
			final double scale_factor = settings.getScaleFactor();
			final PSDLayer origin = shader_node.findChildByNamePrefix(TAGS.ORIGIN);
			if (origin != null) {
				final double shader_x = origin.getRaster().getPosition().getX() * scale_factor;
				final double shader_y = origin.getRaster().getPosition().getY() * scale_factor;
				final ShaderParameterValue canvas_x = new ShaderParameterValue(SHADER_PARAMETER.POSITION_X, "" + shader_x,
					ShaderParameterType.FLOAT);
				final ShaderParameterValue canvas_y = new ShaderParameterValue(SHADER_PARAMETER.POSITION_Y, "" + shader_y,
					ShaderParameterType.FLOAT);

				shader_settings.params.add(canvas_x);
				shader_settings.params.add(canvas_y);

				final PSDLayer radius = shader_node.findChildByNamePrefix(TAGS.RADIUS);
				if (radius != null) {
					final double rx = radius.getRaster().getPosition().getX() * scale_factor;
					final double ry = radius.getRaster().getPosition().getY() * scale_factor;
					final double shader_radius = FloatMath.distance(shader_x, shader_y, rx, ry);

					final ShaderParameterValue radius_p = new ShaderParameterValue(SHADER_PARAMETER.RADIUS, "" + shader_radius,
						ShaderParameterType.FLOAT);

					shader_settings.params.add(radius_p);

				} else {
					Err.reportError("Shader radius not found: " + shader_node);
				}
			}

		}

	}

	private static void convertText (final LayersStack stack, final PSDLayer input_parent, final LayerElement output,
		final ConvertionSettings settings) {
		final SceneStructure structure = settings.getStructure();
		final String name = input_parent.getName();
		output.is_hidden = !input_parent.isVisible();
		output.is_text = true;
		output.name = name;

		output.text_settings = new TextSettings();

		final PSDLayer input = input_parent.findChildByNamePrefix(TAGS.R3_TEXT);

		final PSDLayer frame = input.findChildByNamePrefix(TAGS.FRAME);
		{
			if (frame != null) {
				Err.reportError("Unsupported tag: " + TAGS.FRAME);
			}
		}
		{

			final double scale_factor = settings.getScaleFactor();
			final PSDLayer background = input.findChildByNamePrefix(TAGS.BACKGROUND);
			if (background != null) {
				final PSDLayer child = background.getChild(0);
				final LayerElement raster_element = settings.newLayerElement();
				PSDtoScene2DConverter.convertRaster(child, raster_element, settings);
				output.children.addElement(raster_element, structure);

				raster_element.position_x = 0;
				raster_element.position_y = 0;

				final PSDRaster raster = child.getRaster();
				output.position_x = raster.getPosition().getX() * scale_factor;
				output.position_y = raster.getPosition().getY() * scale_factor;

				// String text_value_asset_id_string =
				// readParameter(id.getName(), TAGS.ID);
				// AssetID text_value_asset_id =
				// naming.childText(text_value_asset_id_string);
				// output.text_settings.text_value_asset_id =
				// text_value_asset_id.toString();
				// result.addRequiredAsset(text_value_asset_id,
				// JUtils.newList(input));
			}
		}
		{
			final PSDLayer id = PSDtoScene2DConverter.findChild(TAGS.ID, input);
			if (id == null) {
				Err.reportError("Missing tag <" + TAGS.ID + ">");
			} else {
				final String bar_id_string = PSDtoScene2DConverter.readParameter(id, TAGS.ID);
				final PsdRepackerNameResolver naming = settings.getNaming();
				final ID bar_id = naming.childText(bar_id_string);
				output.textbar_id = bar_id.toString();
			}
		}
		{
			final PSDLayer text_node = input.findChildByNamePrefix(TAGS.TEXT);
			Logger.d("text_node", text_node);
			if (text_node != null) {
				final PSDLayer id = PSDtoScene2DConverter.findChild(TAGS.ID, text_node);
				final PSDLayer string = PSDtoScene2DConverter.findChild(TAGS.STRING, text_node);

				if (id == null && string == null) {
					Err.reportError("Missing tag <" + TAGS.ID + ">");
				} else {
					if (id != null) {
						final String text_value_asset_id_string = PSDtoScene2DConverter.readParameter(id, TAGS.ID);
						final PsdRepackerNameResolver naming = settings.getNaming();
						final ID text_value_asset_id = Names.newID(text_value_asset_id_string);
// final AssetID text_value_asset_id = naming.childText(text_value_asset_id_string);

						output.text_settings.text_value_asset_id = text_value_asset_id.toString();
						final SceneStructurePackingResult result = settings.getResult();
						result.addRequiredAsset(text_value_asset_id, Collections.newList(input));
					}
					if (string != null) {
						final String stringTest = PSDtoScene2DConverter.readStrings(string);
						output.text_settings.text_value_raw = stringTest;
					}
				}
// final AssetID child_scene_asset_id = null;
// result.addRequiredRaster(child_scene_asset_id, JUtils.newList(input_parent, input, background));
			}
		}
		final double scale_factor = settings.getScaleFactor();
		{
			final PSDLayer font_node = input.findChildByNamePrefix(TAGS.FONT);
			if (font_node != null) {
				{
					final PSDLayer size = PSDtoScene2DConverter.findChild(TAGS.SIZE, font_node);
					if (size == null) {
						Err.reportError("Missing tag <@" + TAGS.SIZE + ">");
					} else {
						final String font_size_string = PSDtoScene2DConverter.readParameter(size.getName(), TAGS.SIZE);
						output.text_settings.font_settings.font_size = (float)(Float.parseFloat(font_size_string) * scale_factor);
// output.text_settings.font_settings.font_scale = (float)scale_factor;
						output.text_settings.font_settings.value_is_in_pixels = true;
					}
				}
				{
					final PSDLayer color = PSDtoScene2DConverter.findChild(TAGS.COLOR, font_node);
					if (color == null) {
						Err.reportError("Missing tag <@" + TAGS.COLOR + ">");
					} else {
						final String font_color_string = PSDtoScene2DConverter.readParameter(color.getName(), TAGS.COLOR);
						output.text_settings.font_settings.font_color = "#" + Colors.newColor(font_color_string).toFullHexString();
					}
				}
				// AssetID child_scene_asset_id = null;
				// result.addRequiredRaster(child_scene_asset_id,
				// JUtils.newList(input_parent, input, background));
			}
			final PSDLayer font_name = font_node.findChildByNamePrefix(TAGS.NAME);
			if (font_name != null) {
				final String font_name_string = PSDtoScene2DConverter.readParameter(font_name.getName(), TAGS.NAME);
				output.text_settings.font_settings.name = font_name_string;
				final SceneStructurePackingResult result = settings.getResult();
				result.addRequiredAsset(Names.newID(font_name_string), Collections.newList(input));
			}
			final PSDLayer padding = input.findChildByNamePrefix(TAGS.PADDING);
			if (padding != null) {
				String padding_string = PSDtoScene2DConverter.readParameter(padding.getName(), TAGS.PADDING);
				padding_string = padding_string.substring(0, padding_string.indexOf("pix"));
				output.text_settings.padding = (float)(Float.parseFloat(padding_string) * scale_factor);
			}
		}
	}

	private static String readStrings (final PSDLayer string) {
		final StringBuilder b = new StringBuilder();
		for (int i = 0; i < string.numberOfChildren(); i++) {
			if (i != 0) {
				b.append("\n");
			}
			b.append(string.getChild(i).getName());

		}
		return b.toString();
	}

	private static void extractButtonOptions (final LayersStack stack, final PSDLayer options, final LayerElement output,
		final ConvertionSettings settings, final Float2 origin) {
		if (options == null) {
			return;
		}
		for (int i = 0; i < options.numberOfChildren(); i++) {
			final PSDLayer child = options.getChild(i);
			final LayerElement converted = settings.newLayerElement();
			;

			PSDtoScene2DConverter.convert(stack, child, converted, settings);
			if (!converted.is_raster) {
// stack.print();
				Err.reportError(converted + "");
			}
			final SceneStructure structure = settings.getStructure();
			output.children.addElement(converted, structure);
			converted.position_x = converted.position_x - origin.getX();
			converted.position_y = converted.position_y - origin.getY();
		}

	}

	private static void extractButtonRaster (final LayersStack stack, final PSDLayer raster, final LayerElement output,
		final ConvertionSettings settings, final Float2 origin) {

		{
			final PSDLayer on_released = raster.findChildByName(TAGS.BUTTON_ON_RELEASED);
			if (on_released != null) {
				final LayerElement converted = settings.newLayerElement();
				;
				PSDtoScene2DConverter.convert(stack, on_released, converted, settings);
				output.input_settings.on_released = converted;
				converted.position_x = converted.position_x - origin.getX();
				converted.position_y = converted.position_y - origin.getY();
			}
		}

		{
			final PSDLayer on_hover = raster.findChildByName(TAGS.BUTTON_ON_HOVER);
			if (on_hover != null) {
				final LayerElement converted = settings.newLayerElement();
				;
				PSDtoScene2DConverter.convert(stack, on_hover, converted, settings);
				output.input_settings.on_hover = converted;
				converted.position_x = converted.position_x - origin.getX();
				converted.position_y = converted.position_y - origin.getY();
			}
		}

		{
			final PSDLayer on_press = raster.findChildByName(TAGS.BUTTON_ON_PRESS);
			if (on_press != null) {
				final LayerElement converted = settings.newLayerElement();
				;
				PSDtoScene2DConverter.convert(stack, on_press, converted, settings);
				output.input_settings.on_press = converted;
				converted.position_x = converted.position_x - origin.getX();
				converted.position_y = converted.position_y - origin.getY();
			}
		}

		{
			final PSDLayer on_pressed = raster.findChildByName(TAGS.BUTTON_ON_PRESSED);
			if (on_pressed != null) {
				final LayerElement converted = settings.newLayerElement();
				;
				PSDtoScene2DConverter.convert(stack, on_pressed, converted, settings);
				output.input_settings.on_pressed = converted;
				converted.position_x = converted.position_x - origin.getX();
				converted.position_y = converted.position_y - origin.getY();
			}
		}

		{
			final PSDLayer on_release = raster.findChildByName(TAGS.BUTTON_ON_RELEASE);
			if (on_release != null) {
				final LayerElement converted = settings.newLayerElement();
				;
				PSDtoScene2DConverter.convert(stack, on_release, converted, settings);
				output.input_settings.on_release = converted;
				converted.position_x = converted.position_x - origin.getX();
				converted.position_y = converted.position_y - origin.getY();
			}
		}

	}

	private static void extractTouchArea (final LayersStack stack, final PSDLayer area, final LayerElement output,
		final ConvertionSettings settings, final Float2 origin) {

		final LayerElement converted = settings.newLayerElement();

		PSDtoScene2DConverter.convert(stack, area, converted, settings);
		if (!converted.is_raster) {
// stack.print();
			Err.reportError(converted + "");
		}
		final SceneStructure structure = settings.getStructure();
		output.children.addElement(converted, structure);
		converted.position_x = converted.position_x - origin.getX();
		converted.position_y = converted.position_y - origin.getY();

	}

	private static PSDLayer findChild (final String name_perefix, final PSDLayer input) {
		for (int i = 0; i < input.numberOfChildren(); i++) {
			final PSDLayer child = input.getChild(i);
			if (child.getName().startsWith(name_perefix)) {
				return child;
			}
		}
		return null;
	}

	private static long getTime (final String anchor_time_string) {
		final List<String> parts = Collections.newList(anchor_time_string.split(":"));
		long ms = 0;
		long sec = 0;
		long min = 0;

		parts.reverse();

		if (parts.size() > 0) {
			ms = (long)(1000 * Double.parseDouble(parts.getElementAt(0)));
		}
		if (parts.size() > 1) {
			sec = (long)(1000 * Double.parseDouble(parts.getElementAt(1)));
		}

		if (parts.size() > 2) {
			min = (long)(1000 * Double.parseDouble(parts.getElementAt(2)));
		}

		final long result = (min * 60 * 1000) + (sec * 1000) + ms;
		return result;
	}

	private static void packAnimationEvents (final PSDLayer events_list, final ActionsGroup e_list,
		final ChildAssetsNameResolver naming) {
		e_list.actions = new ArrayList<>();
		for (int i = 0; i < events_list.numberOfChildren(); i++) {
			final PSDLayer element = events_list.getChild(i);
			String event_id = PSDtoScene2DConverter.readParameter(element, TAGS.ID);

			event_id = naming.childEvent(event_id).toString();

			final Action event = new Action();
			event.animation_id = event_id;
			event.is_start_animation = true;
			e_list.actions.add(event);

		}
	}

	private static Float2 readLeftAnchor (final PSDLayer right) {
		final Float2 result = Geometry.newFloat2();
		final PSDRaster raster = right.getRaster();
		Debug.checkNull(raster);
		result.setX(raster.getPosition().getX() + raster.getDimentions().getWidth());
		result.setY(raster.getPosition().getY() + raster.getDimentions().getHeight());
		return result;
	}

	private static void readParallaxSettings (final ParallaxSettings parallax_settings, final PSDLayer layer,
		final PSDLayer frame_layer, final Float2 layerOffset, final ConvertionSettings settings) {

		PSDtoScene2DConverter.readParallaxSettingX(parallax_settings, layer, frame_layer, layerOffset, settings);
		PSDtoScene2DConverter.readParallaxSettingY(parallax_settings, layer, frame_layer, layerOffset, settings);
		PSDtoScene2DConverter.readParallaxSettingZ(parallax_settings, layer, frame_layer, layerOffset, settings);

	}

	private static void readParallaxSettingX (final ParallaxSettings parallax_settings, final PSDLayer layer,
		final PSDLayer frame_layer, final Float2 layerOffset, final ConvertionSettings settings) {

		final PSDLayer mx = layer.findChildByNamePrefix(TAGS.PARALLAX_MULTIPLIER_X);
		parallax_settings.multiplier_x = 1.0f;
		if (mx != null) {
			parallax_settings.multiplier_x = Float.parseFloat(PSDtoScene2DConverter.readParameter(mx, TAGS.PARALLAX_MULTIPLIER_X));
			return;
		}

		if (frame_layer == null) {
			return;
		}

		final PSDRaster frame = frame_layer.getRaster();
		final PSDRasterPosition position = frame.getPosition();
		final PSDRasterDimentions dim = frame.getDimentions();

		final PSDLayer right = layer.findChildByNamePrefix(TAGS.ANCHOR_RIGHT);
		final PSDLayer left = layer.findChildByNamePrefix(TAGS.ANCHOR_LEFT);
		if ((right == null) && (left == null)) {
			return;
		}

		final Float2 rightPos = PSDtoScene2DConverter.readRightAnchor(right);
		final Float2 leftPos = PSDtoScene2DConverter.readLeftAnchor(left);
		final double layerWidth = rightPos.getX() - leftPos.getX();

		final double scaleFactor = settings.getScaleFactor();

		final double offfsetX = -(leftPos.getX() - position.getX());
		layerOffset.setX(offfsetX * scaleFactor);

		parallax_settings.multiplier_x = (float)((layerWidth - dim.getWidth()) / dim.getWidth());

	}

	private static void readParallaxSettingY (final ParallaxSettings parallax_settings, final PSDLayer layer,
		final PSDLayer frame_layer, final Float2 layerOffset, final ConvertionSettings settings) {
		parallax_settings.multiplier_y = 0.0f;
	}

	private static void readParallaxSettingZ (final ParallaxSettings parallax_settings, final PSDLayer layer,
		final PSDLayer frame_layer, final Float2 layerOffset, final ConvertionSettings settings) {
		parallax_settings.multiplier_z = 1.0f;
	}

	private static String readParameter (final PSDLayer layer, final String id) {
		String id_string = PSDtoScene2DConverter.readParameter(layer.getName(), id);
// if (id_string.length() > 0) {
// return id_string;
// }
		PSDLayer next = layer;
// id_string = "";
		while (next.numberOfChildren() > 0) {
			next = next.getChild(0);
			id_string = id_string + "." + next.getName();
		}
		return id_string;
	}

	private static String readParameter (final String raw_value, final String prefix) {

		Debug.checkEmpty("raw_value", raw_value);
		Debug.checkEmpty("prefix", prefix);

		Debug.checkNull("raw_value", raw_value);
		Debug.checkNull("prefix", prefix);

		return raw_value.substring(prefix.length(), raw_value.length());
	}

	private static Float2 readRightAnchor (final PSDLayer right) {
		final Float2 result = Geometry.newFloat2();
		final PSDRaster raster = right.getRaster();
		Debug.checkNull(raster);
		result.setX(raster.getPosition().getX());
		result.setY(raster.getPosition().getY());
		return result;
	}

	private static void setupCamera (final LayersStack stack, final PSDLayer camera_layer, final LayerElement element,
		final double scale_factor) {
		if (camera_layer == null) {
			return;
		}
		final CameraSettings cameraSettings = new CameraSettings();

		final PSDLayer area = camera_layer.findChildByNamePrefix(TAGS.AREA);
		final PSDLayer mode = camera_layer.findChildByNamePrefix(TAGS.MODE);
		PSDLayer origin = camera_layer.findChildByNamePrefix(TAGS.ORIGIN);

		if (origin == null) {
			origin = area;
			L.d("stack", stack);
		}
		Debug.checkNull("origin", origin);
		Debug.checkNull("area", area);

		Debug.checkNull("mode", mode);
		final String modeString = PSDtoScene2DConverter.readParameter(mode, TAGS.MODE);
		cameraSettings.mode = MODE.valueOf(modeString.toUpperCase());

		{
			final PSDRaster originRaster = origin.getRaster();
			Debug.checkNull("raster", originRaster);

			final PSDRaster areaRaster = area.getRaster();
			Debug.checkNull("raster", areaRaster);

			cameraSettings.position_x = (originRaster.getPosition().getX() * scale_factor)
				+ ((originRaster.getBufferedImage().getWidth() * scale_factor) / 2d);
			cameraSettings.position_y = (originRaster.getPosition().getY() * scale_factor)
				+ ((originRaster.getBufferedImage().getHeight() * scale_factor) / 2d);
			;

			cameraSettings.width = areaRaster.getDimentions().getWidth() * scale_factor;
			cameraSettings.height = areaRaster.getDimentions().getHeight() * scale_factor;

			cameraSettings.origin_relative_x = (cameraSettings.position_x - (areaRaster.getPosition().getX() * scale_factor))
				/ cameraSettings.width;
			cameraSettings.origin_relative_y = (cameraSettings.position_y - (areaRaster.getPosition().getY() * scale_factor))
				/ cameraSettings.height;

		}

		element.camera_settings = cameraSettings;

	}

}
