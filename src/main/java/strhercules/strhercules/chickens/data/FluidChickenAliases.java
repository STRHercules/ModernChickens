package strhercules.chickens.data;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pack-owned equivalence rules for fluids that should share one chicken.
 * Rules are deliberately opt-in because registry IDs with the same display
 * name are not automatically interchangeable in every modpack.
 */
final class FluidChickenAliases {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensFluidAliases");
    private static final String RESOURCE = "/defaultconfigs/fluid_chicken_aliases.toml";
    private static final String CONFIG_FILE = "fluid_chicken_aliases.toml";
    private static volatile List<AliasRule> rules = List.of();

    private FluidChickenAliases() {
    }

    static void load() {
        Path path = TomlConfigBridge.configDirectory().resolve(CONFIG_FILE);
        TomlConfigBridge.ensureResource(path, RESOURCE);
        if (!Files.exists(path)) {
            rules = List.of();
            return;
        }

        List<AliasRule> exact = new ArrayList<>();
        List<AliasRule> wildcard = new ArrayList<>();
        try (CommentedFileConfig config = CommentedFileConfig.builder(path, TomlFormat.instance()).build()) {
            config.load();
            Object rawAliases = config.getRaw("aliases");
            if (rawAliases instanceof UnmodifiableConfig aliases) {
                for (UnmodifiableConfig.Entry entry : aliases.entrySet()) {
                    AliasRule rule = AliasRule.parse(entry.getKey(), String.valueOf((Object) entry.getRawValue()));
                    if (rule == null) {
                        continue;
                    }
                    (rule.wildcard() ? wildcard : exact).add(rule);
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to read {}; fluid aliases are disabled", path.getFileName(), ex);
        }

        exact.addAll(wildcard);
        rules = List.copyOf(exact);
        LOGGER.info("Loaded {} fluid chicken alias rules from {}", rules.size(), path.getFileName());
    }

    static ResourceLocation canonicalId(ResourceLocation fluidId) {
        ResourceLocation current = fluidId;
        Set<ResourceLocation> visited = new HashSet<>();
        while (visited.add(current)) {
            ResourceLocation next = null;
            for (AliasRule rule : rules) {
                next = rule.resolve(current);
                if (next != null) {
                    break;
                }
            }
            if (next == null || next.equals(current)) {
                return current;
            }
            current = next;
        }

        LOGGER.warn("Ignoring cyclic fluid chicken alias chain beginning at {}", fluidId);
        return fluidId;
    }

    private record AliasRule(String aliasNamespace, String aliasPath, boolean aliasWildcard,
                             String canonicalNamespace, String canonicalPath, boolean canonicalWildcard) {
        private static AliasRule parse(String rawAlias, String rawCanonical) {
            String alias = rawAlias.trim().toLowerCase(Locale.ROOT);
            String canonical = rawCanonical.trim().toLowerCase(Locale.ROOT);
            int aliasSeparator = alias.indexOf(':');
            int canonicalSeparator = canonical.indexOf(':');
            if (aliasSeparator <= 0 || canonicalSeparator <= 0
                    || aliasSeparator != alias.lastIndexOf(':')
                    || canonicalSeparator != canonical.lastIndexOf(':')) {
                LOGGER.warn("Ignoring malformed fluid chicken alias {} = {}", rawAlias, rawCanonical);
                return null;
            }

            String aliasNamespace = alias.substring(0, aliasSeparator);
            String aliasPath = alias.substring(aliasSeparator + 1);
            String canonicalNamespace = canonical.substring(0, canonicalSeparator);
            String canonicalPath = canonical.substring(canonicalSeparator + 1);
            boolean aliasWildcard = aliasPath.endsWith("*");
            boolean canonicalWildcard = canonicalPath.endsWith("*");
            if (aliasPath.indexOf('*') >= 0 && !aliasWildcard
                    || canonicalPath.indexOf('*') >= 0 && !canonicalWildcard
                    || canonicalWildcard && !aliasWildcard) {
                LOGGER.warn("Ignoring unsupported fluid chicken alias {} = {}; only trailing path wildcards are supported",
                        rawAlias, rawCanonical);
                return null;
            }

            String aliasPrefix = aliasWildcard ? aliasPath.substring(0, aliasPath.length() - 1) : aliasPath;
            String canonicalPrefix = canonicalWildcard
                    ? canonicalPath.substring(0, canonicalPath.length() - 1)
                    : canonicalPath;
            String aliasProbe = aliasNamespace + ":" + aliasPrefix + (aliasWildcard ? "alias" : "");
            String canonicalProbe = canonicalNamespace + ":" + canonicalPrefix + (canonicalWildcard ? "canonical" : "");
            if (ResourceLocation.tryParse(aliasProbe) == null || ResourceLocation.tryParse(canonicalProbe) == null) {
                LOGGER.warn("Ignoring malformed fluid chicken alias {} = {}", rawAlias, rawCanonical);
                return null;
            }
            return new AliasRule(aliasNamespace, aliasPrefix, aliasWildcard,
                    canonicalNamespace, canonicalPrefix, canonicalWildcard);
        }

        private ResourceLocation resolve(ResourceLocation source) {
            if (!aliasNamespace.equals(source.getNamespace())
                    || aliasWildcard && !source.getPath().startsWith(aliasPath)
                    || !aliasWildcard && !aliasPath.equals(source.getPath())) {
                return null;
            }
            String suffix = aliasWildcard ? source.getPath().substring(aliasPath.length()) : "";
            String targetPath = canonicalWildcard ? canonicalPath + suffix : canonicalPath;
            return ResourceLocation.tryParse(canonicalNamespace + ":" + targetPath);
        }

        private boolean wildcard() {
            return aliasWildcard;
        }
    }
}
