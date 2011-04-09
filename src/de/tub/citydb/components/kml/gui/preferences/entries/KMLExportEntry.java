package de.tub.citydb.components.kml.gui.preferences.entries;

import de.tub.citydb.config.internal.Internal;
import de.tub.citydb.gui.preferences.DefaultPreferencesEntry;
import de.tub.citydb.gui.preferences.NullComponent;

public class KMLExportEntry extends DefaultPreferencesEntry {

	public KMLExportEntry() {
		super(NullComponent.getInstance());
	}
	
	@Override
	public String getLocalizedTitle() {
		return Internal.I18N.getString("pref.tree.kmlExport");
	}

}
