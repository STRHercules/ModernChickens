package strhercules.chickens.integration.almostunified;

import strhercules.chickens.ChickensMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Optional integration with AlmostUnified.
 * <p>
 * If AlmostUnified is present at runtime, tag resolution for chicken drops
 * will defer to its runtime lookup so that the mod-priority config is
 * respected (e.g. prefer Mekanism copper over Create copper).
 * <p>
 * If AlmostUnified is absent the fallback path picks the first item found
 * in the tag, which mirrors the behaviour of {@code stackFromTag} already
 * used for lay-item resolution elsewhere in ModdedChickens.
 */
public final class AlmostUnifiedCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensAUCompat");

    /** Set to true once we have confirmed AU is on the class-path. */
    private static final boolean AVAILABLE;

    static {
        boolean found = false;
        try {
            Class.forName("com.almostreliable.unified.AlmostUnifiedCommon");
            found = true;
            LOGGER.info("AlmostUnified detected – chicken drops will respect AU mod priorities.");
        } catch (ClassNotFoundException ignored) {
            LOGGER.debug("AlmostUnified not found – falling back to first-tag-entry resolution.");
        }
        AVAILABLE = found;
    }

    private AlmostUnifiedCompat() {}

    /**
     * Resolves the preferred item for the given tag string.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>AlmostUnified runtime {@code getTagTargetItem} (if AU is loaded)</li>
     *   <li>First holder in the tag (vanilla / NeoForge tag lookup)</li>
     * </ol>
     *
     * @param tagName e.g. {@code "c:ingots/copper"} or {@code "c:silicon"}
     * @return the resolved stack, or {@link Optional#empty()} if the tag has no entries
     */
    public static Optional<ItemStack> resolvePreferred(String tagName) {
        ResourceLocation id = ResourceLocation.parse(tagName);
        TagKey<Item> tag = TagKey.create(Registries.ITEM, id);

        if (AVAILABLE) {
            Optional<ItemStack> auResult = resolveViaAU(tag);
            if (auResult.isPresent()) {
                return auResult;
            }
        }

        return resolveFromTag(tag);
    }

    // ── AlmostUnified path ────────────────────────────────────────────────

    private static Optional<ItemStack> resolveViaAU(TagKey<Item> tag) {
        try {
            var runtime = com.almostreliable.unified.AlmostUnifiedCommon.getRuntime();
            if (runtime == null) {
                return Optional.empty();
            }
            var lookup = runtime.getUnificationLookup();
            var entry = lookup.getTagTargetItem(tag);
            if (entry == null) {
                return Optional.empty();
            }
            Item item = entry.value();
            return Optional.of(new ItemStack(item));
        } catch (Exception e) {
            LOGGER.warn("AlmostUnified lookup failed for tag '{}', falling back to tag resolution: {}", tag.location(), e.getMessage());
            return Optional.empty();
        }
    }

    // ── Fallback: first item in tag ───────────────────────────────────────

    private static Optional<ItemStack> resolveFromTag(TagKey<Item> tag) {
        // Try HolderSet first (works after tags are loaded)
        Optional<ItemStack> fromHolder = BuiltInRegistries.ITEM.getTag(tag)
                .flatMap(set -> set.stream().findFirst())
                .map(h -> new ItemStack(h.value()));
        if (fromHolder.isPresent()) {
            return fromHolder;
        }
        // Fallback: linear scan (covers edge cases where getTag returns empty)
        for (Item item : BuiltInRegistries.ITEM) {
            if (item.builtInRegistryHolder().is(tag)) {
                return Optional.of(new ItemStack(item));
            }
        }
        return Optional.empty();
    }
}