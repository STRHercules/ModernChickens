package strhercules.chickens.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Edits the player-owned TOML config while preserving its comments and layout. */
public final class ChickensConfigScreen extends Screen {
    private static final Pattern SECTION = Pattern.compile("^\\s*\\[([^]]+)]\\s*$");
    private static final Pattern ENTRY = Pattern.compile("^\\s*([A-Za-z][A-Za-z0-9_]*)\\s*=\\s*(.*)$");
    private static final Set<String> BOOLEAN_KEYS = Set.of(
            "alwaysShowStats", "disableEggLaying", "avianFluxEffectsEnabled",
            "avianFluidConverterEffectsEnabled", "avianChemicalConverterEffectsEnabled",
            "liquidEggHazardsEnabled", "scalingDrops", "enableFluidChickens",
            "autoRegisterFluidChickens", "allowAllLiquidChemicalDousing",
            "enableChemicalChickens", "enableGasChickens", "enabled", "allowNaturalSpawn", "allowDousing",
            "generatedTexture");
    private static final Set<String> INTEGER_KEYS = Set.of(
            "spawnProbability", "minBroodSize", "maxBroodSize", "roosterAuraRange",
            "nestMaxRoosters", "nestSeedDurationTicks", "collectorScanRange", "avianFluxCapacity",
            "mechanicalNestBaseEnergyPerTick", "mechanicalNestEnergyPerRoostPerTick", "mechanicalNestRange",
            "mechanicalRoostTier1EnergyCost", "mechanicalRoostTier10EnergyCost",
            "avianFluxMaxReceive", "avianFluxMaxExtract", "avianFluidConverterCapacity",
            "avianFluidConverterTransferRate", "avianChemicalConverterCapacity",
            "avianChemicalConverterTransferRate", "incubatorEnergyCost", "incubatorCapacity",
            "incubatorMaxReceive", "roostDropCount", "layItemAmount", "layItemMeta",
            "dropItemAmount", "dropItemMeta", "liquidDousingCost", "id");
    private static final Set<String> DECIMAL_KEYS = Set.of(
            "netherSpawnChanceMultiplier", "overworldSpawnChance", "netherSpawnChance",
            "endSpawnChance", "roostSpeed", "breederSpeed", "roosterAuraMultiplier",
            "fluxEggCapacityMultiplier", "mechanicalNestEnergyCostSpeedIncrease",
            "mechanicalRoostEnergyCostSpeedIncrease", "layCoefficient");
    private static final int ROW_HEIGHT = 24;
    private static final int CONTENT_TOP = 54;
    private static final int CONTENT_BOTTOM = 28;
    private static final int PANEL_WIDTH = 760;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_MIN_THUMB = 20;

    private final Screen parent;
    private final ConfigDocument document;
    private final boolean chickenSettings;
    private final List<EditBox> valueBoxes = new ArrayList<>();
    private int[] boundEntries = new int[0];
    private EditBox searchBox;
    private Button saveButton;
    private List<ConfigEntry> filteredEntries = List.of();
    private int firstVisible;
    private int panelLeft;
    private int valueX;
    private int valueWidth;
    private boolean draggingScrollbar;
    private double scrollbarDragOffset;
    private Component error;

    public ChickensConfigScreen(Screen parent) {
        this(parent, ConfigDocument.load("chickens.toml"), false);
    }

    private ChickensConfigScreen(Screen parent, ConfigDocument document, boolean chickenSettings) {
        super(Component.translatable(chickenSettings
                ? "screen.chickens.config.chickens"
                : "screen.chickens.config"));
        this.parent = parent;
        this.document = document;
        this.chickenSettings = chickenSettings;
        this.error = document.loadError;
    }

    @Override
    protected void init() {
        valueBoxes.clear();
        panelLeft = Math.max(10, (width - Math.min(PANEL_WIDTH, width - 20)) / 2);
        int panelWidth = Math.min(PANEL_WIDTH, width - 20);
        valueX = panelLeft + Math.min(350, panelWidth / 2);
        valueWidth = Math.max(80, panelLeft + panelWidth - valueX - 8);

        searchBox = addRenderableWidget(new EditBox(font, panelLeft + 8, 24,
                Math.max(120, valueX - panelLeft - 20), 20,
                Component.translatable("screen.chickens.config.search")));
        searchBox.setMaxLength(128);
        searchBox.setResponder(value -> {
            firstVisible = 0;
            refreshRows(value);
        });

        int buttonRight = panelLeft + panelWidth - 8;
        if (chickenSettings) {
            saveButton = addRenderableWidget(Button.builder(Component.translatable("screen.chickens.config.save"),
                    button -> saveAndClose()).bounds(buttonRight - 184, 24, 88, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.chickens.config.back"),
                    button -> onClose()).bounds(buttonRight - 88, 24, 88, 20).build());
        } else {
            int cancelX = buttonRight - 88;
            int chickenSettingsX = cancelX - 148;
            saveButton = addRenderableWidget(Button.builder(Component.translatable("screen.chickens.config.save"),
                    button -> saveAndClose()).bounds(chickenSettingsX - 96, 24, 88, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.chickens.config.chickens_button"),
                    button -> minecraft.setScreen(new ChickensConfigScreen(this,
                            ConfigDocument.load("custom_chickens.toml"), true)))
                    .bounds(chickenSettingsX, 24, 140, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                    .bounds(cancelX, 24, 88, 20).build());
        }
        saveButton.active = error == null;

        int visibleRows = Math.max(1, (height - CONTENT_TOP - CONTENT_BOTTOM) / ROW_HEIGHT);
        boundEntries = new int[visibleRows];
        for (int slot = 0; slot < visibleRows; slot++) {
            int row = slot;
            EditBox box = new EditBox(font, valueX, CONTENT_TOP + slot * ROW_HEIGHT + 1, valueWidth, 20,
                    Component.empty());
            box.setMaxLength(512);
            box.setResponder(value -> {
                int entryIndex = boundEntries[row];
                if (entryIndex >= 0 && entryIndex < filteredEntries.size()) {
                    filteredEntries.get(entryIndex).value = value;
                }
            });
            valueBoxes.add(addRenderableWidget(box));
        }

        refreshRows(searchBox.getValue());
    }

    private void refreshRows(String query) {
        String normalizedQuery = query == null ? "" : query.toLowerCase(Locale.ROOT);
        filteredEntries = document.entries.stream()
                .filter(entry -> chickenSettings
                        ? !entry.section.equalsIgnoreCase("general")
                        : entry.section.equalsIgnoreCase("general"))
                .filter(entry -> normalizedQuery.isBlank()
                        || entry.searchText().contains(normalizedQuery)
                        || labelFor(entry).getString().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .toList();
        int maxFirst = Math.max(0, filteredEntries.size() - valueBoxes.size());
        firstVisible = Math.min(firstVisible, maxFirst);
        bindRows();
    }

    private void bindRows() {
        for (int slot = 0; slot < valueBoxes.size(); slot++) {
            EditBox box = valueBoxes.get(slot);
            int entryIndex = firstVisible + slot;
            boundEntries[slot] = entryIndex < filteredEntries.size() ? entryIndex : -1;
            if (boundEntries[slot] < 0) {
                box.setVisible(false);
                box.active = false;
                box.setValue("");
                continue;
            }

            ConfigEntry entry = filteredEntries.get(entryIndex);
            box.setVisible(true);
            box.active = true;
            box.setX(valueX);
            box.setY(CONTENT_TOP + slot * ROW_HEIGHT + 1);
            box.setWidth(valueWidth);
            box.setValue(entry.value);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY >= CONTENT_TOP && mouseY < height - CONTENT_BOTTOM && !filteredEntries.isEmpty()) {
            int maxFirst = Math.max(0, filteredEntries.size() - valueBoxes.size());
            int step = scrollY > 0 ? -3 : scrollY < 0 ? 3 : 0;
            firstVisible = Math.max(0, Math.min(maxFirst, firstVisible + step));
            bindRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hasScrollbar() && isOverScrollbar(mouseX, mouseY)) {
            int thumbY = scrollbarThumbY();
            int thumbHeight = scrollbarThumbHeight();
            scrollbarDragOffset = mouseY >= thumbY && mouseY < thumbY + thumbHeight
                    ? mouseY - thumbY
                    : thumbHeight / 2.0;
            draggingScrollbar = true;
            updateScrollFromMouse(mouseY - scrollbarDragOffset);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            updateScrollFromMouse(mouseY - scrollbarDragOffset);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean hasScrollbar() {
        return filteredEntries.size() > valueBoxes.size();
    }

    private int scrollbarX() {
        return panelLeft + Math.min(PANEL_WIDTH, width - panelLeft - 10) - SCROLLBAR_WIDTH;
    }

    private int scrollbarBottom() {
        return height - CONTENT_BOTTOM;
    }

    private int scrollbarTrackHeight() {
        return scrollbarBottom() - CONTENT_TOP;
    }

    private int scrollbarThumbHeight() {
        int trackHeight = scrollbarTrackHeight();
        return Math.max(SCROLLBAR_MIN_THUMB, trackHeight * valueBoxes.size() / filteredEntries.size());
    }

    private int scrollbarThumbY() {
        int maxFirst = filteredEntries.size() - valueBoxes.size();
        int travel = scrollbarTrackHeight() - scrollbarThumbHeight();
        int offset = maxFirst <= 0 ? 0 : (int) Math.round(travel * firstVisible / (double) maxFirst);
        return CONTENT_TOP + offset;
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        return mouseX >= scrollbarX() - 4 && mouseX <= scrollbarX() + SCROLLBAR_WIDTH + 4
                && mouseY >= CONTENT_TOP && mouseY < scrollbarBottom();
    }

    private void updateScrollFromMouse(double thumbY) {
        int maxFirst = filteredEntries.size() - valueBoxes.size();
        int travel = scrollbarTrackHeight() - scrollbarThumbHeight();
        int clampedThumbY = Math.max(CONTENT_TOP, Math.min(CONTENT_TOP + travel, (int) Math.round(thumbY)));
        firstVisible = travel <= 0 ? 0
                : (int) Math.round(maxFirst * (clampedThumbY - CONTENT_TOP) / (double) travel);
        bindRows();
    }

    private void saveAndClose() {
        Component validationError = document.validate();
        if (validationError != null) {
            error = validationError;
            return;
        }
        try {
            document.save();
            minecraft.setScreen(parent);
        } catch (IOException ex) {
            error = Component.translatable("screen.chickens.config.error.save", document.fileName, ex.getMessage());
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);
        for (int slot = 0; slot < valueBoxes.size(); slot++) {
            int entryIndex = boundEntries[slot];
            if (entryIndex < 0 || entryIndex >= filteredEntries.size()) {
                continue;
            }
            int y = CONTENT_TOP + slot * ROW_HEIGHT;
            int color = slot % 2 == 0 ? 0x40101010 : 0x30202020;
            graphics.fill(panelLeft, y, panelLeft + Math.min(PANEL_WIDTH, width - panelLeft - 10), y + ROW_HEIGHT,
                    color);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.chickens.config.search_label"), panelLeft + 8,
                16, 0xA0A0A0);
        for (int slot = 0; slot < valueBoxes.size(); slot++) {
            int entryIndex = boundEntries[slot];
            if (entryIndex < 0 || entryIndex >= filteredEntries.size()) {
                continue;
            }
            graphics.drawString(font, labelFor(filteredEntries.get(entryIndex)), panelLeft + 8,
                    CONTENT_TOP + slot * ROW_HEIGHT + 7, 0xFFFFFF);
        }
        if (hasScrollbar()) {
            int scrollbarX = scrollbarX();
            graphics.fill(scrollbarX, CONTENT_TOP, scrollbarX + SCROLLBAR_WIDTH, scrollbarBottom(), 0x55333333);
            int thumbY = scrollbarThumbY();
            graphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH,
                    thumbY + scrollbarThumbHeight(), draggingScrollbar ? 0xFFFFFFFF : 0xFFAAAAAA);
        }
        Component footer = error != null ? error : Component.translatable("screen.chickens.config.restart");
        graphics.drawString(font, footer, panelLeft + 8, height - 18,
                error != null ? 0xFF5555 : 0xA0A0A0);
    }

    private static Component labelFor(ConfigEntry entry) {
        Component option = optionName(entry.key);
        if (entry.section.equalsIgnoreCase("general")) {
            return option;
        }
        return Component.translatable("config.chickens.chicken.option", chickenName(entry.section), option);
    }

    private static Component optionName(String key) {
        String translationKey = "config.chickens.option." + key;
        return I18n.exists(translationKey)
                ? Component.translatable(translationKey)
                : Component.translatable("config.chickens.option.unknown", key);
    }

    private static Component chickenName(String section) {
        String translationKey = "entity." + section + ".name";
        return I18n.exists(translationKey)
                ? Component.translatable(translationKey)
                : Component.translatable("config.chickens.chicken.name", section);
    }

    private static final class ConfigDocument {
        private final Path path;
        private final String fileName;
        private final List<DocumentLine> lines;
        private final List<ConfigEntry> entries;
        private final Component loadError;

        private ConfigDocument(Path path, String fileName, List<DocumentLine> lines, List<ConfigEntry> entries,
                Component loadError) {
            this.path = path;
            this.fileName = fileName;
            this.lines = lines;
            this.entries = entries;
            this.loadError = loadError;
        }

        private static ConfigDocument load(String fileName) {
            Path path = FMLPaths.CONFIGDIR.get().resolve(fileName);
            List<String> source;
            try {
                if (Files.exists(path)) {
                    source = Files.readAllLines(path, StandardCharsets.UTF_8);
                } else {
                    try (InputStream stream = ChickensConfigScreen.class.getResourceAsStream("/defaultconfigs/" + fileName)) {
                        if (stream == null) {
                            throw new IOException("default config is missing");
                        }
                        source = new java.io.BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().toList();
                    }
                }
            } catch (IOException ex) {
                return new ConfigDocument(path, fileName, new ArrayList<>(), new ArrayList<>(),
                        Component.translatable("screen.chickens.config.error.read", fileName, ex.getMessage()));
            }

            List<DocumentLine> lines = new ArrayList<>();
            List<ConfigEntry> entries = new ArrayList<>();
            String section = null;
            for (String text : source) {
                Matcher sectionMatch = SECTION.matcher(text);
                if (sectionMatch.matches()) {
                    String table = sectionMatch.group(1).trim();
                    section = table.equalsIgnoreCase("general") ? "general"
                            : table.regionMatches(true, 0, "chickens.", 0, "chickens.".length())
                                    ? unquote(table.substring("chickens.".length()))
                                    : null;
                }

                ConfigEntry entry = null;
                Matcher entryMatch = ENTRY.matcher(text);
                if (section != null && entryMatch.matches()) {
                    String key = entryMatch.group(1).trim();
                    char type = typeFor(key);
                    entry = new ConfigEntry(section, key, type, decodeValue(entryMatch.group(2).trim(), type));
                    entries.add(entry);
                }
                lines.add(new DocumentLine(text, entry));
            }
            return new ConfigDocument(path, fileName, lines, entries, null);
        }

        private static String unquote(String value) {
            return value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"'
                    ? value.substring(1, value.length() - 1)
                    : value;
        }

        private Component validate() {
            for (ConfigEntry entry : entries) {
                String value = entry.value.trim();
                try {
                    switch (entry.type) {
                        case 'B' -> {
                            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                                return Component.translatable("screen.chickens.config.error.boolean", labelFor(entry));
                            }
                            entry.value = value.toLowerCase(Locale.ROOT);
                        }
                        case 'I' -> {
                            Integer.parseInt(value);
                            entry.value = value;
                        }
                        case 'D' -> {
                            if (!Double.isFinite(Double.parseDouble(value))) {
                                return Component.translatable("screen.chickens.config.error.decimal", labelFor(entry));
                            }
                            entry.value = value;
                        }
                        default -> {
                            // S and unknown legacy value types are intentionally free-form.
                        }
                    }
                } catch (NumberFormatException ex) {
                    String errorKey = entry.type == 'I'
                            ? "screen.chickens.config.error.integer"
                            : "screen.chickens.config.error.decimal";
                    return Component.translatable(errorKey, labelFor(entry));
                }
            }
            return null;
        }

        private void save() throws IOException {
            Files.createDirectories(path.getParent());
            List<String> output = new ArrayList<>(lines.size());
            for (DocumentLine line : lines) {
                output.add(line.entry == null ? line.text
                        : line.text.substring(0, line.text.indexOf('=') + 1) + " " + formatValue(line.entry));
            }
            Files.write(path, output, StandardCharsets.UTF_8);
        }
    }

    private static final class DocumentLine {
        private final String text;
        private final ConfigEntry entry;

        private DocumentLine(String text, ConfigEntry entry) {
            this.text = text;
            this.entry = entry;
        }
    }

    private static final class ConfigEntry {
        private final String section;
        private final String key;
        private final char type;
        private String value;

        private ConfigEntry(String section, String key, char type, String value) {
            this.section = section;
            this.key = key;
            this.type = type;
            this.value = value;
        }

        private String searchText() {
            return (section + "." + key).toLowerCase(Locale.ROOT);
        }
    }

    private static char typeFor(String key) {
        if (BOOLEAN_KEYS.contains(key)) {
            return 'B';
        }
        if (INTEGER_KEYS.contains(key)) {
            return 'I';
        }
        if (DECIMAL_KEYS.contains(key)) {
            return 'D';
        }
        return 'S';
    }

    private static String decodeValue(String raw, char type) {
        if (type != 'S' || raw.length() < 2 || raw.charAt(0) != '"' || raw.charAt(raw.length() - 1) != '"') {
            return raw;
        }
        return raw.substring(1, raw.length() - 1)
                .replace("\\\\", "\\")
                .replace("\\\"", "\"");
    }

    private static String formatValue(ConfigEntry entry) {
        if (entry.type != 'S') {
            return entry.value;
        }
        return "\"" + entry.value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
