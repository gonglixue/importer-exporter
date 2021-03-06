/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 * 
 * Copyright 2013 - 2016
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.gis.bgu.tum.de/
 * 
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 * 
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Taufkirchen <http://www.moss.de/>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.citydb.modules.citygml.common.database.cache.model;

import org.citydb.database.adapter.AbstractSQLAdapter;

public class CacheTableBasic extends CacheTableModel {
	public static CacheTableBasic instance = null;

	private CacheTableBasic() {		
	}

	public synchronized static CacheTableBasic getInstance() {
		if (instance == null)
			instance = new CacheTableBasic();

		return instance;
	}

	@Override
	public CacheTableModelEnum getType() {
		return CacheTableModelEnum.BASIC;
	}

	@Override
	protected String getColumns(AbstractSQLAdapter sqlAdapter) {
		StringBuilder builder = new StringBuilder("(")
		.append("ID ").append(sqlAdapter.getInteger()).append(", ")
		.append("FROM_TABLE ").append(sqlAdapter.getNumeric(3)).append(", ")
		.append("GMLID ").append(sqlAdapter.getCharacterVarying(256)).append(", ")
		.append("TO_TABLE ").append(sqlAdapter.getNumeric(3)).append(", ")
		.append("ATTRNAME ").append(sqlAdapter.getCharacterVarying(30))
		.append(")");
		
		return builder.toString();
	}

}
