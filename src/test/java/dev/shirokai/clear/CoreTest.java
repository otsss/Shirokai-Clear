package dev.shirokai.clear;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class CoreTest {
    @TempDir Path temp;
    @Test void defaultConfigRoundTrip() throws Exception { var c=ClearConfig.load(temp.resolve("config.json")); assertEquals(600,c.intervalSeconds); assertTrue(c.pokemon.protectBattles); }
    @Test void invalidReloadPreservesFile() throws Exception {
        var p=temp.resolve("bad.json"); Files.writeString(p,"{broken");
        assertThrows(Exception.class,()->ClearConfig.load(p)); assertEquals("{broken",Files.readString(p));
    }
    @Test void mandatoryProtectionsCannotBeDisabled() {
        var c=new ClearConfig(); c.pokemon.protectBattles=false; assertThrows(IllegalArgumentException.class,c::validate);
        c=new ClearConfig();c.pokemon.protectNpcPokemon=false;assertThrows(IllegalArgumentException.class,c::validate);
        c=new ClearConfig();c.pokemon.protectRaidBosses=false;assertThrows(IllegalArgumentException.class,c::validate);
        c=new ClearConfig();c.clearOtherEntities=true;assertThrows(IllegalArgumentException.class,c::validate);
        c=new ClearConfig();c.performance.onlyLoadedChunks=false;assertThrows(IllegalArgumentException.class,c::validate);
    }
    @Test void invalidLimitsFailClosed() {var c=new ClearConfig();c.performance.maxEntitiesPerBatch=0;assertThrows(IllegalArgumentException.class,c::validate);}
    @Test void explicitNullFailsClosed() throws Exception {var p=temp.resolve("null.json");Files.writeString(p,"{\"pokemon\":null}");assertThrows(Exception.class,()->ClearConfig.load(p));}
    @Test void dimensionsDenyTakesPrecedence() {var w=new ClearConfig.Worlds();w.enabledDimensions.add("minecraft:overworld");w.excludedDimensions.add("minecraft:overworld");assertFalse(w.accepts("minecraft:overworld"));assertFalse(w.accepts("minecraft:the_nether"));}
    @Test void indexSurvivesConcurrentLifecycleBetweenBatches() {
        var i=new LoadedIndex<Object>();var a=new Object();var b=new Object();var c=new Object();
        i.add(a);i.add(b);long limit=i.boundary();long first=i.after(0).getKey();
        i.remove(a);i.remove(b);i.add(c);assertTrue(i.after(first).getKey()>limit);
    }
    @Test void indexDeduplicatesAndResets() {var i=new LoadedIndex<Object>();var a=new Object();i.add(a);i.add(a);assertEquals(1,i.boundary());i.clear();assertNull(i.after(0));assertEquals(0,i.boundary());}
}
