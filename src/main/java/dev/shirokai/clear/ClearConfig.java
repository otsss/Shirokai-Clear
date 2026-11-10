package dev.shirokai.clear;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public final class ClearConfig {
    public boolean enabled = true;
    public int intervalSeconds = 600;
    public boolean clearItems = true, clearWildPokemon = true, clearOtherEntities = false;
    public Items items = new Items();
    public Pokemon pokemon = new Pokemon();
    public Npcs npcs = new Npcs();
    public Worlds worlds = new Worlds();
    public Announcements announcements = new Announcements();
    public Performance performance = new Performance();
    public Logging logging = new Logging();
    public static final class Items {
        public int minimumAgeSeconds = 60;
        public boolean protectNamedItems = true;
        public Set<String> excludedItemIds = new HashSet<>();
    }
    public static final class Pokemon {
        public boolean protectBattles = true, protectPlayerPokemon = true, protectNpcPokemon = true;
        public boolean protectLegendary = true, protectMythical = true, protectShiny = true;
        public boolean protectRaidBosses = true, protectNamedPokemon = true, protectTaggedPokemon = true;
        public int minimumAgeSeconds = 120;
        public Set<String> excludedSpecies = new HashSet<>();
    }
    public static final class Npcs { public boolean protectEasyNPC = true, protectAllNPCs = true; }
    public static final class Worlds {
        public Set<String> enabledDimensions = new HashSet<>(), excludedDimensions = new HashSet<>();
        public boolean accepts(String id) {
            return !excludedDimensions.contains(id) && (enabledDimensions.isEmpty() || enabledDimensions.contains(id));
        }
    }
    public static final class Announcements {
        public boolean enabled = true, showResults = true;
        public Set<Integer> warningSeconds = new HashSet<>(List.of(60,30,10,5,3,2,1));
        public String warningMessage = "§b✦ Shirokai §8» §7Limpeza de entidades em §e{seconds} segundos§7!";
        public String resultMessage = "§b✦ Shirokai §8» §aLimpeza concluída! §7Itens removidos: §f{items} §8| §7Pokémon removidos: §f{pokemon}";
    }
    public static final class Performance {
        public int maxEntitiesPerBatch = 100, batchIntervalTicks = 1;
        public boolean onlyLoadedChunks = true;
    }
    public static final class Logging { public boolean enabled = true, logProtectedEntities = false, logSummary = true; }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static ClearConfig load(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(new ClearConfig()) + System.lineSeparator(), StandardOpenOption.CREATE_NEW);
        }
        // Gson defaults omitted fields, but explicit nulls must never weaken protection.
        var tree = JsonParser.parseString(Files.readString(path));
        if (!tree.isJsonObject()) throw new IllegalArgumentException("A configuração deve ser um objeto JSON");
        var c = GSON.fromJson(tree, ClearConfig.class);
        c.validate(); return c;
    }
    public void validate() {
        Objects.requireNonNull(items); Objects.requireNonNull(pokemon); Objects.requireNonNull(npcs);
        Objects.requireNonNull(worlds); Objects.requireNonNull(announcements);
        Objects.requireNonNull(performance); Objects.requireNonNull(logging);
        Objects.requireNonNull(items.excludedItemIds); Objects.requireNonNull(pokemon.excludedSpecies);
        Objects.requireNonNull(worlds.enabledDimensions); Objects.requireNonNull(worlds.excludedDimensions);
        Objects.requireNonNull(announcements.warningSeconds); Objects.requireNonNull(announcements.warningMessage);
        Objects.requireNonNull(announcements.resultMessage);
        if (intervalSeconds < 1 || intervalSeconds > 604800 || items.minimumAgeSeconds < 0 || pokemon.minimumAgeSeconds < 0
            || items.minimumAgeSeconds > 604800 || pokemon.minimumAgeSeconds > 604800
            || performance.maxEntitiesPerBatch < 1 || performance.maxEntitiesPerBatch > 10000
            || performance.batchIntervalTicks < 1 || performance.batchIntervalTicks > 1200)
            throw new IllegalArgumentException("Intervalo, idade ou lote fora dos limites seguros");
        if (clearOtherEntities || !performance.onlyLoadedChunks || !pokemon.protectBattles || !pokemon.protectPlayerPokemon
            || !pokemon.protectNpcPokemon || !pokemon.protectRaidBosses || !npcs.protectEasyNPC || !npcs.protectAllNPCs)
            throw new IllegalArgumentException("Batalhas, donos, NPCs e raids são proteções obrigatórias; somente chunks carregados e itens/Pokémon são suportados");
        for (Integer s : announcements.warningSeconds)
            if (s == null || s < 1 || s > 604800) throw new IllegalArgumentException("Aviso inválido");
        for (var ids : List.of(items.excludedItemIds, worlds.enabledDimensions, worlds.excludedDimensions))
            for (String id : ids) if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("Identificador inválido: " + id);
        for (String id : pokemon.excludedSpecies)
            if (id == null || !id.matches("(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+")) throw new IllegalArgumentException("Espécie inválida: " + id);
    }
}
