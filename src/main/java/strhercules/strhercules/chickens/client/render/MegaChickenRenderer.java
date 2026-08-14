package strhercules.chickens.client.render;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.entity.MegaChicken;
import strhercules.chickens.entity.MegaChickenSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Renderer for the supplied 3x Mega Chicken model and its tame-state equipment. */
public final class MegaChickenRenderer extends MobRenderer<MegaChicken, MegaChickenModel> {
    private static final ResourceLocation UNTAMED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "chickens", "textures/entity/megachicken/mega_chicken.png");
    private static final ResourceLocation TAMED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "chickens", "textures/entity/megachicken/mega_chicken_tamed.png");
    private static final ResourceLocation ROBOT_CHICKEN_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "chickens", "textures/entity/megachicken/robot_chicken.png");
    private static final ResourceLocation ROBOT_ROOSTER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "chickens", "textures/entity/megachicken/robot_rooster.png");
    private static final Set<ResourceLocation> AVAILABLE_TEXTURES = new HashSet<>();

    public MegaChickenRenderer(EntityRendererProvider.Context context) {
        super(context, new MegaChickenModel(context.bakeLayer(MegaChickenModel.LAYER_LOCATION)), 0.9F);
    }

    @Override
    public ResourceLocation getTextureLocation(MegaChicken chicken) {
        if (!chicken.isTamed()) {
            return UNTAMED_TEXTURE;
        }
        ResourceLocation robotTexture = switch (chicken.getRobotSkin()) {
            case MegaChicken.ROBOT_SKIN_CHICKEN -> ROBOT_CHICKEN_TEXTURE;
            case MegaChicken.ROBOT_SKIN_ROOSTER -> ROBOT_ROOSTER_TEXTURE;
            default -> null;
        };
        if (robotTexture != null && hasTexture(robotTexture)) {
            return robotTexture;
        }
        MegaChickenSkin directSkin = MegaChickenSkin.byId(chicken.getSkinId());
        if (directSkin != null && hasTexture(directSkin.texture())) {
            return directSkin.texture();
        }
        ResourceLocation appearance = appearanceTexture(chicken);
        return appearance == null ? TAMED_TEXTURE : appearance;
    }

    private static ResourceLocation appearanceTexture(MegaChicken chicken) {
        ChickensRegistryItem appearance = ChickensRegistry.getByType(chicken.getAppearanceType());
        if (appearance == null) {
            return null;
        }
        String path = appearance.getTexture().getPath();
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        if (name.endsWith(".png")) {
            name = name.substring(0, name.length() - 4);
        }
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(ChickensMod.MOD_ID,
                "textures/entity/megachicken/" + name.toLowerCase(Locale.ROOT) + ".png");
        return hasTexture(texture) ? texture : null;
    }

    private static boolean hasTexture(ResourceLocation texture) {
        if (AVAILABLE_TEXTURES.contains(texture)) {
            return true;
        }
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            AVAILABLE_TEXTURES.add(texture);
            return true;
        }
        return false;
    }

    @Override
    protected float getBob(MegaChicken chicken, float partialTicks) {
        if (!chicken.isFlightActive() || chicken.onGround()) {
            return 0.0F;
        }
        float flap = net.minecraft.util.Mth.lerp(partialTicks, chicken.oFlap, chicken.flap);
        float speed = net.minecraft.util.Mth.lerp(partialTicks, chicken.oFlapSpeed, chicken.flapSpeed);
        return (net.minecraft.util.Mth.sin(flap) + 1.0F) * speed;
    }
}
