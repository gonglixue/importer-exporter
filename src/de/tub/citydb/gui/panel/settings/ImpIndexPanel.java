package de.tub.citydb.gui.panel.settings;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

import de.tub.citydb.config.Config;
import de.tub.citydb.config.project.importer.ImpIndex;
import de.tub.citydb.config.project.importer.ImpIndexMode;
import de.tub.citydb.gui.ImpExpGui;
import de.tub.citydb.gui.components.StatusDialog;
import de.tub.citydb.gui.util.GuiUtil;
import de.tub.citydb.log.Logger;
import de.tub.citydb.util.DBUtil;
import de.tub.citydb.util.DBUtil.DB_INDEX_TYPE;

public class ImpIndexPanel extends PrefPanelBase implements PropertyChangeListener {	
	private final ReentrantLock mainLock = new ReentrantLock();
	private final Logger LOG = Logger.getInstance();

	private JRadioButton impSIRadioDeacAc;
	private JRadioButton impSIRadioDeac;
	private JRadioButton impSIRadioNoDeac;
	private JRadioButton impNIRadioDeacAc;
	private JRadioButton impNIRadioDeac;
	private JRadioButton impNIRadioNoDeac;
	private JPanel block1;
	private JPanel block2;
	private JPanel block3;
	private JPanel block4;
	private JButton impSIDeactivate;
	private JButton impSIActivate;
	private JButton impNIDeactivate;
	private JButton impNIActivate;

	private ImpExpGui topFrame;

	public ImpIndexPanel(Config config, ImpExpGui topFrame) {
		super(config);
		this.topFrame = topFrame;
		initGui();

		config.getInternal().addPropertyChangeListener(this);
	}

	public boolean isModified() {
		if (super.isModified()) return true;

		ImpIndex index = config.getProject().getImporter().getIndexes();

		if (impSIRadioNoDeac.isSelected() != index.isSpatialIndexModeUnchanged()) return true;
		if (impSIRadioDeacAc.isSelected() != index.isSpatialIndexModeDeactivateActivate()) return true;
		if (impSIRadioDeac.isSelected() != index.isSpatialIndexModeDeactivate()) return true;

		if (impNIRadioNoDeac.isSelected() != index.isNormalIndexModeUnchanged()) return true;
		if (impNIRadioDeacAc.isSelected() != index.isNormalIndexModeDeactivateActivate()) return true;
		if (impNIRadioDeac.isSelected() != index.isNormalIndexModeDeactivate()) return true;

		return false;
	}

	public void initGui() {
		impSIRadioDeacAc = new JRadioButton("");
		impSIRadioDeac = new JRadioButton("");
		impSIRadioNoDeac = new JRadioButton("");
		ButtonGroup impSIRadio = new ButtonGroup();
		impSIRadio.add(impSIRadioNoDeac);
		impSIRadio.add(impSIRadioDeacAc);
		impSIRadio.add(impSIRadioDeac);
		impSIDeactivate = new JButton("");
		impSIActivate = new JButton("");
		impSIDeactivate.setEnabled(false);
		impSIActivate.setEnabled(false);

		impNIRadioDeacAc = new JRadioButton("");
		impNIRadioDeac = new JRadioButton("");
		impNIRadioNoDeac = new JRadioButton("");
		ButtonGroup impNIRadio = new ButtonGroup();
		impNIRadio.add(impNIRadioNoDeac);
		impNIRadio.add(impNIRadioDeacAc);
		impNIRadio.add(impNIRadioDeac);
		impNIDeactivate = new JButton("");
		impNIActivate = new JButton("");
		impNIDeactivate.setEnabled(false);
		impNIActivate.setEnabled(false);		

		//layout
		setLayout(new GridBagLayout());
		{
			block1 = new JPanel();
			add(block1, GuiUtil.setConstraints(0,0,1.0,0.0,GridBagConstraints.BOTH,5,0,5,0));
			block1.setBorder(BorderFactory.createTitledBorder(""));
			block1.setLayout(new GridBagLayout());
			impSIRadioNoDeac.setIconTextGap(10);
			impSIRadioDeacAc.setIconTextGap(10);
			impSIRadioDeac.setIconTextGap(10);
			{
				block1.add(impSIRadioNoDeac, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,0,5,0,5));
				block1.add(impSIRadioDeacAc, GuiUtil.setConstraints(0,1,1.0,1.0,GridBagConstraints.BOTH,0,5,0,5));
				block1.add(impSIRadioDeac, GuiUtil.setConstraints(0,2,1.0,1.0,GridBagConstraints.BOTH,0,5,0,5));
			}

			block2 = new JPanel();
			add(block2, GuiUtil.setConstraints(0,1,1.0,0.0,GridBagConstraints.BOTH,5,0,5,0));
			block2.setBorder(BorderFactory.createTitledBorder(""));
			block2.setLayout(new GridBagLayout());
			{
				block2.add(impSIDeactivate, GuiUtil.setConstraints(0,0,0.0,1.0,GridBagConstraints.BOTH,5,5,5,5));
				block2.add(impSIActivate, GuiUtil.setConstraints(1,0,0.0,1.0,GridBagConstraints.BOTH,5,5,5,5));
			}

			block3 = new JPanel();
			add(block3, GuiUtil.setConstraints(0,2,1.0,0.0,GridBagConstraints.BOTH,5,0,5,0));
			block3.setBorder(BorderFactory.createTitledBorder(""));
			block3.setLayout(new GridBagLayout());
			impNIRadioNoDeac.setIconTextGap(10);
			impNIRadioDeacAc.setIconTextGap(10);
			impNIRadioDeac.setIconTextGap(10);
			{
				block3.add(impNIRadioNoDeac, GuiUtil.setConstraints(0,0,1.0,1.0,GridBagConstraints.BOTH,0,5,0,5));
				block3.add(impNIRadioDeacAc, GuiUtil.setConstraints(0,1,1.0,1.0,GridBagConstraints.BOTH,0,5,0,5));
				block3.add(impNIRadioDeac, GuiUtil.setConstraints(0,2,1.0,1.0,GridBagConstraints.BOTH,0,5,0,5));
			}

			block4 = new JPanel();
			add(block4, GuiUtil.setConstraints(0,3,1.0,0.0,GridBagConstraints.BOTH,5,0,5,0));
			block4.setBorder(BorderFactory.createTitledBorder(""));
			block4.setLayout(new GridBagLayout());
			{
				block4.add(impNIDeactivate, GuiUtil.setConstraints(0,0,0.0,1.0,GridBagConstraints.BOTH,5,5,5,5));
				block4.add(impNIActivate, GuiUtil.setConstraints(1,0,0.0,1.0,GridBagConstraints.BOTH,5,5,5,5));
			}
		}	

		impSIDeactivate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						dropIndex(DB_INDEX_TYPE.SPATIAL);
					}
				};
				thread.start();
			}
		});

		impNIDeactivate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						dropIndex(DB_INDEX_TYPE.NORMAL);
					}
				};
				thread.start();
			}
		});

		impSIActivate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						createIndex(DB_INDEX_TYPE.SPATIAL);
					}
				};
				thread.start();
			}
		});

		impNIActivate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread() {
					public void run() {
						createIndex(DB_INDEX_TYPE.NORMAL);
					}
				};
				thread.start();
			}
		});
	}

	public void doTranslation() {
		block1.setBorder(BorderFactory.createTitledBorder(ImpExpGui.labels.getString("pref.import.index.spatial.border.handling")));
		impSIRadioDeacAc.setText(ImpExpGui.labels.getString("pref.import.index.spatial.label.autoActivate"));
		impSIRadioDeac.setText(ImpExpGui.labels.getString("pref.import.index.spatial.label.manuActivate"));
		impSIRadioNoDeac.setText(ImpExpGui.labels.getString("pref.import.index.spatial.label.keepState"));

		block2.setBorder(BorderFactory.createTitledBorder(ImpExpGui.labels.getString("pref.import.index.spatial.border.manual")));
		impSIDeactivate.setText(ImpExpGui.labels.getString("pref.import.index.spatial.button.deactivate"));
		impSIActivate.setText(ImpExpGui.labels.getString("pref.import.index.spatial.button.activate"));

		block3.setBorder(BorderFactory.createTitledBorder(ImpExpGui.labels.getString("pref.import.index.normal.border.handling")));
		impNIRadioDeacAc.setText(ImpExpGui.labels.getString("pref.import.index.normal.label.autoActivate"));
		impNIRadioDeac.setText(ImpExpGui.labels.getString("pref.import.index.normal.label.manuActivate"));
		impNIRadioNoDeac.setText(ImpExpGui.labels.getString("pref.import.index.normal.label.keepState"));

		block4.setBorder(BorderFactory.createTitledBorder(ImpExpGui.labels.getString("pref.import.index.normal.border.manual")));
		impNIDeactivate.setText(ImpExpGui.labels.getString("pref.import.index.normal.button.deactivate"));
		impNIActivate.setText(ImpExpGui.labels.getString("pref.import.index.normal.button.activate"));
	}

	private void dropIndex(DB_INDEX_TYPE type) {
		final ReentrantLock lock = this.mainLock;
		lock.lock();

		try {
			String statusTextKey, logStartMsg, statusTitle, statusHeader, logSuccess, logFail;

			if (type == DB_INDEX_TYPE.SPATIAL) {
				statusTextKey =	"main.status.database.deactivate.spatial.label";
				logStartMsg = "Deactivating spatial indexes...";
				statusTitle = ImpExpGui.labels.getString("pref.import.index.spatial.dialog.title");
				statusHeader = ImpExpGui.labels.getString("pref.import.index.spatial.dialog.deactivate");
				logSuccess = "Deactivating spatial indexes successfully finished.";
				logFail = "Deactivating spatial indexes aborted.";
			} else {
				statusTextKey = "main.status.database.deactivate.normal.label";
				logStartMsg = "Deactivating normal indexes...";
				statusTitle = ImpExpGui.labels.getString("pref.import.index.normal.dialog.title");
				statusHeader = ImpExpGui.labels.getString("pref.import.index.normal.dialog.deactivate");
				logSuccess = "Deactivating normal indexes successfully finished.";
				logFail = "Deactivating normal indexes aborted.";
			}			

			String statusMsg = ImpExpGui.labels.getString("pref.import.index.deactivate.detail");

			topFrame.getConsoleText().setText("");
			topFrame.getStatusText().setText(ImpExpGui.labels.getString(statusTextKey));

			LOG.info(logStartMsg);
			final DBUtil dbUtil;
			try {
				dbUtil = new DBUtil(topFrame.getDBPool());
			} catch (SQLException sqlEx) {
				String text = ImpExpGui.labels.getString("pref.import.index.deactivate.error");
				Object[] args = new Object[]{ sqlEx.getMessage() };
				String result = MessageFormat.format(text, args);

				topFrame.errorMessage(ImpExpGui.labels.getString("common.dialog.error.db.title"), result);
				LOG.error("Failed to deactivate indexes: " + sqlEx.getMessage().trim());
				return;
			}

			final StatusDialog reportDialog = new StatusDialog(topFrame, statusTitle, statusHeader, statusMsg, false);	
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					reportDialog.pack();
					reportDialog.setLocationRelativeTo(getTopLevelAncestor());
					reportDialog.setVisible(true);
				}
			});

			String[] report = null;
			String dbSqlEx = null;
			try {
				if (type == DB_INDEX_TYPE.SPATIAL)
					report = dbUtil.dropSpatialIndexes();
				else 
					report = dbUtil.dropNormalIndexes();

				if (report != null) {

					for (String line : report) {
						String[] parts = line.split(":");

						if (!parts[4].equals("DROPPED")) {
							LOG.error("FAILED: " + parts[0] + " on " + parts[1] + "(" + parts[2] + ")");
							String errMsg = dbUtil.errorMessage(parts[3]);
							LOG.error("Error cause: " + errMsg);
						} else
							LOG.info("SUCCESS: " + parts[0] + " on " + parts[1] + "(" + parts[2] + ")");
					}
				}

			} catch (SQLException sqlEx) {
				dbSqlEx = sqlEx.getMessage();
			} finally {		
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						reportDialog.dispose();
					}
				});

				if (report != null)  {
					LOG.info(logSuccess);
				} else {

					if (dbSqlEx == null) {
						LOG.warn(logFail);
					} else  {
						String text = ImpExpGui.labels.getString("pref.import.index.deactivate.error");
						Object[] args = new Object[]{ dbSqlEx };
						String result = MessageFormat.format(text, args);

						topFrame.errorMessage(ImpExpGui.labels.getString("common.dialog.error.db.title"), result);
						LOG.error("Failed to deactivate indexes: " + dbSqlEx.trim());
					}
				}
			}

			topFrame.getStatusText().setText(ImpExpGui.labels.getString("main.status.ready.label"));
		} finally {
			lock.unlock();
		}
	}

	private void createIndex(DB_INDEX_TYPE type) {
		final ReentrantLock lock = this.mainLock;
		lock.lock();

		try {
			String statusTextKey, logStartMsg, statusTitle, statusHeader, logSuccess, logFail;

			if (type == DB_INDEX_TYPE.SPATIAL) {
				statusTextKey =	"main.status.database.activate.spatial.label";
				logStartMsg = "Activating spatial indexes...";
				statusTitle = ImpExpGui.labels.getString("pref.import.index.spatial.dialog.title");
				statusHeader = ImpExpGui.labels.getString("pref.import.index.spatial.dialog.activate");
				logSuccess = "Activating spatial indexes successfully finished.";
				logFail = "Activating spatial indexes aborted.";
			} else {
				statusTextKey = "main.status.database.activate.normal.label";
				logStartMsg = "Activating normal indexes...";
				statusTitle = ImpExpGui.labels.getString("pref.import.index.normal.dialog.title");
				statusHeader = ImpExpGui.labels.getString("pref.import.index.normal.dialog.activate");
				logSuccess = "Activating normal indexes successfully finished.";
				logFail = "Activating normal indexes aborted.";
			}			

			String statusMsg = ImpExpGui.labels.getString("pref.import.index.activate.detail");

			topFrame.getConsoleText().setText("");
			topFrame.getStatusText().setText(ImpExpGui.labels.getString(statusTextKey));

			LOG.info(logStartMsg);
			final DBUtil dbUtil;
			try {
				dbUtil = new DBUtil(topFrame.getDBPool());
			} catch (SQLException sqlEx) {
				String text = ImpExpGui.labels.getString("pref.import.index.activate.error");
				Object[] args = new Object[]{ sqlEx.getMessage() };
				String result = MessageFormat.format(text, args);

				topFrame.errorMessage(ImpExpGui.labels.getString("common.dialog.error.db.title"), result);
				LOG.error("Failed to activate indexes: " + sqlEx.getMessage().trim());
				return;
			}

			final StatusDialog reportDialog = new StatusDialog(topFrame, statusTitle, statusHeader, statusMsg, false);			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					reportDialog.pack();
					reportDialog.setLocationRelativeTo(getTopLevelAncestor());
					reportDialog.setVisible(true);
				}
			});

			String[] report = null;
			String dbSqlEx = null;
			try {
				if (type == DB_INDEX_TYPE.SPATIAL)
					report = dbUtil.createSpatialIndexes();
				else 
					report = dbUtil.createNormalIndexes();

				if (report != null) {				
					for (String line : report) {				
						String[] parts = line.split(":");

						if (!parts[4].equals("VALID")) {
							LOG.error("FAILED: " + parts[0] + " on " + parts[1] + "(" + parts[2] + ")");
							String errMsg = dbUtil.errorMessage(parts[3]);
							LOG.error("Error cause: " + errMsg);
						} else
							LOG.info("SUCCESS: " + parts[0] + " on " + parts[1] + "(" + parts[2] + ")");
					}
				}

			} catch (SQLException sqlEx) {
				dbSqlEx = sqlEx.getMessage();
			} finally {	
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						reportDialog.dispose();
					}
				});

				if (report != null)  {
					LOG.info(logSuccess);
				} else {

					if (dbSqlEx == null) {
						LOG.warn(logFail);
					} else  {
						String text = ImpExpGui.labels.getString("pref.import.index.activate.error");
						Object[] args = new Object[]{ dbSqlEx };
						String result = MessageFormat.format(text, args);

						topFrame.errorMessage(ImpExpGui.labels.getString("common.dialog.error.db.title"), result);
						LOG.error("Failed to activate indexes: " + dbSqlEx.trim());
					}
				}
			}

			topFrame.getStatusText().setText(ImpExpGui.labels.getString("main.status.ready.label"));
		} finally {
			lock.unlock();
		}
	}

	//config
	public void loadSettings() {
		ImpIndex index = config.getProject().getImporter().getIndexes();

		if (index.isSpatialIndexModeUnchanged())
			impSIRadioNoDeac.setSelected(true);
		else if (index.isSpatialIndexModeDeactivateActivate())
			impSIRadioDeacAc.setSelected(true);
		else
			impSIRadioDeac.setSelected(true);

		if (index.isNormalIndexModeUnchanged())
			impNIRadioNoDeac.setSelected(true);
		else if (index.isNormalIndexModeDeactivateActivate())
			impNIRadioDeacAc.setSelected(true);
		else
			impNIRadioDeac.setSelected(true);
	}

	public void setSettings() {
		ImpIndex index = config.getProject().getImporter().getIndexes();

		if (impSIRadioNoDeac.isSelected())
			index.setSpatial(ImpIndexMode.UNCHANGED);
		else if (impSIRadioDeacAc.isSelected())
			index.setSpatial(ImpIndexMode.DEACTIVATE_ACTIVATE);
		else
			index.setSpatial(ImpIndexMode.DEACTIVATE);

		if (impNIRadioNoDeac.isSelected())
			index.setNormal(ImpIndexMode.UNCHANGED);
		else if (impNIRadioDeacAc.isSelected())
			index.setNormal(ImpIndexMode.DEACTIVATE_ACTIVATE);
		else
			index.setNormal(ImpIndexMode.DEACTIVATE);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("internal.dbIsConnected")) {
			boolean status = (Boolean)evt.getNewValue();

			impSIActivate.setEnabled(status);
			impSIDeactivate.setEnabled(status);
			impNIActivate.setEnabled(status);
			impNIDeactivate.setEnabled(status);
		}
	}
}
