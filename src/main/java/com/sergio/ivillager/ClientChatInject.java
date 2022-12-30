package com.sergio.ivillager;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sergio.ivillager.NPCVillager.CustomEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@Mod.EventBusSubscriber
public class ClientChatInject {

    public static final Logger LOGGER = LogManager.getLogger(ClientChatInject.class);

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onChatMessage(ClientChatEvent event) {
        // Get the message that was sent
        String message = event.getMessage();
        if (message.startsWith("/")) {
        } else {
            PlayerEntity player = getCurrentPlayer();
            ArrayList<CustomEntity> nearestVillager =
                    getNeareFacedVillager(player, getNearbyEntities(player,
                            player.getEntity().level, 6.0));

            if (nearestVillager.size() != 0) {

                // If no custom villager is interacted, it means the message is a normal
                // broadcast message, no need to capture and reproduce

                Map<String, Object> j0 = new HashMap<String, Object>();
                ArrayList<String> interactVillagerIDs = new ArrayList<String>();
                for (CustomEntity obj : nearestVillager) {
                    interactVillagerIDs.add(obj.getStringUUID());

                    // Set the status on the client side
                    // TODO: Status maintainence
                    obj.setIsTalkingToPlayer(player);
                    obj.goalSelector.disableControlFlag(Goal.Flag.MOVE);
                    obj.setProcessingMessage(true);
                }
                j0.put("interacted",interactVillagerIDs);
                j0.put("msg",message);

                // change event
                String modifiedMessage = String.format("<villager command>%s", Utils.JsonConverter.encodeMapToJsonString(j0));

                LOGGER.warn(String.format("[CLIENT] Sending message: %s", modifiedMessage));

                event.setMessage(modifiedMessage);
                event.setCanceled(false);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static ArrayList<CustomEntity> getNeareFacedVillager(PlayerEntity player, List<Entity> nearByEntities) {
        Vector3d playerPos = player.position();
        Vector3d playerLook = player.getViewVector(0.5f);

        ArrayList<CustomEntity> nearCustomVillagerList = new ArrayList<>();

        for (Entity data : nearByEntities) {
            if (data instanceof CustomEntity) {
                Vector3d villagerPos = data.position();
                Vector3d playerToVillager = villagerPos.subtract(playerPos);
                double distance = playerToVillager.length();
                if (distance < 6 && playerLook.dot(playerToVillager) > 0) {
                    nearCustomVillagerList.add((CustomEntity) data);
                }
            }
        }
        LOGGER.warn(String.format("[CLIENT] %d villagers in client world instance, %d in " +
                "interactive " +
                "range", nearByEntities.size(),nearCustomVillagerList.size()));

        return nearCustomVillagerList;
    }

    @OnlyIn(Dist.CLIENT)
    public static PlayerEntity getCurrentPlayer() {
        return Minecraft.getInstance().player;
    }

    @OnlyIn(Dist.CLIENT)
    public static List<Entity> getNearbyEntities(PlayerEntity player, World world, double radius) {
        double x0 = player.position().x;
        double y0 = player.position().y;
        double z0 = player.position().z;
        AxisAlignedBB boundingBox = new AxisAlignedBB(x0 - radius, y0 - radius, z0 - radius, x0 + radius, y0 + radius, z0 + radius);
        return world.getEntities(player, boundingBox);
    }

    @OnlyIn(Dist.CLIENT)
    public static Boolean isPlayerEntitySelf(int entityID) {
        return getCurrentPlayer().getId() == entityID;
    }

    @OnlyIn(Dist.CLIENT)
    public static Boolean isEntityInPlayerInteractiveRange(Entity entity) {
        List<Entity> n0 = getNearbyEntities(getCurrentPlayer(),
                getCurrentPlayer().getEntity().level, 8.0);
        if (n0.contains(entity)) {
            return true;
        }
        return false;
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onClientReceivedChat(ClientChatReceivedEvent event){
        LOGGER.warn(String.format("[CLIENT] Received message: %s from sender: %s",
                event.getMessage().getString(), event.getSenderUUID().toString()));

        try {
            // Get the message that was received
            ITextComponent message = event.getMessage();

            String rawMsg = message.getString();
            if (rawMsg.contains("<villager command>")) {
                LOGGER.error("Exception message received (Message should not be received)");
                // Server has already taken care of all the messages
                event.setCanceled(true);
            } else if (rawMsg.contains("<villager response>")) {
                JsonObject rawMsgJsonObject =
                        Utils.JsonConverter.encodeStringToJson(rawMsg.replace("<villager response>", ""));

                String originalMsg = rawMsgJsonObject.get("msg").getAsString();
                int fromEntityID = rawMsgJsonObject.get("from_entity").getAsInt();
                int toEntityID = rawMsgJsonObject.get("to_entity").getAsInt();
                int code = rawMsgJsonObject.get("code").getAsInt();

                if (code != 0) {
                    Entity fromEntity = Minecraft.getInstance().level.getEntity(fromEntityID);
                    Entity toEntity = Minecraft.getInstance().level.getEntity(toEntityID);

                    if (abandonMessageOutsideRange(event, fromEntityID, toEntityID, fromEntity, toEntity)) return;

                    if (fromEntity instanceof CustomEntity) {
                        CustomEntity f0 = (CustomEntity) fromEntity;

                        controlVillagerLocally(originalMsg, toEntityID, f0);

                        TextComponent messageComponent = getTextComponent(originalMsg, toEntity, f0);

                        event.setMessage(messageComponent);
                        event.setCanceled(false);
                    } else {
                        errorMSG(event, originalMsg, toEntityID);
                    }
                } else {
                    // Error Message
                    errorMSG(event, originalMsg, toEntityID);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
            event.setCanceled(true);
            e.printStackTrace();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean abandonMessageOutsideRange (ClientChatReceivedEvent event,
                                                      int fromEntityID, int toEntityID, Entity fromEntity, Entity toEntity) {
        if (fromEntityID != getCurrentPlayer().getId() && toEntityID != getCurrentPlayer().getId()) {
            if (!isEntityInPlayerInteractiveRange(fromEntity) || !isEntityInPlayerInteractiveRange(toEntity)) {
                event.setCanceled(true);
                return true;
            }
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    private static void errorMSG(ClientChatReceivedEvent event, String originalMsg, int toEntityID) {
        if (!isPlayerEntitySelf(toEntityID)) {
            return;
        }
        ITextComponent errorString =
                new StringTextComponent(originalMsg).setStyle(Style.EMPTY.withColor(TextFormatting.DARK_RED));
        event.setMessage(errorString);
        event.setCanceled(false);
    }

    @OnlyIn(Dist.CLIENT)
    private static TextComponent getTextComponent(String originalMsg, Entity toEntity, CustomEntity f0) {
        ITextComponent nameString = new StringTextComponent(String.format("<%s>",
                f0.getName().getString()))
                .setStyle(Style.EMPTY.withColor(TextFormatting.BLUE));

        ITextComponent targetNameString = new StringTextComponent(String.format(
                " @%s ",
                toEntity.getName().getString()))
                .setStyle(Style.EMPTY.withBold(true).withColor(TextFormatting.YELLOW));

        ITextComponent contentString = new StringTextComponent(originalMsg)
                .setStyle(Style.EMPTY);

        TextComponent messageComponent = new TextComponent() {
            @Override
            public TextComponent plainCopy() {
                return null;
            }
        };

        messageComponent.append(nameString);
        messageComponent.append(targetNameString);
        messageComponent.append(contentString);
        return messageComponent;
    }

    @OnlyIn(Dist.CLIENT)
    private static void controlVillagerLocally(String originalMsg, int toEntityID, CustomEntity f0) {
        if (isPlayerEntitySelf(toEntityID)) {
            f0.getLookControl().setLookAt(getCurrentPlayer().position());
            if (originalMsg.startsWith("*") && originalMsg.endsWith("*")) {
                f0.eatAndDigestFood();
                f0.setJumping(true);
            }
            f0.playTalkSound();
            f0.setProcessingMessage(false);
        }
    }
}
