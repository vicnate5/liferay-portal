/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.tools.schema.manifest;

import com.liferay.petra.string.StringBundler;

import java.io.File;

import java.nio.file.Files;

import java.util.List;
import java.util.Map;

/**
 * @author Victor Ware
 */
public class SchemaManifestBuilder {

	public static void main(String[] args) throws Exception {
		String moduleDirPath = null;

		for (String arg : args) {
			if (arg.startsWith("--module-dir=")) {
				moduleDirPath = arg.substring("--module-dir=".length());
			}
		}

		if (moduleDirPath == null) {
			System.err.println("Usage: --module-dir=<path-to-module>");
			System.exit(1);
		}

		File moduleDir = new File(
			moduleDirPath
		).getCanonicalFile();

		if (!moduleDir.isDirectory()) {
			System.err.println("Not a directory: " + moduleDir);
			System.exit(1);
		}

		File tablesFile = new File(
			moduleDir, "src/main/resources/META-INF/sql/tables.sql");

		if (!tablesFile.exists()) {
			System.err.println("tables.sql not found: " + tablesFile);
			System.exit(1);
		}

		System.out.println("Parsing " + tablesFile);

		Map<String, TableDef> tables = SqlParser.parse(tablesFile);

		System.out.println(
			StringBundler.concat(
				"Found ", tables.size(), " tables: ", tables.keySet()));

		File registratorFile = _findRegistratorFile(moduleDir);

		if (registratorFile == null) {
			System.err.println(
				"No *UpgradeStepRegistrator.java found under " + moduleDir);
			System.exit(1);
		}

		System.out.println("Parsing " + registratorFile);

		List<UpgradeEdge> edges = RegistratorParser.parse(registratorFile);

		System.out.println("Found " + edges.size() + " upgrade transitions");

		String moduleName = _bundleSymbolicName(moduleDir);

		System.out.println("Module: " + moduleName);

		File outputFile = new File(
			moduleDir, "src/main/resources/META-INF/schema/manifest.json");

		ManifestWriter.write(outputFile, moduleName, edges, tables);

		System.out.println("Wrote manifest: " + outputFile);
	}

	private static String _bundleSymbolicName(File moduleDir) throws Exception {
		File bndFile = new File(moduleDir, "bnd.bnd");

		if (!bndFile.exists()) {
			return moduleDir.getName();
		}

		for (String line : Files.readAllLines(bndFile.toPath())) {
			if (line.startsWith("Bundle-SymbolicName:")) {
				return line.substring(
					"Bundle-SymbolicName:".length()
				).trim();
			}
		}

		return moduleDir.getName();
	}

	private static File _findRegistratorFile(File moduleDir) throws Exception {
		File srcDir = new File(moduleDir, "src/main/java");

		if (!srcDir.isDirectory()) {
			return null;
		}

		return Files.walk(
			srcDir.toPath()
		).filter(
			p -> p.getFileName(
			).toString(
			).endsWith(
				"UpgradeStepRegistrator.java"
			)
		).findFirst(
		).map(
			p -> p.toFile()
		).orElse(
			null
		);
	}

}