import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * 静态校验 mixin 注入点是否仍然存在于指定版本的 Minecraft 命名 jar 中。
 *
 * 用法: java -cp <asm-jars> MixinAudit.java <minecraft-named.jar> <mod.jar> [mixinPackagePrefix]
 *
 * 校验内容:
 *   1. @Mixin 的目标类存在(含内部类 A$B)
 *   2. @Inject/@Redirect/@Overwrite/@Shadow 方法选择器 name(desc)ret 能在目标类(含父类/接口)中解析
 *   3. @Shadow 字段存在
 *   4. @Redirect/@Inject(at=INVOKE/FIELD/NEW) 指定的调用点在目标方法字节码中真实存在(含 ordinal 计数)
 *
 * 目的: 编译期不会校验注入点,版本升级后 mixin 会在运行时才炸。本工具让升级可验证。
 */
public final class MixinAudit {
    private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String INJECT_DESC = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String REDIRECT_DESC = "Lorg/spongepowered/asm/mixin/injection/Redirect;";
    private static final String OVERWRITE_DESC = "Lorg/spongepowered/asm/mixin/Overwrite;";
    private static final String SHADOW_DESC = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String AT_DESC = "Lorg/spongepowered/asm/mixin/injection/At;";

    private final Map<String, ClassNode> mc = new HashMap<>();
    private final List<String> errors = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private int checks = 0;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: MixinAudit <minecraft-named.jar> <mod.jar> [mixin-prefix]");
            System.exit(2);
        }
        String prefix = args.length > 2 ? args[2] : "me/farlandsprobe/mixin/";
        MixinAudit audit = new MixinAudit();
        audit.index(Path.of(args[0]), prefix);
        List<String> mixinClasses = audit.listMixinClasses(Path.of(args[1]), prefix);
        System.out.println("MC jar : " + args[0]);
        System.out.println("Mod jar: " + args[1]);
        System.out.println("Mixin classes found: " + mixinClasses.size());
        System.out.println();
        for (String name : mixinClasses) {
            audit.auditMixin(name, Path.of(args[1]));
        }
        System.out.println();
        System.out.println("checks=" + audit.checks + " errors=" + audit.errors.size());
        for (String e : audit.errors) System.out.println("  [FAIL] " + e);
        for (String n : audit.notes) System.out.println("  [note] " + n);
        if (!audit.errors.isEmpty()) System.exit(1);
        System.out.println("RESULT: PASS");
    }

    /** 建立 MC 命名 jar 的类索引(name -> ClassNode,按需解析)。 */
    private void index(Path mcJar, String prefix) throws IOException {
        try (JarFile jar = new JarFile(mcJar.toFile())) {
            Enumeration<JarEntry> e = jar.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = e.nextElement();
                String n = entry.getName();
                if (!n.endsWith(".class")) continue;
                ClassNode node = new ClassNode();
                try (InputStream in = jar.getInputStream(entry)) {
                    new ClassReader(in).accept(node, ClassReader.SKIP_FRAMES);
                }
                mc.put(node.name, node);
            }
        }
        System.out.println("Indexed " + mc.size() + " classes from MC jar");
    }

    private List<String> listMixinClasses(Path modJar, String prefix) throws IOException {
        List<String> out = new ArrayList<>();
        try (JarFile jar = new JarFile(modJar.toFile())) {
            // 以 jar 内的 mixins.json 为准:只有注册过的 mixin 才会被 Mixin 处理,
            // 未注册的类(例如共享源码里被本版本换掉的旧注入)不参与审计。
            Set<String> registered = registeredMixins(jar, prefix);
            Enumeration<JarEntry> e = jar.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = e.nextElement();
                String n = entry.getName();
                if (n.endsWith(".class") && n.startsWith(prefix)) {
                    String name = n.substring(0, n.length() - ".class".length());
                    if (registered.contains(name)) out.add(name);
                }
            }
        }
        Collections.sort(out);
        return out;
    }

    /** 解析 mod jar 里所有 *.mixins.json,返回完整类名集合(按各自声明的 package 前缀展开)。 */
    private static Set<String> registeredMixins(JarFile jar, String prefix) throws IOException {
        // mixins.json 的保留键(非类名):其余字符串值按类名处理。
        Set<String> keys = Set.of(
                "required", "minVersion", "package", "mixins", "client", "server", "injectors",
                "defaultRequire", "compatibilityLevel", "refmap", "priority", "plugin"
        );
        Set<String> out = new HashSet<>();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String n = entry.getName();
            if (!n.endsWith(".mixins.json")) continue;
            String json;
            try (InputStream in = jar.getInputStream(entry)) {
                json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            String pkg = "";
            java.util.regex.Matcher pkgMatcher =
                    java.util.regex.Pattern.compile("\"package\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
            if (pkgMatcher.find()) pkg = pkgMatcher.group(1).replace('.', '/');
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("\"([A-Za-z_$][A-Za-z0-9_$.]*)\"").matcher(json);
            while (m.find()) {
                String v = m.group(1);
                if (keys.contains(v)) continue;
                out.add(pkg.isEmpty() ? v.replace('.', '/') : pkg + "/" + v.replace('.', '/'));
            }
        }
        return out;
    }
    private void auditMixin(String mixinName, Path modJar) throws IOException {
        ClassNode mixin;
        try (JarFile jar = new JarFile(modJar.toFile())) {
            ClassNode node = new ClassNode();
            try (InputStream in = jar.getInputStream(jar.getEntry(mixinName + ".class"))) {
                new ClassReader(in).accept(node, 0);
            }
            mixin = node;
        }
        AnnotationNode mixinAnn = findAnnotation(mixin.visibleAnnotations, MIXIN_DESC);
        if (mixinAnn == null) mixinAnn = findAnnotation(mixin.invisibleAnnotations, MIXIN_DESC);
        if (mixinAnn == null) {
            notes.add("skip (no @Mixin): " + mixinName);
            return;
        }
        List<String> targets = new ArrayList<>();
        Object value = annValue(mixinAnn, "value");
        if (value instanceof List<?> list) {
            for (Object o : list) if (o instanceof Type t) targets.add(t.getInternalName());
        }
        Object targetsAttr = annValue(mixinAnn, "targets");
        if (targetsAttr instanceof List<?> list) {
            for (Object o : list) if (o instanceof String s) targets.add(s.replace('.', '/'));
        }
        if (targets.isEmpty()) {
            errors.add(mixinName + ": @Mixin has no resolvable target");
            return;
        }
        boolean mixinIsInterface = (mixin.access & Opcodes.ACC_INTERFACE) != 0;
        for (String target : targets) {
            ClassNode targetNode = mc.get(target);
            if (targetNode == null) {
                errors.add(mixinName + ": target class not found in MC jar: " + target);
                continue;
            }
            // Mixin 要求 mixin 的"类型"与目标一致:接口 mixin 只能打接口目标,
            // class mixin 只能打 class 目标(否则运行时 InvalidMixinException:
            // "@Mixin target type mismatch")。
            checks++;
            boolean targetIsInterface = (targetNode.access & Opcodes.ACC_INTERFACE) != 0;
            if (mixinIsInterface != targetIsInterface) {
                errors.add(mixinName + ": mixin type mismatch - mixin is "
                        + (mixinIsInterface ? "an interface" : "a class") + " but " + target
                        + " is " + (targetIsInterface ? "an interface" : "a class"));
            }
            for (MethodNode m : mixin.methods) {
                auditMethod(mixinName, target, targetNode, m);
            }
            for (FieldNode f : mixin.fields) {
                AnnotationNode shadow = findAnnotation(f.visibleAnnotations, SHADOW_DESC);
                if (shadow == null) shadow = findAnnotation(f.invisibleAnnotations, SHADOW_DESC);
                if (shadow == null) continue;
                checks++;
                FieldNode found = findField(targetNode, f.name, f.desc);
                if (found == null) {
                    errors.add(mixinName + ": @Shadow field not found in " + target + ": " + f.name + " " + f.desc);
                }
            }
        }
    }

    private void auditMethod(String mixinName, String target, ClassNode targetNode, MethodNode m) {
        List<AnnotationNode> anns = new ArrayList<>();
        if (m.visibleAnnotations != null) anns.addAll(m.visibleAnnotations);
        if (m.invisibleAnnotations != null) anns.addAll(m.invisibleAnnotations);
        if (anns.isEmpty()) return;

        for (AnnotationNode ann : anns) {
            if (ann.desc.equals(SHADOW_DESC)) {
                // @Shadow 方法:方法名 + 描述符必须存在于目标类层级中(描述符可能因注入签名不同而省略)
                checks++;
                if (findMethod(targetNode, m.name, m.desc) == null) {
                    errors.add(mixinName + ": @Shadow method not found in " + target + ": " + m.name + m.desc);
                }
                continue;
            }
            boolean isInject = ann.desc.equals(INJECT_DESC);
            boolean isRedirect = ann.desc.equals(REDIRECT_DESC);
            boolean isOverwrite = ann.desc.equals(OVERWRITE_DESC);
            if (!isInject && !isRedirect && !isOverwrite) continue;

            List<String> selectors = selectors(ann);
            if (selectors.isEmpty()) {
                if (isOverwrite) selectors = List.of(m.name + m.desc);
                else {
                    errors.add(mixinName + ": " + simpleName(ann.desc) + " without method selector (" + m.name + ")");
                    continue;
                }
            }
            for (String selector : selectors) {
                checks++;
                String name = selectorName(selector);
                String desc = selectorDesc(selector);
                MethodNode found = desc == null ? findMethodByName(targetNode, name) : findMethod(targetNode, name, desc);
                if (found == null) {
                    errors.add(mixinName + ": " + simpleName(ann.desc) + " target method not found in " + target
                            + ": " + selector);
                    continue;
                }
                if (isRedirect || isInject) {
                    checkAtPoints(mixinName, target, found, ann, m);
                }
            }
        }
    }

    /** 校验 @At 指定的 INVOKE/FIELD/NEW 调用点在目标方法字节码中存在(考虑 ordinal)。 */
    private void checkAtPoints(String mixinName, String target, MethodNode targetMethod, AnnotationNode ann, MethodNode mixinMethod) {
        List<AnnotationNode> ats = atPoints(ann);
        for (AnnotationNode at : ats) {
            Object v = annValue(at, "value");
            String kind = enumName(v);
            if (kind == null) continue;
            if (!kind.equals("INVOKE") && !kind.equals("FIELD") && !kind.equals("NEW") && !kind.equals("CONSTANT")) continue;
            String targetRef = (String) annValue(at, "target");
            if (targetRef == null) {
                errors.add(mixinName + ": @At(" + kind + ") without target in " + mixinMethod.name);
                continue;
            }
            Integer ordinal = (Integer) annValue(at, "ordinal");
            checks++;
            List<AbstractInsnNode> matches = new ArrayList<>();
            for (AbstractInsnNode insn : targetMethod.instructions) {
                String ref = refOf(insn);
                if (ref == null) continue;
                if (kind.equals("INVOKE") && (insn instanceof MethodInsnNode)) {
                    if (matchesMember(ref, targetRef, true)) matches.add(insn);
                } else if (kind.equals("FIELD") && (insn instanceof FieldInsnNode)) {
                    if (matchesMember(ref, targetRef, false)) matches.add(insn);
                } else if (kind.equals("NEW") && (insn instanceof TypeInsnNode tin) && tin.getOpcode() == Opcodes.NEW) {
                    if (("L" + tin.desc + ";").equals(targetRef) || tin.desc.equals(targetRef)) matches.add(insn);
                }
            }
            if (matches.isEmpty()) {
                errors.add(mixinName + ": @At(" + kind + " target=" + targetRef + ") not present in "
                        + target + "." + targetMethod.name + targetMethod.desc);
            } else if (ordinal != null && (ordinal < 0 || ordinal >= matches.size())) {
                errors.add(mixinName + ": @At(" + kind + " target=" + targetRef + " ordinal=" + ordinal
                        + ") out of range (found " + matches.size() + ") in " + target + "." + targetMethod.name);
            }
        }
    }

    private static String refOf(AbstractInsnNode insn) {
        if (insn instanceof MethodInsnNode mi) {
            return "L" + mi.owner + ";" + mi.name + mi.desc;
        }
        if (insn instanceof FieldInsnNode fi) {
            return "L" + fi.owner + ";" + fi.name + ":" + fi.desc;
        }
        return null;
    }

    /** 比较字节码里的调用点与 @At 的 target 字符串(容忍 . 与 / 的差异)。 */
    private static boolean matchesMember(String actual, String expected, boolean method) {
        String a = actual.replace('.', '/');
        String e = expected.replace('.', '/');
        if (a.equals(e)) return true;
        // 容忍 @At target 省略返回类型或参数类型(只给 name)的情况
        if (e.endsWith(";" + selectorName(e.substring(1)))) {
            return a.startsWith(e);
        }
        return false;
    }

    private static List<AnnotationNode> atPoints(AnnotationNode ann) {
        Object at = annValue(ann, "at");
        List<AnnotationNode> out = new ArrayList<>();
        if (at instanceof AnnotationNode node) out.add(node);
        else if (at instanceof List<?> list) {
            for (Object o : list) if (o instanceof AnnotationNode node) out.add(node);
        }
        return out;
    }

    private static List<String> selectors(AnnotationNode ann) {
        Object v = annValue(ann, "method");
        List<String> out = new ArrayList<>();
        if (v instanceof String s) out.add(s);
        else if (v instanceof List<?> list) {
            for (Object o : list) if (o instanceof String s) out.add(s);
        }
        return out;
    }

    private static String selectorName(String selector) {
        int paren = selector.indexOf('(');
        return paren < 0 ? selector : selector.substring(0, paren);
    }

    private static String selectorDesc(String selector) {
        int paren = selector.indexOf('(');
        if (paren < 0) return null;
        return selector.substring(paren);
    }

    private static String simpleName(String desc) {
        int i = desc.lastIndexOf('/');
        return desc.substring(i + 1, desc.length() - 1);
    }

    private static String enumName(Object v) {
        if (v instanceof String[] arr && arr.length == 2) return arr[1];
        return null;
    }

    private static Object annValue(AnnotationNode ann, String key) {
        if (ann.values == null) return null;
        for (int i = 0; i + 1 < ann.values.size(); i += 2) {
            if (key.equals(ann.values.get(i))) return ann.values.get(i + 1);
        }
        return null;
    }

    private static AnnotationNode findAnnotation(List<AnnotationNode> anns, String desc) {
        if (anns == null) return null;
        for (AnnotationNode a : anns) if (a.desc.equals(desc)) return a;
        return null;
    }

    /** 在目标类及其父类/接口中查找方法。 */
    private MethodNode findMethod(ClassNode start, String name, String desc) {
        for (ClassNode c = start; c != null; c = superOf(c)) {
            for (MethodNode m : c.methods) {
                if (m.name.equals(name) && m.desc.equals(desc)) return m;
            }
        }
        return null;
    }

    private MethodNode findMethodByName(ClassNode start, String name) {
        for (ClassNode c = start; c != null; c = superOf(c)) {
            for (MethodNode m : c.methods) {
                if (m.name.equals(name)) return m;
            }
        }
        return null;
    }

    private FieldNode findField(ClassNode start, String name, String desc) {
        for (ClassNode c = start; c != null; c = superOf(c)) {
            for (FieldNode f : c.fields) {
                if (f.name.equals(name) && (desc == null || f.desc.equals(desc))) return f;
            }
        }
        return null;
    }

    private ClassNode superOf(ClassNode c) {
        if (c.superName == null) return null;
        return mc.get(c.superName);
    }
}
