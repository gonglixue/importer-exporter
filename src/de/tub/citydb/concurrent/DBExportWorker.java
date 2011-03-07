package de.tub.citydb.concurrent;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import oracle.jdbc.driver.OracleConnection;

import org.citygml4j.factory.CityGMLFactory;

import de.tub.citydb.concurrent.WorkerPool.WorkQueue;
import de.tub.citydb.config.Config;
import de.tub.citydb.config.project.database.Database;
import de.tub.citydb.db.DBConnectionPool;
import de.tub.citydb.db.cache.DBGmlIdLookupServerManager;
import de.tub.citydb.db.exporter.DBAppearance;
import de.tub.citydb.db.exporter.DBBuilding;
import de.tub.citydb.db.exporter.DBCityFurniture;
import de.tub.citydb.db.exporter.DBCityObjectGroup;
import de.tub.citydb.db.exporter.DBExporterEnum;
import de.tub.citydb.db.exporter.DBExporterManager;
import de.tub.citydb.db.exporter.DBGenericCityObject;
import de.tub.citydb.db.exporter.DBLandUse;
import de.tub.citydb.db.exporter.DBPlantCover;
import de.tub.citydb.db.exporter.DBReliefFeature;
import de.tub.citydb.db.exporter.DBSolitaryVegetatObject;
import de.tub.citydb.db.exporter.DBSplittingResult;
import de.tub.citydb.db.exporter.DBTransportationComplex;
import de.tub.citydb.db.exporter.DBWaterBody;
import de.tub.citydb.db.xlink.DBXlink;
import de.tub.citydb.event.EventDispatcher;
import de.tub.citydb.event.statistic.FeatureCounterEvent;
import de.tub.citydb.event.statistic.GeometryCounterEvent;
import de.tub.citydb.event.statistic.TopLevelFeatureCounterEvent;
import de.tub.citydb.filter.ExportFilter;
import de.tub.citydb.log.Logger;
import de.tub.citydb.sax.events.SAXEvent;

public class DBExportWorker implements Worker<DBSplittingResult> {
	private final Logger LOG = Logger.getInstance();
	
	// instance members needed for WorkPool
	private volatile boolean shouldRun = true;
	private ReentrantLock runLock = new ReentrantLock();
	private WorkQueue<DBSplittingResult> workQueue = null;
	private DBSplittingResult firstWork;
	private Thread workerThread = null;

	// instance members needed to do work
	private final JAXBContext jaxbContext;
	private final DBConnectionPool dbConnectionPool;
	private final WorkerPool<Vector<SAXEvent>> ioWriterPool;
	private final WorkerPool<DBXlink> xlinkExporterPool;
	private final DBGmlIdLookupServerManager lookupServerManager;
	private final CityGMLFactory cityGMLFactory;
	private final ExportFilter exportFilter;
	private final Config config;
	private Connection connection;	
	private DBExporterManager dbExporterManager;
	private final EventDispatcher eventDispatcher;
	private int exportCounter = 0;

	public DBExportWorker(JAXBContext jaxbContext,
			DBConnectionPool dbConnectionPool,
			WorkerPool<Vector<SAXEvent>> ioWriterPool,
			WorkerPool<DBXlink> xlinkExporterPool,
			DBGmlIdLookupServerManager lookupServerManager,
			CityGMLFactory cityGMLFactory,
			ExportFilter exportFilter,
			Config config,
			EventDispatcher eventDispatcher) throws SQLException {
		this.jaxbContext = jaxbContext;
		this.dbConnectionPool = dbConnectionPool;
		this.ioWriterPool = ioWriterPool;
		this.xlinkExporterPool = xlinkExporterPool;
		this.lookupServerManager = lookupServerManager;
		this.cityGMLFactory = cityGMLFactory;
		this.exportFilter = exportFilter;
		this.config = config;
		this.eventDispatcher = eventDispatcher;
		init();
	}

	private void init() throws SQLException {
		connection = dbConnectionPool.getConnection();
		connection.setAutoCommit(false);
		((OracleConnection)connection).setDefaultRowPrefetch(50);

		// try and change workspace for both connections if needed
		Database database = config.getProject().getDatabase();
		dbConnectionPool.changeWorkspace(
				connection, 
				database.getWorkspace().getExportWorkspace(), 
				database.getWorkspace().getExportDate());

		dbExporterManager = new DBExporterManager(
				jaxbContext,
				connection,
				ioWriterPool,
				xlinkExporterPool,
				lookupServerManager,
				cityGMLFactory,
				exportFilter,
				config,
				eventDispatcher);
	}

	@Override
	public Thread getThread() {
		return workerThread;
	}

	@Override
	public void interrupt() {
		shouldRun = false;
		workerThread.interrupt();
	}

	@Override
	public void interruptIfIdle() {
		final ReentrantLock runLock = this.runLock;
		shouldRun = false;

		if (runLock.tryLock()) {
			try {
				workerThread.interrupt();
			} finally {
				runLock.unlock();
			}
		}
	}

	@Override
	public void setFirstWork(DBSplittingResult firstWork) {
		this.firstWork = firstWork;
	}

	@Override
	public void setThread(Thread workerThread) {
		this.workerThread = workerThread;
	}

	@Override
	public void setWorkQueue(WorkQueue<DBSplittingResult> workQueue) {
		this.workQueue = workQueue;
	}

	@Override
	public void run() {
		if (firstWork != null && shouldRun) {
			doWork(firstWork);
			firstWork = null;
		}

		while (shouldRun) {
			try {
				DBSplittingResult work = workQueue.take();
				doWork(work);
			} catch (InterruptedException ie) {
				// re-check state
			}
		}

		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				//
			}

			connection = null;
		}

		// propagate the number of features  and geometries this worker
		// did work on...
		eventDispatcher.triggerEvent(new TopLevelFeatureCounterEvent(exportCounter));
		eventDispatcher.triggerEvent(new FeatureCounterEvent(dbExporterManager.getFeatureCounter()));
		eventDispatcher.triggerEvent(new GeometryCounterEvent(dbExporterManager.getGeometryCounter()));
	}

	private void doWork(DBSplittingResult work) {
		final ReentrantLock runLock = this.runLock;
		runLock.lock();

		try {
			try {
				boolean success = false;

				if (work.isCheckIfAlreadyExported())
					if (dbExporterManager.getGmlId(work.getPrimaryKey(), work.getCityObjectType()) != null)
						return;						

				switch (work.getCityObjectType()) {
				case BUILDING:
					DBBuilding dbBuilding = (DBBuilding)dbExporterManager.getDBExporter(DBExporterEnum.BUILDING);
					if (dbBuilding != null)
						success = dbBuilding.read(work);
					break;
				case CITYFURNITURE:
					DBCityFurniture dbCityFurniture = (DBCityFurniture)dbExporterManager.getDBExporter(DBExporterEnum.CITY_FURNITURE);
					if (dbCityFurniture != null)
						success = dbCityFurniture.read(work);
					break;
				case LANDUSE:
					DBLandUse dbLandUse = (DBLandUse)dbExporterManager.getDBExporter(DBExporterEnum.LAND_USE);
					if (dbLandUse != null)
						success = dbLandUse.read(work);
					break;
				case WATERBODY:
					DBWaterBody dbWaterBody = (DBWaterBody)dbExporterManager.getDBExporter(DBExporterEnum.WATERBODY);
					if (dbWaterBody != null)
						success = dbWaterBody.read(work);
					break;
				case PLANTCOVER:
					DBPlantCover dbPlantCover = (DBPlantCover)dbExporterManager.getDBExporter(DBExporterEnum.PLANT_COVER);
					if (dbPlantCover != null)
						success = dbPlantCover.read(work);
					break;
				case SOLITARYVEGETATIONOBJECT:
					DBSolitaryVegetatObject dbSolVegObject = (DBSolitaryVegetatObject)dbExporterManager.getDBExporter(DBExporterEnum.SOLITARY_VEGETAT_OBJECT);
					if (dbSolVegObject != null)
						success = dbSolVegObject.read(work);
					break;
				case TRANSPORTATIONCOMPLEX:
				case TRACK:
				case RAILWAY:
				case ROAD:
				case SQUARE:
					DBTransportationComplex dbTransComplex = (DBTransportationComplex)dbExporterManager.getDBExporter(DBExporterEnum.TRANSPORTATION_COMPLEX);
					if (dbTransComplex != null)
						success = dbTransComplex.read(work);
					break;
				case RELIEFFEATURE:
					DBReliefFeature dbReliefFeature = (DBReliefFeature)dbExporterManager.getDBExporter(DBExporterEnum.RELIEF_FEATURE);
					if (dbReliefFeature != null)
						success = dbReliefFeature.read(work);
					break;
				case APPEARANCE:
					// we are working on global appearances here
					DBAppearance dbAppearance = (DBAppearance)dbExporterManager.getDBExporter(DBExporterEnum.APPEARANCE);
					if (dbAppearance != null)
						success = dbAppearance.read(work);
					break;
				case GENERICCITYOBJECT:
					DBGenericCityObject dbGenericCityObject = (DBGenericCityObject)dbExporterManager.getDBExporter(DBExporterEnum.GENERIC_CITYOBJECT);
					if (dbGenericCityObject != null)
						success = dbGenericCityObject.read(work);
					break;
				case CITYOBJECTGROUP:
					DBCityObjectGroup dbCityObjectGroup = (DBCityObjectGroup)dbExporterManager.getDBExporter(DBExporterEnum.CITYOBJECTGROUP);
					if (dbCityObjectGroup != null)
						success = dbCityObjectGroup.read(work);
					break;
				}

				if (success)
					++exportCounter;

			} catch (SQLException sqlEx) {
				LOG.error("SQL error while querying city object: " + sqlEx.getMessage());
				return;
			} catch (JAXBException jaxbEx) {
				return;
			}

			if (exportCounter == 20) {
				eventDispatcher.triggerEvent(new TopLevelFeatureCounterEvent(exportCounter));
				exportCounter = 0;
			}

		} finally {
			runLock.unlock();
		}
	}

}
