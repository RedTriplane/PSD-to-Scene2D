
package com.jfixby.tool.psd2scene2d;

import java.util.Vector;

import com.jfixby.cmns.api.assets.AssetID;
import com.jfixby.cmns.api.assets.Names;
import com.jfixby.cmns.api.collections.Collections;
import com.jfixby.cmns.api.collections.List;
import com.jfixby.cmns.api.collections.Map;
import com.jfixby.cmns.api.debug.Debug;
import com.jfixby.cmns.api.err.Err;
import com.jfixby.cmns.api.floatn.Float2;
import com.jfixby.cmns.api.geometry.Geometry;
import com.jfixby.cmns.api.log.L;
import com.jfixby.cmns.api.math.FloatMath;
import com.jfixby.psd.unpacker.api.PSDLayer;
import com.jfixby.psd.unpacker.api.PSDRaster;
import com.jfixby.psd.unpacker.api.PSDRasterPosition;
import com.jfixby.r3.api.shader.srlz.SHADER_PARAMETERS;
import com.jfixby.r3.ext.api.scene2d.srlz.Action;
import com.jfixby.r3.ext.api.scene2d.srlz.ActionsGroup;
import com.jfixby.r3.ext.api.scene2d.srlz.Anchor;
import com.jfixby.r3.ext.api.scene2d.srlz.AnimationSettings;
import com.jfixby.r3.ext.api.scene2d.srlz.CameraSettings;
import com.jfixby.r3.ext.api.scene2d.srlz.ChildSceneSettings;
import com.jfixby.r3.ext.api.scene2d.srlz.InputSettings;
import com.jfixby.r3.ext.api.scene2d.srlz.LayerElement;
import com.jfixby.r3.ext.api.scene2d.srlz.RASTER_BLEND_MODE;
import com.jfixby.r3.ext.api.scene2d.srlz.Scene2DPackage;
import com.jfixby.r3.ext.api.scene2d.srlz.SceneStructure;
import com.jfixby.r3.ext.api.scene2d.srlz.ShaderParameterType;
import com.jfixby.r3.ext.api.scene2d.srlz.ShaderParameterValue;
import com.jfixby.r3.ext.api.scene2d.srlz.ShaderSettings;
import com.jfixby.r3.ext.api.scene2d.srlz.TextSettings;

public class PSDtoScene2DConverter {

	public static ConversionResult convert (final Scene2DPackage container, final AssetID package_prefix, final PSDLayer root,
		final Map<PSDLayer, AssetID> raster_names) {
		final ConversionResult results = new ConversionResult();
		// naming.print("naming");

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
					L.d("missing NAME tag");
					continue;
				}

				final PSDLayer camera_layer = candidate.findChildByNamePrefix(TAGS.CAMERA);

				float scale_factor = 1f;
				{
					final PSDLayer divisor = candidate.findChildByNamePrefix(TAGS.SCALE_DIVISOR);
					if (divisor != null) {
						final String divisor_string = readParameter(divisor, TAGS.SCALE_DIVISOR);
						scale_factor = 1f / Float.parseFloat(divisor_string);
					}
				}
				final SceneStructure structure = new SceneStructure();

				final SceneStructurePackingResult result_i = new SceneStructurePackingResult(structure);

				result_i.setScaleFactor(scale_factor);

				container.structures.addElement(structure);
				structure.structure_name = readParameter(name_layer.getName(), TAGS.STRUCTURE_NAME);
				structure.structure_name = package_prefix.child(structure.structure_name).toString();
				final LayerElement element = structure.root;

				final PsdRepackerNameResolver naming = new PsdRepackerNameResolver(Names.newAssetID(structure.structure_name),
					raster_names);

				convert(content_layer, element, naming, result_i, scale_factor);

				element.name = structure.structure_name;

				setupCamera(camera_layer, element, scale_factor);

				L.d("structure found", structure.structure_name);

				results.putResult(structure, result_i);
			}
		}

		return results;
	}

	private static void setupCamera (final PSDLayer camera_layer, final LayerElement element, final double scale_factor) {
		if (camera_layer == null) {
			return;
		}
		final CameraSettings settings = new CameraSettings();

		final PSDLayer area = camera_layer.findChildByNamePrefix(TAGS.AREA);
		if (area == null) {
			throw new Error("Tag <" + TAGS.AREA + "> not found.");
		}

		{
			final PSDRaster raster = area.getRaster();
			Debug.checkNull("raster", raster);

			settings.position_x = raster.getPosition().getX() * scale_factor;
			settings.position_y = raster.getPosition().getY() * scale_factor;
			settings.width = raster.getDimentions().getWidth() * scale_factor;
			settings.height = raster.getDimentions().getHeight() * scale_factor;

		}

		element.camera_settings = settings;

	}

	private static void convert (final PSDLayer input, final LayerElement output, final ChildAssetsNameResolver naming,
		final SceneStructurePackingResult result, final double scale_factor) {

		if (input.isFolder()) {
			final PSDLayer animation_node = input.findChildByNamePrefix(TAGS.ANIMATION);
			final PSDLayer childscene_node = input.findChildByNamePrefix(TAGS.CHILD_SCENE);
			final PSDLayer text_node = input.findChildByNamePrefix(TAGS.R3_TEXT);
			final PSDLayer shader_node = input.findChildByNamePrefix(TAGS.R3_SHADER);
			final PSDLayer user_input = input.findChildByNamePrefix(TAGS.INPUT);
			// PSDLayer events_node = input.findChild(EVENT);
			if (animation_node != null) {
				if (input.numberOfChildren() != 1) {
					throw new Error("Annotation problem (only one child allowed). This is not an animation node: " + input);
				}
				convertAnimation(input, output, naming, result, scale_factor);
			} else if (childscene_node != null) {
				if (input.numberOfChildren() != 1) {
					throw new Error("Annotation problem (only one child allowed). This is not an child scene node: " + input);
				}
				convertChildScene(input, output, naming, result, scale_factor);
			} else if (shader_node != null) {
				if (input.numberOfChildren() != 1) {
					throw new Error("Annotation problem (only one child allowed). This is not an	 child scene node: " + input);
				}
				convertShader(input, output, naming, result, scale_factor);
			} else if (text_node != null) {
				if (input.numberOfChildren() != 1) {
					throw new Error("Annotation problem (only one child allowed). This is not an child scene node: " + input);
				}
				convertText(input, output, naming, result, scale_factor);
			} else if (user_input != null) {
				if (input.numberOfChildren() != 1) {
					throw new Error("Annotation problem (only one child allowed). This is not an child scene node: " + input);
				}
				convertInput(input, output, naming, result, scale_factor);
			} else if (false) {
				if (input.numberOfChildren() != 1) {
					throw new Error("Annotation problem (only one child allowed). This is not an child scene node: " + input);
				}
				// convertEventsSequence(input, output, naming, result,
				// scale_factor);
			} else {
				convertFolder(input, output, naming, result, scale_factor);
			}

		} else if (input.isRaster()) {
			convertRaster(input, output, naming, result, scale_factor);
		}

	}

	private static void convertShader (final PSDLayer input, final LayerElement output, final ChildAssetsNameResolver naming,
		final SceneStructurePackingResult result, final double scale_factor) {

		final PSDLayer shader_node = input.findChildByNamePrefix(TAGS.R3_SHADER);
		ShaderSettings shader_settings = null;
		Debug.checkNull("shader_node", shader_node);

		shader_settings = new ShaderSettings();

		output.is_hidden = !input.isVisible();
		output.is_shader = true;
		output.shader_settings = shader_settings;

		{
			final PSDLayer id_layer = findChild(TAGS.ID, shader_node);
			if (id_layer == null) {
				throw new Error("Missing tag <" + TAGS.ID + ">");
			} else {
				final String id_string = readParameter(id_layer, TAGS.ID);
				output.shader_id = id_string;
				output.name = input.getName();

				result.addRequiredAsset(Names.newAssetID(id_string), Collections.newList(shader_node));
			}
		}

		{

			final PSDLayer origin = shader_node.findChildByNamePrefix(TAGS.ORIGIN);
			if (origin != null) {
				final double shader_x = origin.getRaster().getPosition().getX() * scale_factor;
				final double shader_y = origin.getRaster().getPosition().getY() * scale_factor;
				final ShaderParameterValue canvas_x = new ShaderParameterValue(SHADER_PARAMETERS.POSITION_X, "" + shader_x,
					ShaderParameterType.FLOAT);
				final ShaderParameterValue canvas_y = new ShaderParameterValue(SHADER_PARAMETERS.POSITION_Y, "" + shader_y,
					ShaderParameterType.FLOAT);

				shader_settings.params.addElement(canvas_x);
				shader_settings.params.addElement(canvas_y);

				final PSDLayer radius = shader_node.findChildByNamePrefix(TAGS.RADIUS);
				if (radius != null) {
					final double rx = radius.getRaster().getPosition().getX() * scale_factor;
					final double ry = radius.getRaster().getPosition().getY() * scale_factor;
					final double shader_radius = FloatMath.distance(shader_x, shader_y, rx, ry);

					final ShaderParameterValue radius_p = new ShaderParameterValue(SHADER_PARAMETERS.RADIUS, "" + shader_radius,
						ShaderParameterType.FLOAT);

					shader_settings.params.addElement(radius_p);

				} else {
					Err.reportError("Shader radius not found: " + shader_node);
				}
			}

		}

	}

	private static String readParameter (final PSDLayer layer, final String id) {
		String id_string = readParameter(layer.getName(), id);
		if (id_string.length() > 0) {
			return id_string;
		}
		PSDLayer next = layer;
		id_string = "";
		do {
			next = next.getChild(0);
			id_string = id_string + next.getName();
			if (next.numberOfChildren() > 0) {
				id_string = id_string + ".";
			}
		} while (next.numberOfChildren() > 0);
		return id_string;
	}

	private static void convertChildScene (final PSDLayer input_parent, final LayerElement output,
		final ChildAssetsNameResolver naming, final SceneStructurePackingResult result, final double scale_factor) {

		final String name = input_parent.getName();
		output.is_hidden = !input_parent.isVisible();
		output.is_child_scene = true;
		output.name = name;

		output.child_scene_settings = new ChildSceneSettings();

		final PSDLayer input = input_parent.findChildByNamePrefix(TAGS.CHILD_SCENE);

		final PSDLayer frame = input.findChildByNamePrefix(TAGS.FRAME);
		{
			if (frame != null) {
				throw new Error("Unsupported tag: " + TAGS.FRAME);
			}
		}

		final PSDLayer origin = input.findChildByNamePrefix(TAGS.ORIGIN);
		if (origin != null) {
			output.child_scene_settings.frame_position_x = origin.getRaster().getPosition().getX() * scale_factor;
			output.child_scene_settings.frame_position_y = origin.getRaster().getPosition().getY() * scale_factor;

			output.child_scene_settings.frame_width = origin.getRaster().getDimentions().getWidth();
			output.child_scene_settings.frame_height = origin.getRaster().getDimentions().getHeight();
		}
		{
			final PSDLayer id = findChild(TAGS.ID, input);

			if (id == null) {
				throw new Error("Missing tag <@" + TAGS.ID + ">");
			} else {
				final String child_id = readParameter(id, TAGS.ID);

				final AssetID child_scene_asset_id = naming.childScene(child_id);

				output.child_scene_settings.child_scene_id = child_scene_asset_id.toString();

				// L.e("!!!!!!");
				result.addRequiredAsset(child_scene_asset_id, Collections.newList(input_parent, input, origin));
			}
		}

	}

	private static void convertText (final PSDLayer input_parent, final LayerElement output, final ChildAssetsNameResolver naming,
		final SceneStructurePackingResult result, final double scale_factor) {

		final String name = input_parent.getName();
		output.is_hidden = !input_parent.isVisible();
		output.is_text = true;
		output.name = name;

		output.text_settings = new TextSettings();

		final PSDLayer input = input_parent.findChildByNamePrefix(TAGS.R3_TEXT);

		final PSDLayer frame = input.findChildByNamePrefix(TAGS.FRAME);
		{
			if (frame != null) {
				throw new Error("Unsupported tag: " + TAGS.FRAME);
			}
		}
		{
			final PSDLayer background = input.findChildByNamePrefix(TAGS.BACKGROUND);
			if (background != null) {
				final PSDLayer child = background.getChild(0);
				final LayerElement raster_element = new LayerElement();
				convertRaster(child, raster_element, naming, result, scale_factor);
				output.children.addElement(raster_element);

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
			final PSDLayer id = findChild(TAGS.ID, input);
			if (id == null) {
				throw new Error("Missing tag <@" + TAGS.ID + ">");
			} else {
				final String bar_id_string = readParameter(id, TAGS.ID);
				final AssetID bar_id = naming.childText(bar_id_string);
				output.textbar_id = bar_id.toString();
			}
		}
		{
			final PSDLayer text_node = input.findChildByNamePrefix(TAGS.TEXT);
			if (text_node != null) {
				final PSDLayer id = findChild(TAGS.ID, text_node);
				if (id == null) {
					throw new Error("Missing tag <@" + TAGS.ID + ">");
				} else {
					final String text_value_asset_id_string = readParameter(id.getName(), TAGS.ID);
					final AssetID text_value_asset_id = naming.childText(text_value_asset_id_string);
					output.text_settings.text_value_asset_id = text_value_asset_id.toString();
					result.addRequiredAsset(text_value_asset_id, Collections.newList(input));
				}
				// AssetID child_scene_asset_id = null;
				// result.addRequiredRaster(child_scene_asset_id,
				// JUtils.newList(input_parent, input, background));
			}
		}
		{
			final PSDLayer font_node = input.findChildByNamePrefix(TAGS.FONT);
			if (font_node != null) {
				final PSDLayer size = findChild(TAGS.SIZE, font_node);
				if (size == null) {
					throw new Error("Missing tag <@" + TAGS.SIZE + ">");
				} else {
					final String font_size_string = readParameter(size.getName(), TAGS.SIZE);
					output.text_settings.font_settings.font_size = (Float.parseFloat(font_size_string));
					output.text_settings.font_settings.font_scale = (float)scale_factor;
					output.text_settings.font_settings.value_is_in_pixels = true;
				}
				// AssetID child_scene_asset_id = null;
				// result.addRequiredRaster(child_scene_asset_id,
				// JUtils.newList(input_parent, input, background));
			}
			final PSDLayer font_name = font_node.findChildByNamePrefix(TAGS.NAME);
			if (font_name != null) {
				final String font_name_string = readParameter(font_name.getName(), TAGS.NAME);
				output.text_settings.font_settings.name = font_name_string;
				result.addRequiredAsset(Names.newAssetID(font_name_string), Collections.newList(input));
			}
			final PSDLayer padding = input.findChildByNamePrefix(TAGS.PADDING);
			if (padding != null) {
				String padding_string = readParameter(padding.getName(), TAGS.PADDING);
				padding_string = padding_string.substring(0, padding_string.indexOf("pix"));
				output.text_settings.padding = (float)(Float.parseFloat(padding_string) * scale_factor);
			}
		}
	}

	private static void convertFolder (final PSDLayer input, final LayerElement coutput, final ChildAssetsNameResolver naming,
		final SceneStructurePackingResult result, final double scale_factor) {

		{
			final LayerElement output = coutput;
			// output.shader_settings = shader_settings;
			output.is_hidden = !input.isVisible();
			output.name = input.getName();

			output.is_sublayer = true;

			for (int i = 0; i < input.numberOfChildren(); i++) {
				final PSDLayer child = input.getChild(i);
				// if (shader_node != null && shader_node == child) {
				// continue;
				// }
				final LayerElement element = new LayerElement();
				output.children.addElement(element);
				convert(child, element, naming, result, scale_factor);

				if (element.name.startsWith("@")) {
					throw new Error("Bad layer name: " + element.name);
				}
			}
		}
	}

	private static void convertInput (final PSDLayer input_parent, final LayerElement output, final ChildAssetsNameResolver naming,
		final SceneStructurePackingResult result, final double scale_factor) {

		final String name = input_parent.getName();
		output.is_hidden = !input_parent.isVisible();
		output.is_user_input = true;
		output.name = name;

		final PSDLayer input = input_parent.findChildByNamePrefix(TAGS.INPUT);

		final InputSettings input_settings = new InputSettings();
		output.input_settings = input_settings;

		{
			final PSDLayer debug = findChild(TAGS.DEBUG, input);

			if (debug == null) {
				output.debug_mode = false;
			} else {
				final String debug_mode = readParameter(debug, TAGS.DEBUG);
				output.debug_mode = Boolean.parseBoolean(debug_mode);
			}
		}

		{
			final PSDLayer raster = findChild(TAGS.RASTER, input);

			if (raster == null) {
				throw new Error("Missing button raster: " + input);
			} else {
				extractButtonRaster(raster, output, naming, result, scale_factor);
			}
		}

		{
			final PSDLayer id = findChild(TAGS.ID, input);

			if (id == null) {
				throw new Error("Input @ID tag not found: " + input);
			} else {
				output.input_id = readParameter(id.getName(), TAGS.ID);
				output.input_id = naming.childInput(output.input_id).toString();
			}
		}
		// PSDLayer type = findChild(ANIMATION_TYPE, input);
		{
			final PSDLayer type = findChild(TAGS.TYPE, input);
			if (type == null) {

			} else {
				final String type_value = readParameter(type.getName(), TAGS.TYPE);

				output.input_settings.is_button = TAGS.VALUE_BUTTON.equalsIgnoreCase(type_value);

				// animation_settings.is_positions_modifyer_animation =
				// ANIMATION_TYPE_POSITION_MODIFIER
				// c;

			}

		}

		{
			final PSDLayer touch_area = findChild(TAGS.AREA, input);
			// output.input_settings.areas = new Vector<TouchArea>();
			if (touch_area != null) {
				final LayerElement touch_areas = new LayerElement();
				output.input_settings.touch_area = touch_areas;

				for (int i = 0; i < touch_area.numberOfChildren(); i++) {
					final PSDLayer child = touch_area.getChild(i);
					if (child.isFolder()) {
						throw new Error("Touch area has no dimentions: " + child);
					} else {
						final PSDRaster raster = child.getRaster();
						Debug.checkNull("raster", raster);

						final LayerElement area = new LayerElement();
						touch_areas.children.addElement(area);
						area.position_x = raster.getPosition().getX() * scale_factor;
						area.position_y = raster.getPosition().getY() * scale_factor;
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

	private static void extractButtonRaster (final PSDLayer raster, final LayerElement output,
		final ChildAssetsNameResolver naming, final SceneStructurePackingResult result, final double scale_factor) {

		{
			final PSDLayer on_released = raster.findChildByName(TAGS.BUTTON_ON_RELEASED);
			if (on_released != null) {
				final LayerElement converted = new LayerElement();
				convert(on_released, converted, naming, result, scale_factor);
				output.input_settings.on_released = converted;
			}
		}

		{
			final PSDLayer on_hover = raster.findChildByName(TAGS.BUTTON_ON_HOVER);
			if (on_hover != null) {
				final LayerElement converted = new LayerElement();
				convert(on_hover, converted, naming, result, scale_factor);
				output.input_settings.on_hover = converted;
			}
		}

		{
			final PSDLayer on_press = raster.findChildByName(TAGS.BUTTON_ON_PRESS);
			if (on_press != null) {
				final LayerElement converted = new LayerElement();
				convert(on_press, converted, naming, result, scale_factor);
				output.input_settings.on_press = converted;
			}
		}

		{
			final PSDLayer on_pressed = raster.findChildByName(TAGS.BUTTON_ON_PRESSED);
			if (on_pressed != null) {
				final LayerElement converted = new LayerElement();
				convert(on_pressed, converted, naming, result, scale_factor);
				output.input_settings.on_pressed = converted;
			}
		}

		{
			final PSDLayer on_release = raster.findChildByName(TAGS.BUTTON_ON_RELEASE);
			if (on_release != null) {
				final LayerElement converted = new LayerElement();
				convert(on_release, converted, naming, result, scale_factor);
				output.input_settings.on_release = converted;
			}
		}

	}

	private static void convertAnimation (final PSDLayer input_parent, final LayerElement output,
		final ChildAssetsNameResolver naming, final SceneStructurePackingResult result, final double scale_factor) {

		final String name = input_parent.getName();
		output.is_hidden = !input_parent.isVisible();
		output.is_animation = true;
		output.name = name;

		final PSDLayer input = input_parent.findChildByNamePrefix(TAGS.ANIMATION);

		final AnimationSettings animation_settings = new AnimationSettings();
		output.animation_settings = animation_settings;

		{
			final PSDLayer looped = findChild(TAGS.IS_LOOPED, input);

			if (looped == null) {
				animation_settings.is_looped = true;
			} else {
				final String is_looped = readParameter(looped, TAGS.IS_LOOPED);
				animation_settings.is_looped = Boolean.parseBoolean(is_looped);
			}
		}

		{
			final PSDLayer debug = findChild(TAGS.DEBUG, input);

			if (debug == null) {
				output.debug_mode = false;
			} else {
				final String debug_mode = readParameter(debug.getName(), TAGS.DEBUG);
				output.debug_mode = Boolean.parseBoolean(debug_mode);
			}
		}

		{
			final PSDLayer autostart = findChild(TAGS.AUTOSTART, input);

			if (autostart == null) {
				animation_settings.autostart = false;
			} else {
				final String autostart_string = readParameter(autostart.getName(), TAGS.AUTOSTART);
				animation_settings.autostart = Boolean.parseBoolean(autostart_string);
			}
		}

		{
			final PSDLayer id = findChild(TAGS.ID, input);

			if (id == null) {
				throw new Error("Animation ID tag not found: " + input);
			} else {
				output.animation_id = readParameter(id.getName(), TAGS.ID);
				output.animation_id = naming.childAnimation(output.animation_id).toString();
			}
		}
		// PSDLayer type = findChild(ANIMATION_TYPE, input);
		{
			final PSDLayer type = findChild(TAGS.TYPE, input);
			if (type == null) {
				animation_settings.is_positions_modifyer_animation = false;
				animation_settings.is_simple_animation = true;
			} else {
				final String type_value = readParameter(type.getName(), TAGS.TYPE);
				animation_settings.is_positions_modifyer_animation = TAGS.ANIMATION_TYPE_POSITION_MODIFIER
					.equalsIgnoreCase(type_value);

			}

			if (!(animation_settings.is_positions_modifyer_animation || animation_settings.is_simple_animation)) {
				throw new Error("Unknown animation type: " + type);
			}
		}

		if (animation_settings.is_simple_animation) {
			{
				final PSDLayer frames = findChild(TAGS.ANIMATION_FRAMES, input);
				if (frames == null) {
					L.d("Missing <frames> folder in node: " + input);
				}
				Debug.checkNull("frames", frames);
				for (int i = 0; i < frames.numberOfChildren(); i++) {
					final PSDLayer child = frames.getChild(i);
					final LayerElement element = new LayerElement();
					output.children.addElement(element);
					convert(child, element, naming, result, scale_factor);
				}
				if (frames.numberOfChildren() == 0) {
					throw new Error("No frames found for " + output.animation_id);
				}
			}
			{
				final PSDLayer frame = findChild(TAGS.FRAME_TIME, input);
				if (frame == null) {
					// animation_settings.single_frame_time = Long.MAX_VALUE;
					throw new Error("Missing frame time tag: @" + TAGS.FRAME_TIME);

				} else {
					final String type_value = readParameter(frame.getName(), TAGS.FRAME_TIME);
					animation_settings.single_frame_time = "" + Long.parseLong(type_value);
				}
			}
			return;
		}

		if (animation_settings.is_positions_modifyer_animation) {
			final PSDLayer anchors = findChild(TAGS.ANIMATION_ANCHORS, input);
			Debug.checkNull("frames", anchors);
			animation_settings.anchors = new Vector<Anchor>();

			for (int i = 0; i < anchors.numberOfChildren(); i++) {
				final PSDLayer anchor_layer = anchors.getChild(i);
				final String anchor_time_string = anchor_layer.getName();
				final PSDRasterPosition position = anchor_layer.getRaster().getPosition();
				final Anchor anchor = new Anchor();

				anchor.time = "" + getTime(anchor_time_string);
				anchor.position_x = position.getX() * scale_factor;
				anchor.position_y = position.getY() * scale_factor;
				animation_settings.anchors.add(anchor);
			}

			final PSDLayer scene = findChild(TAGS.ANIMATION_SCENE, input);
			final PSDLayer origin_layer = findChild(TAGS.ORIGIN, scene);
			final Float2 origin = Geometry.newFloat2();
			if (origin_layer != null) {
				final PSDRaster raster = origin_layer.getRaster();
				origin.setXY(raster.getPosition().getX() * scale_factor, raster.getPosition().getY() * scale_factor);
			}
			{
				// LayerElement element = new LayerElement();
				// output.children.addElement(element);
				// convert(scene, element, naming, result);

				for (int i = 0; i < scene.numberOfChildren(); i++) {
					final PSDLayer child = scene.getChild(i);
					Debug.checkNull("child", child);
					if (child == origin_layer) {
						continue;
					}
					final LayerElement element = new LayerElement();
					output.children.addElement(element);
					convert(child, element, naming, result, scale_factor);
					element.position_x = element.position_x - origin.getX();
					element.position_y = element.position_y - origin.getY();

				}
			}

			return;
		}

	}

	private static void packAnimationEvents (final PSDLayer events_list, final ActionsGroup e_list,
		final ChildAssetsNameResolver naming) {
		e_list.actions = new Vector<Action>();
		for (int i = 0; i < events_list.numberOfChildren(); i++) {
			final PSDLayer element = events_list.getChild(i);
			String event_id = readParameter(element, TAGS.ID);

			event_id = naming.childEvent(event_id).toString();

			final Action event = new Action();
			event.animation_id = event_id;
			event.is_start_animation = true;
			e_list.actions.addElement(event);

		}
	}

	private static long getTime (final String anchor_time_string) {
		final List<String> list = Collections.newList(anchor_time_string.split(":"));
		list.reverse();

		final long frame = Long.parseLong(list.getElementAt(0));
		final long second = Long.parseLong(list.getElementAt(1));
		long min = 0;
		if (list.size() > 2) {
			min = Long.parseLong(list.getElementAt(2));
		}
		final long ms = frame * 1000 / 30;

		return min * 60 * 1000 + second * 1000 + ms;
	}

	private static String readParameter (final String raw_value, final String prefix) {

		Debug.checkEmpty("raw_value", raw_value);
		Debug.checkEmpty("prefix", prefix);

		Debug.checkNull("raw_value", raw_value);
		Debug.checkNull("prefix", prefix);

		return raw_value.substring(prefix.length(), raw_value.length());
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

	private static void convertRaster (final PSDLayer input, final LayerElement output, final ChildAssetsNameResolver naming,
		final SceneStructurePackingResult result, final double scale_factor) {
		final PSDRasterPosition position = input.getRaster().getPosition();
		output.is_hidden = !input.isVisible();
		output.name = input.getName();

		if (output.name.startsWith("@")) {
			throw new Error("Bad layer name: " + output.name);
		}

		// if (input.getName().startsWith("area_touch1")) {
		// L.d();
		// }

		output.is_raster = true;
		output.blend_mode = RASTER_BLEND_MODE.valueOf(input.getMode().toString());
		output.position_x = position.getX() * scale_factor;
		output.position_y = position.getY() * scale_factor;
		output.width = position.getWidth() * scale_factor;
		output.height = position.getHeight() * scale_factor;
		final String raster_name = naming.getPSDLayerName(input).toString();
		output.raster_id = raster_name;
		result.addRequiredAsset(Names.newAssetID(output.raster_id), Collections.newList(input));
	}

}
