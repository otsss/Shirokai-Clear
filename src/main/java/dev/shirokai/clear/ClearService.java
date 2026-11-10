package dev.shirokai.clear;

import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.slf4j.Logger;

/** One instance per server; ticks, jobs and entity changes always run on the server thread. */
public final class ClearService {
    public enum Mode { ALL, ITEMS, POKEMON }
    public record Result(long items, long pokemon, boolean preview) {}
    private final MinecraftServer server;
    private final Logger log;
    private final LoadedIndex<Entity> index = new LoadedIndex<>();
    private ClearConfig config;
    private long ticks, next;
    private boolean paused;
    private Job job;
    private Result last = new Result(0,0,false);
    private static final class Job {
        final long boundary; final Mode mode; final boolean preview;
        long cursor, items, pokemon, nextBatch;
        java.util.function.Consumer<Result> completion;
        Job(long boundary, Mode mode, boolean preview) { this.boundary=boundary; this.mode=mode; this.preview=preview; }
    }
    public ClearService(MinecraftServer server, Logger log, ClearConfig config) {
        this.server=server; this.log=log; this.config=config; resetDeadline();
    }
    public void loaded(Entity entity) {
        if (entity instanceof ItemEntity || entity instanceof com.cobblemon.mod.common.entity.pokemon.PokemonEntity) index.add(entity);
    }
    public void unloaded(Entity entity) { index.remove(entity); }
    public void stop() { job=null; index.clear(); }
    public ClearConfig config() { return config; }
    public void reload(ClearConfig value) { config=value; resetDeadline(); }
    public void pause() { paused=true; }
    public void resume() { paused=false; resetDeadline(); }
    private void resetDeadline() { next=ticks+(long)config.intervalSeconds*20; }
    public long remainingSeconds() { return Math.max(0,(next-ticks+19)/20); }
    public boolean paused() { return paused; }
    public boolean busy() { return job!=null; }
    public Result last() { return last; }
    public boolean start(Mode mode, boolean preview) { return start(mode,preview,null); }
    public boolean start(Mode mode, boolean preview, java.util.function.Consumer<Result> completion) {
        if (!server.isSameThread()) throw new IllegalStateException("Thread incorreta");
        if (job!=null) return false;
        job=new Job(index.boundary(),mode,preview); job.nextBatch=ticks; job.completion=completion; return true;
    }
    public void tick() {
        ticks++;
        if (config.enabled && !paused) {
            if (ticks>=next) { if(job==null) start(Mode.ALL,false); resetDeadline(); }
            else if ((next-ticks)%20==0 && config.announcements.enabled && config.announcements.warningSeconds.contains((int)((next-ticks)/20)))
                broadcast(config.announcements.warningMessage.replace("{seconds}",Long.toString((next-ticks)/20)));
        } else next++; // Freeze the displayed countdown while paused/disabled.
        if (job==null || ticks<job.nextBatch) return;
        Job active=job; active.nextBatch=ticks+config.performance.batchIntervalTicks;
        // No yield between snapshot, eligibility and discard. Every new batch gets fresh battle state.
        BattleGuard battles=new BattleGuard();
        for (int i=0;i<config.performance.maxEntitiesPerBatch;i++) {
            var entry=index.after(active.cursor);
            if (entry==null || entry.getKey()>active.boundary) { finish(active); return; }
            active.cursor=entry.getKey(); Entity entity=entry.getValue();
            boolean item=entity instanceof ItemEntity;
            if ((active.mode==Mode.ITEMS && !item) || (active.mode==Mode.POKEMON && item)) continue;
            String reason=Eligibility.reason(entity,config,battles);
            if (reason!=null) {
                if(config.logging.enabled && config.logging.logProtectedEntities) log.info("Protegida {}: {}",entity.getUUID(),reason);
                continue;
            }
            // Query the object again at the mutation point; no persisted eligibility decisions.
            if (Eligibility.reason(entity,config,battles)!=null) continue;
            if (!active.preview) entity.discard();
            if(item) active.items++; else active.pokemon++;
        }
    }
    private void finish(Job active) {
        job=null;
        var result=new Result(active.items,active.pokemon,active.preview);
        if (!active.preview) last=result;
        if(config.logging.enabled && config.logging.logSummary) log.info("{}: itens={}, Pokémon={}",active.preview?"Prévia":"Limpeza",active.items,active.pokemon);
        if(active.completion!=null) active.completion.accept(result);
        if(!active.preview && config.announcements.enabled && config.announcements.showResults)
            broadcast(config.announcements.resultMessage.replace("{items}",Long.toString(active.items)).replace("{pokemon}",Long.toString(active.pokemon)));
    }
    private void broadcast(String message) { server.getPlayerList().broadcastSystemMessage(Component.literal(message),false); }
}
