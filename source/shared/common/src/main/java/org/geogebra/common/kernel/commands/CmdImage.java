package org.geogebra.common.kernel.commands;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.arithmetic.Command;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoImage;
import org.geogebra.common.kernel.geos.GeoText;
import org.geogebra.common.main.MyError;

/**
 * Image[ &lt;URL&gt; ]
 * 
 * Creates a GeoImage object from a URL or filename.
 * The URL will be saved in the file element's name attribute.
 */
public class CmdImage extends CommandProcessor {
	/**
	 * Create new command processor
	 * 
	 * @param kernel
	 *            kernel
	 */
	public CmdImage(Kernel kernel) {
		super(kernel);
	}

	@Override
	public GeoElement[] process(Command c, EvalInfo info) throws MyError {
		int n = c.getArgumentNumber();
		boolean[] ok = new boolean[n];
		GeoElement[] arg;
		arg = resArgs(c, info);

		switch (n) {
		case 1:
			ok[0] = arg[0].isGeoText();
			if (ok[0]) {
				GeoText urlText = (GeoText) arg[0];
				String url = urlText.getTextString();
				
				// Create GeoImage and set the URL as filename
				GeoImage geoImage = new GeoImage(cons);
				geoImage.setImageFileName(url);
				geoImage.setLabel(c.getLabel());
				
				GeoElement[] ret = { geoImage };
				return ret;
			}
			throw argErr(c, arg[0]);

		default:
			throw argNumErr(c);
		}
	}
}

