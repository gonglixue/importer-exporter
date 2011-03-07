package de.tub.citydb.db.xlink.importer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import de.tub.citydb.db.temp.DBTempGTT;
import de.tub.citydb.db.xlink.DBXlinkTextureAssociation;

public class DBXlinkImporterTextureAssociation implements DBXlinkImporter {
	private final DBTempGTT tempTable;
	private PreparedStatement psXlink;

	public DBXlinkImporterTextureAssociation(DBTempGTT tempTable) throws SQLException {
		this.tempTable = tempTable;

		init();
	}

	private void init() throws SQLException {
		psXlink = tempTable.getWriter().prepareStatement("insert into " + tempTable.getTableName() + 
			" (SURFACE_DATA_ID, SURFACE_GEOMETRY_ID, GMLID) values " +
			"(?, ?, ?)");
	}

	public boolean insert(DBXlinkTextureAssociation xlinkEntry) throws SQLException {
		psXlink.setLong(1, xlinkEntry.getSurfaceDataId());
		psXlink.setLong(2, xlinkEntry.getSurfaceGeometryId());
		psXlink.setString(3, xlinkEntry.getGmlId());

		psXlink.addBatch();

		return true;
	}

	@Override
	public void executeBatch() throws SQLException {
		psXlink.executeBatch();
	}

	@Override
	public DBXlinkImporterEnum getDBXlinkImporterType() {
		return DBXlinkImporterEnum.XLINK_TEXTUREASSOCIATION;
	}

}
