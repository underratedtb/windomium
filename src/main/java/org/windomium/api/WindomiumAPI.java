package org.windomium.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class WindomiumAPI {
    private static final String API_VERSION = "1.0.0";
    private static final Map<String, RenderBehaviour> ENTITY_BEHAVIOURS = new ConcurrentHashMap<>();
    private static final Map<String, RenderBehaviour> BLOCK_ENTITY_BEHAVIOURS = new ConcurrentHashMap<>();
    private static final Map<String, RenderBehaviour> RENDERER_CLASS_BEHAVIOURS = new ConcurrentHashMap<>();
    private static Consumer<IOptimisationModule> moduleRegistrar;

    private WindomiumAPI() {
    }

    public static String getApiVersion() {
        return API_VERSION;
    }

    public static void setModuleRegistrar(Consumer<IOptimisationModule> registrar) {
        if (moduleRegistrar == null) {
            moduleRegistrar = registrar;
        }
    }

    public static void registerModule(IOptimisationModule module) {
        if (moduleRegistrar != null) {
            moduleRegistrar.accept(module);
        }
    }

    public static void registerEntityBehaviour(String entityId, RenderBehaviour behaviour) {
        ENTITY_BEHAVIOURS.put(entityId, behaviour);
    }

    public static void registerBlockEntityBehaviour(String blockEntityId, RenderBehaviour behaviour) {
        BLOCK_ENTITY_BEHAVIOURS.put(blockEntityId, behaviour);
    }

    public static void registerRendererBehaviour(Class<?> rendererClass, RenderBehaviour behaviour) {
        RENDERER_CLASS_BEHAVIOURS.put(rendererClass.getName(), behaviour);
    }

    public static void bypassRendererClass(String className) {
        RENDERER_CLASS_BEHAVIOURS.put(className, RenderBehaviour.DO_NOT_TOUCH);
    }

    public static Optional<RenderBehaviour> getEntityBehaviour(String entityId) {
        return Optional.ofNullable(ENTITY_BEHAVIOURS.get(entityId));
    }

    public static Optional<RenderBehaviour> getBlockEntityBehaviour(String blockEntityId) {
        return Optional.ofNullable(BLOCK_ENTITY_BEHAVIOURS.get(blockEntityId));
    }

    public static Optional<RenderBehaviour> getRendererBehaviour(String className) {
        return Optional.ofNullable(RENDERER_CLASS_BEHAVIOURS.get(className));
    }

    public static Collection<String> getBypassedRenderers() {
        return RENDERER_CLASS_BEHAVIOURS.entrySet().stream()
                .filter(entry -> entry.getValue() == RenderBehaviour.DO_NOT_TOUCH)
                .map(Map.Entry::getKey)
                .toList();
    }
}