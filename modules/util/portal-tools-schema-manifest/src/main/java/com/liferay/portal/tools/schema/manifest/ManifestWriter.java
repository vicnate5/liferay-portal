/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.schema.manifest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManifestWriter {

	public static void write(
			File outputFile, String moduleName, List<UpgradeEdge> edges,
			Map<String, TableDef> tables)
		throws IOException {

		outputFile.getParentFile().mkdirs();

		StringBuilder sb = new StringBuilder();

		sb.append("{\n");
		sb.append("  \"module\": ").append(_q(moduleName)).append(",\n");
		sb.append("  \"generatedAt\": ").append(_q(LocalDate.now().toString())).append(",\n");

		// Transitions
		sb.append("  \"transitions\": [\n");

		for (int i = 0; i < edges.size(); i++) {
			UpgradeEdge edge = edges.get(i);

			sb.append("    {\n");
			sb.append("      \"fromVersion\": ").append(_q(edge.fromVersion)).append(",\n");
			sb.append("      \"toVersion\": ").append(_q(edge.toVersion)).append(",\n");
			sb.append("      \"changes\": [\n");

			for (int j = 0; j < edge.changes.size(); j++) {
				sb.append(_formatChange(edge.changes.get(j)));

				if (j < edge.changes.size() - 1) {
					sb.append(",");
				}

				sb.append("\n");
			}

			sb.append("      ]\n");
			sb.append("    }");

			if (i < edges.size() - 1) {
				sb.append(",");
			}

			sb.append("\n");
		}

		sb.append("  ],\n");

		// Snapshot
		sb.append("  \"snapshot\": {\n");
		sb.append("    \"tables\": {\n");

		String[] tableNames = tables.keySet().toArray(new String[0]);

		for (int i = 0; i < tableNames.length; i++) {
			sb.append(_formatTable(tableNames[i], tables.get(tableNames[i])));

			if (i < tableNames.length - 1) {
				sb.append(",");
			}

			sb.append("\n");
		}

		sb.append("    }\n");
		sb.append("  }\n");
		sb.append("}\n");

		Files.write(outputFile.toPath(), sb.toString().getBytes("UTF-8"));
	}

	private static String _formatChange(ChangeDescriptor c) {
		List<String[]> fields = new ArrayList<>();

		fields.add(new String[] {"op", c.op});

		if (c.table != null) {
			fields.add(new String[] {"table", c.table});
		}

		if (c.column != null) {
			fields.add(new String[] {"column", c.column});
		}

		if (c.toType != null) {
			fields.add(new String[] {"toType", c.toType});
		}

		if (c.derivation != null) {
			fields.add(new String[] {"derivation", c.derivation});
		}

		if (c.note != null) {
			fields.add(new String[] {"note", c.note});
		}

		StringBuilder sb = new StringBuilder("        {");

		for (int i = 0; i < fields.size(); i++) {
			sb.append(" ").append(_q(fields.get(i)[0])).append(": ").append(_q(fields.get(i)[1]));

			if (i < fields.size() - 1) {
				sb.append(",");
			}
		}

		sb.append(" }");

		return sb.toString();
	}

	private static String _formatTable(String name, TableDef table) {
		StringBuilder sb = new StringBuilder();

		sb.append("      ").append(_q(name)).append(": {\n");
		sb.append("        \"primaryKey\": [");

		boolean first = true;

		for (Column col : table.columns) {
			if (col.primaryKey) {
				if (!first) {
					sb.append(", ");
				}

				sb.append(_q(col.name));
				first = false;
			}
		}

		sb.append("],\n");
		sb.append("        \"columns\": [\n");

		List<Column> columns = table.columns;

		for (int i = 0; i < columns.size(); i++) {
			Column col = columns.get(i);

			sb.append("          {");
			sb.append(" \"name\": ").append(_q(col.name));
			sb.append(", \"type\": ").append(_q(col.type));
			sb.append(", \"nullable\": ").append(col.nullable);
			sb.append(", \"primaryKey\": ").append(col.primaryKey);
			sb.append(" }");

			if (i < columns.size() - 1) {
				sb.append(",");
			}

			sb.append("\n");
		}

		sb.append("        ]\n");
		sb.append("      }");

		return sb.toString();
	}

	private static String _q(String s) {
		StringBuilder sb = new StringBuilder("\"");

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (c == '"') {
				sb.append("\\\"");
			}
			else if (c == '\\') {
				sb.append("\\\\");
			}
			else if (c == '\n') {
				sb.append("\\n");
			}
			else if (c == '\r') {
				sb.append("\\r");
			}
			else if (c == '\t') {
				sb.append("\\t");
			}
			else {
				sb.append(c);
			}
		}

		sb.append('"');

		return sb.toString();
	}

}
