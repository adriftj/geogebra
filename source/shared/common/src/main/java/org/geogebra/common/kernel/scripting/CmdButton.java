/*
 * GeoGebra - Dynamic Mathematics for Everyone
 * Copyright (c) GeoGebra GmbH, Altenbergerstr. 69, 4040 Linz, Austria
 * https://www.geogebra.org
 *
 * This file is licensed by GeoGebra GmbH under the EUPL 1.2 licence and
 * may be used under the EUPL 1.2 in compatible projects (see Article 5
 * and the Appendix of EUPL 1.2 for details).
 * You may obtain a copy of the licence at:
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Note: The overall GeoGebra software package is free to use for
 * non-commercial purposes only.
 * See https://www.geogebra.org/license for full licensing details
 */

package org.geogebra.common.kernel.scripting;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.arithmetic.Command;
import org.geogebra.common.kernel.commands.CommandProcessor;
import org.geogebra.common.kernel.commands.EvalInfo;
import org.geogebra.common.kernel.geos.GeoButton;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoImage;
import org.geogebra.common.kernel.geos.GeoText;
import org.geogebra.common.kernel.geos.GProperty;
import org.geogebra.common.kernel.geos.properties.FillType;
import org.geogebra.common.main.MyError;

/**
 * Button[], Button[caption], Button[caption, image]
 * 
 * @author Zbynek
 *
 */
public class CmdButton extends CommandProcessor {

	/**
	 * Create new command processor
	 * 
	 * @param kernel
	 *            kernel
	 */
	public CmdButton(Kernel kernel) {
		super(kernel);
	}

	/**
	 * Check if the given string is a builtin icon name.
	 * Builtin icon names contain only lowercase letters and underscores.
	 * 
	 * @param iconName icon name to check
	 * @return true if it's a builtin icon name
	 */
	private static boolean isBuiltinIconName(String iconName) {
		if (iconName == null || iconName.isEmpty())
			return false;
		for (int i = 0; i < iconName.length(); i++) {
			char c = iconName.charAt(i);
			if (!(c >= 'a' && c <= 'z' || c == '_'))
				return false;
		}
		return true;
	}

	@Override
	final public GeoElement[] process(Command c, EvalInfo info) throws MyError {
		int n = c.getArgumentNumber();
		GeoElement[] arg;

		switch (n) {
		case 1:
		case 2:
			arg = resArgs(c, info);
			if (arg[0].isGeoText()) {
				String caption = ((GeoText) arg[0]).getTextString();
				GeoButton gb = new GeoButton(cons);
				gb.setLabelVisible(true);
				gb.setCaption(caption);
				gb.setLabel(c.getLabel());
				if (n>=2) {
					// Set image if provided
					if (arg[1] instanceof GeoImage) {
						String fileName = arg[1].getImageFileName();
						if (fileName != null && !fileName.isEmpty()) {
							gb.setFillType(FillType.IMAGE);
							gb.setFillImage(fileName);
							gb.updateVisualStyleRepaint(GProperty.HATCHING);
						}
					} else if (arg[1] instanceof GeoText) {
						String imageName = ((GeoText) arg[1]).getTextString();
						// If it's a builtin icon name (only lowercase letters and underscores),
						// use setImageForFillable; otherwise treat as file path or URL
						if (isBuiltinIconName(imageName))
							app.getImageManager().setImageForFillable(kernel, (GeoText) arg[1], gb);
						else {
							// Treat as file path or URL
							gb.setFillType(FillType.IMAGE);
							gb.setFillImage(imageName);
							gb.updateVisualStyleRepaint(GProperty.HATCHING);
						}
					} else
						throw argErr(c, arg[1]);
				}
				return new GeoElement[] { gb };
			}
			throw argErr(c, arg[0]);
		case 0:
			GeoButton gb = new GeoButton(cons);
			gb.setLabelVisible(true);
			gb.setLabel(c.getLabel());
			return new GeoElement[] { gb };

		default:
			throw argNumErr(c);
		}

	}
}
