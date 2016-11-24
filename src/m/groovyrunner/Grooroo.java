package m.groovyrunner;

import android.content.Context;

import com.android.dx.Version;
import com.android.dx.dex.DexFormat;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.code.PositionList;
import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.dex.file.DexFile;
import com.android.dx.rop.type.TypeList;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import groovy.lang.GroovyClassLoader;

public final class Grooroo {

	public static final void loadFromFiles(Context context, String baseDir) throws Throwable {
		loadFromScript(context, combineScript(baseDir, new Listable() {
			public String[] list(String dir) throws Throwable {
				return new File(dir).list();
			}

			public boolean isFile(String path) throws Throwable {
				return new File(path).isFile();
			}

			public InputStream open(String path) throws Throwable {
				return new FileInputStream(path);
			}
		}));
	}

	public static final void loadFromAssets(final Context context, String baseDir) throws Throwable {
		loadFromScript(context, combineScript(baseDir, new Listable() {
			public String[] list(String dir) throws Throwable {
				return context.getAssets().list(dir);
			}

			public boolean isFile(String path) throws Throwable {
				try {
					InputStream is = context.getAssets().open(path);
					is.close();
					return true;
				} catch (Throwable t) {}
				return false;
			}

			public InputStream open(String path) throws Throwable {
				return context.getAssets().open(path);
			}
		}));
	}

	public static final void loadFromScript(Context context, String script) throws Throwable {
		HashMap<String, byte[]> classes = parseScript(context, script);
		ArrayList<String> classesNames = new ArrayList<String>();
		HashMap<String, String> superClasses = new HashMap<String, String>();
		HashMap<String, ArrayList<String>> interfaces = new HashMap<String, ArrayList<String>>();
		byte[] dalvikBytecode = transformClasses(classes, classesNames, superClasses, interfaces);
		File jarFile = dumpToFile(context, dalvikBytecode);
		loadDex(context, jarFile, classesNames, superClasses, interfaces);
	}

	private static final String combineScript(String baseDir, Listable lister) throws Throwable {
		HashMap<String, InputStream> files = openDir(baseDir, lister);
		HashSet<String> imports = new HashSet<String>();
		StringBuffer script = new StringBuffer();
		for (Map.Entry<String, InputStream> ent : files.entrySet()) {
			InputStream is = ent.getValue();
			InputStreamReader isr = new InputStreamReader(is, "utf-8");
			BufferedReader br = new BufferedReader(isr);
			String line = br.readLine();
			while (line != null) {
				String raw = line.trim();
				if (raw.startsWith("package ")) {
					raw = raw.substring(7).trim().replace(";", "");
					System.err.println("package \"" + raw + "\" has been ignored");
				} else if (raw.startsWith("import ")) {
					imports.add(line);
				} else {
					script.append(line).append('\n');
				}
				line = br.readLine();
			}
			br.close();
		}

		for (String i : imports) {
			String raw = i.substring(6).replace(";", "").trim();
			if (!raw.endsWith(".*")) {
				raw = raw.replace(".", "/");
				boolean found = false;
				for (Map.Entry<String, InputStream> ent : files.entrySet()) {
					if (ent.getKey().startsWith(raw)) {
						found = true;
						break;
					}
				}
				if (found) {
					continue;
				}
			}
			script.append(i).append('\n');
		}

		return script.toString();
	}

	private static final HashMap<String, InputStream> openDir(String dir, Listable lister) throws Throwable {
		HashMap<String, InputStream> paths = new HashMap<String, InputStream>();
		String[] names = lister.list(dir);
		if (names != null) {
			for (String name : names) {
				String path = dir.length() == 0 ? name : (dir + "/" + name);
				if ((path.endsWith(".groovy") || path.endsWith(".java")) && lister.isFile(path)) {
					InputStream is = lister.open(path);
					paths.put(path, is);
				} else {
					paths.putAll(openDir(path, lister));
				}
			}
		}
		return paths;
	}

	private static final HashMap<String, byte[]> parseScript(Context context, String script) throws Throwable {
		final HashMap<String, byte[]> classes = new HashMap<String, byte[]>();
		GroovyClassLoader loader = new GroovyClassLoader(context.getClassLoader()) {
			public Class loadClass(String name, boolean lookupScriptFiles, boolean preferClassOverScript, boolean resolve)
					throws ClassNotFoundException, CompilationFailedException {
				try {
					return super.loadClass(name, lookupScriptFiles, preferClassOverScript, resolve);
				} catch (Throwable t) {}
				return null;
			}

			protected ClassCollector createCollector(CompilationUnit unit, SourceUnit su) {
				return new ClassCollector(new InnerLoader(this), unit, su) {
					protected Class createClass(byte[] code, ClassNode classNode) {
						classes.put(classNode.getName() + ".class", code);
						return null;
					}
				};
			}
		};
		loader.parseClass(script);
		return classes;
	}

	private static final byte[] transformClasses(HashMap<String, byte[]> classes, ArrayList<String> classesNames,
			HashMap<String, String> superClasses, HashMap<String, ArrayList<String>> interfaces) throws Throwable {
		DexOptions dexOptions = new DexOptions();
		dexOptions.targetApiLevel = DexFormat.API_NO_EXTENDED_OPCODES;

		CfOptions cfOptions = new CfOptions();
		cfOptions.positionInfo = PositionList.LINES;
		cfOptions.localInfo = true;
		cfOptions.strictNameCheck = true;
		cfOptions.optimize = false;
		cfOptions.optimizeListFile = null;
		cfOptions.dontOptimizeListFile = null;
		cfOptions.statistics = false;

		DexFile dex = new DexFile(dexOptions);
		for (Map.Entry<String, byte[]> ent : classes.entrySet()) {
			String classPath = ent.getKey();
			ClassDefItem item = CfTranslator.translate(classPath, ent.getValue(), cfOptions, dexOptions);
			dex.add(item);

			String className = classPath.replace("/", ".");
			className = className.substring(0, className.length() - 6);
			classesNames.add(className);

			String superClass = item.getSuperclass().getDescriptor().getString().substring(1);
			superClass = superClass.substring(0, superClass.length() - 1);
			if (classes.containsKey(superClass + ".class")) {
				superClass = superClass.substring(0, superClass.length());
				superClasses.put(className, superClass.replace("/", "."));
			}

			TypeList its = item.getInterfaces();
			if (its.getWordCount() > 0) {
				ArrayList<String> list = new ArrayList<String>();
				for (int i = 0, size = its.getWordCount(); i < size; i++) {
					String name = its.getType(i).getClassName();
					if (classes.containsKey(name + ".class")) {
						list.add(name.replace("/", "."));
					}
				}
				interfaces.put(className, list);
			}
		}

		return dex.toDex(null, false);
	}

	private static final File dumpToFile(Context context, byte[] dalvikBytecode) throws Throwable {
		Manifest manifest = new Manifest();
		Attributes attribs = manifest.getMainAttributes();
		attribs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		attribs.put(new Attributes.Name("Created-By"), "dx " + Version.VERSION);
		attribs.putValue("Dex-Location", DexFormat.DEX_IN_JAR_NAME);

		File jarFile = new File(context.getFilesDir(), "Grooroo" + System.currentTimeMillis() + ".jar");
		FileOutputStream fos = new FileOutputStream(jarFile);
		JarOutputStream jar = new JarOutputStream(fos, manifest);
		JarEntry classes = new JarEntry(DexFormat.DEX_IN_JAR_NAME);
		classes.setSize(dalvikBytecode.length);
		jar.putNextEntry(classes);
		jar.write(dalvikBytecode);
		jar.closeEntry();
		jar.finish();
		fos.flush();
		fos.close();
		jar.close();
		return jarFile;
	}

	private static final void loadDex(Context context, File jarFile, ArrayList<String> classesNames,
			HashMap<String, String> superClasses, HashMap<String, ArrayList<String>> interfaces) throws Throwable {
		String jarPath = jarFile.getAbsolutePath();
		String dexPath = jarPath.substring(0, jarPath.length() - 4) + ".dex";
		dalvik.system.DexFile dexFile = dalvik.system.DexFile.loadDex(jarPath, dexPath, 0);
		while(classesNames.size() > 0) {
			String name = classesNames.remove(0);
			loadClass(context, name, classesNames, superClasses, interfaces, dexFile);
		}
		dexFile.close();
	}

	private static final void loadClass(Context context, String name, ArrayList<String> classes,
			HashMap<String, String> superClasses, HashMap<String, ArrayList<String>> interfaces,
			dalvik.system.DexFile dexFile) throws Throwable{
		String superClass = superClasses.get(name);
		if (superClass != null) {
			int index = classes.indexOf(superClass);
			if (index >= 0) {
				classes.remove(index);
				loadClass(context, superClass, classes, superClasses, interfaces, dexFile);
			}
		}

		ArrayList<String> interfaceNames = interfaces.get(name);
		if (interfaceNames != null) {
			for (String it : interfaceNames) {
				int index = classes.indexOf(it);
				if (index >= 0) {
					classes.remove(index);
					loadClass(context, it, classes, superClasses, interfaces, dexFile);
				}
			}
		}

		dexFile.loadClass(name, context.getClassLoader());
	}

	private static interface Listable {

		public String[] list(String dir) throws Throwable;

		public boolean isFile(String path) throws Throwable;

		public InputStream open(String path) throws Throwable;

	}

}
