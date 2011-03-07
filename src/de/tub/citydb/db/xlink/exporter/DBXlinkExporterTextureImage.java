package de.tub.citydb.db.xlink.exporter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import oracle.jdbc.OracleResultSet;
import oracle.ord.im.OrdImage;
import de.tub.citydb.config.Config;
import de.tub.citydb.db.xlink.DBXlinkExternalFile;
import de.tub.citydb.event.statistic.TextureImageCounterEvent;
import de.tub.citydb.log.Logger;
import de.tub.citydb.util.Util;

public class DBXlinkExporterTextureImage implements DBXlinkExporter {
	private final Logger LOG = Logger.getInstance();
	
	private final DBXlinkExporterManager xlinkExporterManager;
	private final Config config;
	private final Connection connection;

	private PreparedStatement psTextureImage;
	private OracleResultSet rs;

	private String localPath;
	private String texturePath;
	private boolean texturePathIsLocal;
	private boolean overwriteTextureImage;
	private TextureImageCounterEvent counter;

	public DBXlinkExporterTextureImage(Connection connection, Config config, DBXlinkExporterManager xlinkExporterManager) throws SQLException {
		this.xlinkExporterManager = xlinkExporterManager;
		this.config = config;
		this.connection = connection;

		init();
	}

	private void init() throws SQLException {
		localPath = config.getInternal().getExportPath();
		texturePathIsLocal = config.getProject().getExporter().getAppearances().isTexturePathRealtive();
		texturePath = config.getInternal().getExportTextureFilePath();
		overwriteTextureImage = config.getProject().getExporter().getAppearances().isSetOverwriteTextureFiles();
		counter = new TextureImageCounterEvent(1);
		
		psTextureImage = connection.prepareStatement("select TEX_IMAGE from SURFACE_DATA where ID=?");
	}

	public boolean export(DBXlinkExternalFile xlink) throws SQLException {
		String fileName = xlink.getFileURI();
		boolean isRemote = false;

		if (fileName == null || fileName.length() == 0) {
			LOG.error("Database error while exporting a texture file: Attribute TEX_IMAGE_URI is empty.");
			return false;
		}

		// check whether we deal with a remote image uri
		if (Util.isRemoteXlink(fileName)) {
			URL url = null;
			isRemote = true;

			try {
				url = new URL(fileName);
			} catch (MalformedURLException e) {
				LOG.error("Error while exporting a texture file: " + fileName + " could not be interpreted.");
				return false;
			}

			if (url != null) {
				File file = new File(url.getFile());
				fileName = file.getName();
			}
		}

		// start export of texture to file
		// we do not overwrite an already existing file. so no need to
		// query the database in that case.
		String fileURI;
		if (texturePathIsLocal)
			fileURI = localPath + File.separator + texturePath + File.separator + fileName;
		else
			fileURI = texturePath + File.separator + fileName;

		File file = new File(fileURI);
		if (file.exists() && !overwriteTextureImage)
			return false;

		// try and read texture image attribute from surface_data table
		psTextureImage.setLong(1, xlink.getId());
		rs = (OracleResultSet)psTextureImage.executeQuery();

		if (!rs.next()) {
			if (!isRemote) {
				// we could not read from database. if we deal with a remote
				// image uri, we do not really care. but if the texture image should
				// be provided by us, then this is serious...
				LOG.error("Error while exporting a texture file: " + fileName + " does not exist in database.");
			}

			return false;
		}

		LOG.debug("Exporting texture file: " + fileName);

		xlinkExporterManager.propagateEvent(counter);
		
		// read oracle image data type
		OrdImage imgProxy = (OrdImage)rs.getORAData(1, OrdImage.getORADataFactory());
		if (imgProxy == null) {
			LOG.error("Database error while reading texture file: " + fileName);
			return false;
		}

		try {
			imgProxy.getDataInFile(fileURI);
		} catch (IOException ioEx) {
			LOG.error("Failed to write texture file " + fileName + ": " + ioEx.getMessage());
			return false;
		}

		imgProxy = null;
		return true;
	}

	@Override
	public DBXlinkExporterEnum getDBXlinkExporterType() {
		return DBXlinkExporterEnum.TEXTURE_IMAGE;
	}

}
