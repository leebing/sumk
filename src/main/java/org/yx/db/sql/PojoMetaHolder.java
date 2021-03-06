/**
 * Copyright (C) 2016 - 2017 youtongluan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.yx.db.sql;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.yx.db.annotation.Column;
import org.yx.db.annotation.Table;
import org.yx.exception.SumkException;
import org.yx.log.Log;

public class PojoMetaHolder {

	private static Map<Class<?>, PojoMeta> pojoMetas = new ConcurrentHashMap<>();
	private static Map<String, PojoMeta> tableMetas = new ConcurrentHashMap<>();

	public static PojoMeta getTableMeta(String table) {
		return tableMetas.get(table);
	}

	public static PojoMeta getPojoMeta(Class<?> clz) {
		if (clz == null || clz.isInterface() || clz == Object.class) {
			return null;
		}
		Class<?> tmp = clz;
		while (tmp != Object.class) {
			PojoMeta m = pojoMetas.get(tmp);
			if (m != null) {
				return m;
			}
			tmp = tmp.getSuperclass();
		}
		return null;
	}

	public static void resolve(Class<?> pojoClz) {
		Table table = pojoClz.getAnnotation(Table.class);
		if (table == null) {
			return;
		}
		Map<String, Field> map = new HashMap<>();
		Class<?> clz = pojoClz;
		while (clz != Object.class) {
			Field[] fields = clz.getDeclaredFields();
			for (Field f : fields) {
				map.putIfAbsent(f.getName(), f);
			}
			clz = clz.getSuperclass();
		}
		List<ColumnMeta> list = new LinkedList<>();
		Collection<Field> set = map.values();
		for (Field f : set) {
			Column c = f.getAnnotation(Column.class);
			f.setAccessible(true);
			list.add(new ColumnMeta(f, c));
		}
		if (list.isEmpty()) {
			Log.get("pojo").debug("{}'s column is empty", pojoClz.getName());
			return;
		}
		Collections.sort(list);
		PojoMeta tm = new PojoMeta(table, list.toArray(new ColumnMeta[0]), pojoClz);
		if (tm.getPrimaryIDs().length == 0) {
			SumkException.throwException(56456456, pojoClz.getName() + " has no primary key");
		}
		pojoMetas.put(pojoClz, tm);
		tableMetas.put(tm.getTableName(), tm);
	}
}
