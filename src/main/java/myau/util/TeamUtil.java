package myau.util;

import myau.Myau;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class TeamUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isEntityLoaded(Entity entity) {
        if (entity == null) return false;
        return TeamUtil.mc.theWorld.loadedEntityList.contains(entity);
    }

    public static List<Entity> getLoadedEntitiesSorted() {
        return TeamUtil.mc.theWorld.loadedEntityList.stream().sorted((entity1, entity2) -> {
            double dist1 = mc.getRenderManager().getDistanceToCamera(entity1.posX, entity1.posY, entity1.posZ);
            double dist2 = mc.getRenderManager().getDistanceToCamera(entity2.posX, entity2.posY, entity2.posZ);
            if (dist1 < dist2) {
                return 1;
            }
            if (dist1 > dist2) {
                return -1;
            }
            return entity1.getUniqueID().toString().compareTo(entity2.getUniqueID().toString());
        }).collect(Collectors.toList());
    }

    public static float getHealthScore(EntityLivingBase entityLivingBase) {
        return entityLivingBase.getHealth() * (20.0f / (float) entityLivingBase.getTotalArmorValue());
    }

    public static String stripName(Entity entity) {
        return entity.getDisplayName().getFormattedText().replaceAll("§\\S$", "").replaceAll("(?i)§r", "§f").trim();
    }

    public static Color getTeamColor(EntityPlayer player, float alpha) {
        int colorCode = 0xFFFFFF;
        ScorePlayerTeam playerTeam = (ScorePlayerTeam) player.getTeam();
        if (playerTeam != null) {
            String colorPrefix = FontRenderer.getFormatFromString(playerTeam.getColorPrefix());
            if (colorPrefix.length() >= 2) {
                colorCode = TeamUtil.mc.fontRendererObj.getColorCode(colorPrefix.charAt(1));
            }
        }
        return new Color(colorCode & 0xFFFFFF | (int)(alpha * 255) << 24, true);
    }

    public static boolean isBot(EntityPlayer player) {
        if (player == TeamUtil.mc.thePlayer) {
            return false;
        }
        NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(player.getName());
        if (playerInfo == null) {
            return true;
        }
        if (!ServerUtil.isHypixel()) return false;
        if (player.getName().startsWith("§k")) {
            return player.isInvisible();
        }
        if (playerInfo.getResponseTime() < 1) {
            return true;
        }
        ScorePlayerTeam playerTeam = playerInfo.getPlayerTeam();
        if (playerTeam == null) return false;
        if (!playerTeam.getTeamName().isEmpty()) return false;
        return playerTeam.getColorPrefix().equals("§c");
    }

    public static boolean isSameTeam(EntityPlayer player) {
        if (player == TeamUtil.mc.thePlayer) {
            return true;
        }
        NetworkPlayerInfo selfInfo = mc.getNetHandler().getPlayerInfo(TeamUtil.mc.thePlayer.getUniqueID());
        if (selfInfo == null) {
            return false;
        }
        ScorePlayerTeam selfTeam = selfInfo.getPlayerTeam();
        if (selfTeam == null) {
            return false;
        }
        NetworkPlayerInfo targetInfo = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
        if (targetInfo == null) {
            return false;
        }
        ScorePlayerTeam targetTeam = targetInfo.getPlayerTeam();
        if (targetTeam == null) {
            return false;
        }
        return selfTeam.getColorPrefix().equals(targetTeam.getColorPrefix());
    }

    public static boolean hasTeamColor(EntityLivingBase entity) {
        if (entity == TeamUtil.mc.thePlayer) {
            return true;
        }
        NetworkPlayerInfo selfInfo = mc.getNetHandler().getPlayerInfo(TeamUtil.mc.thePlayer.getUniqueID());
        if (selfInfo == null) {
            return false;
        }
        ScorePlayerTeam selfTeam = selfInfo.getPlayerTeam();
        if (selfTeam == null) {
            return false;
        }
        if (selfTeam.getColorPrefix().length() < 2) {
            return false;
        }
        EntityLivingBase nearestArmorStand = TeamUtil.mc.theWorld.findNearestEntityWithinAABB(EntityArmorStand.class, entity.getEntityBoundingBox(), entity);
        if (nearestArmorStand != null) {
            return nearestArmorStand.getName().contains(selfTeam.getColorPrefix().substring(0, 2));
        }
        return false;
    }

    public static boolean isShop(EntityLivingBase entity) {
        if (entity == TeamUtil.mc.thePlayer) {
            return false;
        }
        EntityLivingBase armorStand = TeamUtil.mc.theWorld.findNearestEntityWithinAABB(EntityArmorStand.class, entity.getEntityBoundingBox(), entity);
        if (armorStand == null) return false;
        String displayName = armorStand.getName();
        if (displayName.contains("RIGHT CLICK")) return true;
        if (displayName.contains("ITEM SHOP")) return true;
        if (displayName.contains("UPGRADES")) return true;
        if (displayName.contains("BANKER")) return true;
        return displayName.contains("STREAK POWERS");
    }

    public static boolean isFriend(EntityPlayer player) {
        return Myau.friendManager.isFriend(player.getName());
    }

    public static boolean isTarget(EntityPlayer player) {
        return Myau.targetManager.isFriend(player.getName());
    }
}
