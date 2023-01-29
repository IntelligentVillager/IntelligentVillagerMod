package com.sergio.ivillager.client;


import com.google.gson.JsonObject;
import com.sergio.ivillager.NPCVillager.NPCVillagerEntity;
import com.sergio.ivillager.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            ArrayList<NPCVillagerEntity> nearestVillager =
                    getNeareFacedVillager(player, getNearbyEntities(player,
                            player.getEntity().level, 6.0));

            if (nearestVillager.size() != 0) {

                // If no custom villager is interacted, it means the message is a normal
                // broadcast message, no need to capture and reproduce

                Map<String, Object> j0 = new HashMap<String, Object>();
                ArrayList<String> interactVillagerIDs = new ArrayList<String>();
                for (NPCVillagerEntity obj : nearestVillager) {
                    interactVillagerIDs.add(obj.getStringUUID());
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
    public static ArrayList<NPCVillagerEntity> getNeareFacedVillager(PlayerEntity player, List<Entity> nearByEntities) {
        Vector3d playerPos = player.position();
        Vector3d playerLook = player.getViewVector(0.5f);

        ArrayList<NPCVillagerEntity> nearCustomVillagerList = new ArrayList<>();

        for (Entity data : nearByEntities) {
            if (data instanceof NPCVillagerEntity) {
                Vector3d villagerPos = data.position();
                Vector3d playerToVillager = villagerPos.subtract(playerPos);
                double distance = playerToVillager.length();
                if (distance < 6 && playerLook.dot(playerToVillager) > 0) {
                    nearCustomVillagerList.add((NPCVillagerEntity) data);
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

                    if (fromEntity instanceof NPCVillagerEntity) {
                        NPCVillagerEntity f0 = (NPCVillagerEntity) fromEntity;

                        respondToPotentialActionResponse(originalMsg, toEntity, f0);

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

    private static void respondToPotentialActionResponse(String originalMsg, Entity toEntity, NPCVillagerEntity f0) {
        Pattern pattern1 = Pattern.compile("\\((.*?)\\)");

        Matcher macher = pattern1.matcher(originalMsg);
        while (macher.find()) {
            String action = macher.group(1).toLowerCase();
            Vector3d villagerPos = f0.position();
            Vector3d playerPos = toEntity.position();
            Vector3d playerToVillager = villagerPos.subtract(playerPos);
            double distance = playerToVillager.length();
            switch (action) {
                case "friendly pat":
                    f0.waveHands(Hand.MAIN_HAND, false);
                    break;
                case "wave hand":
                case "wave hands":
                    f0.waveHands(Hand.MAIN_HAND, false);
                    f0.waveHands(Hand.OFF_HAND, false);
                    break;
                case "punch":
                case "beat":
                    f0.waveHands(Hand.MAIN_HAND,true);
                    f0.getLookControl().setLookAt(toEntity
                            .position()
                            .add(0.0f, 1.0f, 0.0f));
                    if (distance <= 6.0f) {
                        toEntity.hurt(DamageSource.mobAttack(f0), 0.5f);  // server side effects
                    }
                    break;
            }
            if (action.startsWith("equip")) {
                String target = action.replace("equips", "").replace("equip", "").trim();
                List<Class<?>> clses = new ArrayList<>();
                switch (target) {
                    case "weapon":
                        clses.add(AxeItem.class);
                        clses.add(SwordItem.class);
                        break;
                    case "armor":
                        clses.add(ArmorItem.class);
                        break;
                    case "sword":
                        clses.add(SwordItem.class);
                        break;
                    case "axe":
                        clses.add(AxeItem.class);
                        break;
                }
                if (!f0.getInventory().isEmpty()) {
                    if (!clses.isEmpty()) {
                        for (int i = 0; i < f0.getInventory().getContainerSize(); i++) {
                            ItemStack itemStack = f0.getInventory().getItem(i);
                            if (!itemStack.isEmpty()) {
                                boolean equipped = false;
                                Item item = itemStack.getItem();
                                for (Class<?> cls : clses) {
                                    if (cls.isInstance(item)) {
                                        f0.equipItemIfPossible(itemStack);
                                        equipped = true;
                                        break;
                                    }
                                }
                                if (equipped) {
                                    break;
                                }
                            }
                        }
                    } else {
                        // maybe equip diamond_sword
                        for (int i = 0; i < f0.getInventory().getContainerSize(); i++) {
                            ItemStack itemStack = f0.getInventory().getItem(i);
                            if (!itemStack.isEmpty()) {
                                Item item = itemStack.getItem();
                                if (item.toString().contains(target)) {
                                    f0.equipItemIfPossible(itemStack);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (action.startsWith("attack with")) {
                String target = action.replace("attack with", "").trim();
                f0.waveHands(Hand.MAIN_HAND,true);
                f0.getLookControl().setLookAt(toEntity
                        .position()
                        .add(0.0f, 1.0f, 0.0f));
                if (distance <= 6.0f) {
                    boolean didDamage = false;
                    // get sword
                    for (ItemStack itemStack : f0.getHandSlots() ){
                        if (itemStack.isEmpty()) {
                            continue;
                        }
                        Item item = itemStack.getItem();
                        float damage = 0f;
                        if (item instanceof SwordItem) {
                            damage = ((SwordItem) itemStack.getItem()).getDamage();
                            toEntity.hurt(DamageSource.mobAttack(f0), damage);
                        } else if (item instanceof ToolItem) {
                            damage = ((ToolItem) item).getAttackDamage();
                        }
                        if (damage > 0) {
                            didDamage = true;
                            toEntity.hurt(DamageSource.mobAttack(f0), damage);
                            break;
                        }
                    }
                    if (!didDamage) {
                        toEntity.hurt(DamageSource.mobAttack(f0), 0.5f);
                    }
                }
            }


            if (action.startsWith("eat")) {
                String target = action.replace("eat", "").trim();
                if (!f0.getInventory().isEmpty()) {
                    // maybe equip diamond_sword
                    for (int i = 0; i < f0.getInventory().getContainerSize(); i++) {
                        ItemStack itemStack = f0.getInventory().getItem(i);
                        if (!itemStack.isEmpty()) {
                            Item item = itemStack.getItem();
                            if (item.isEdible() && item.toString().contains(target)) {
                                f0.equipItemIfPossible(itemStack);
                                f0.playEatSound(itemStack);
                                f0.eatAndDigestFood();
                                break;
                            }
                        }
                    }
                }
            }

            if (action.startsWith("give away") || action.startsWith("give")) {
                String target = action.replace("give away", "").trim().replace("give", "").trim();
                if (!f0.getInventory().isEmpty()) {
                    // maybe equip diamond_sword
                    for (int i = 0; i < f0.getInventory().getContainerSize(); i++) {
                        ItemStack itemStack = f0.getInventory().getItem(i);
                        if (!itemStack.isEmpty()) {
                            Item item = itemStack.getItem();
                            if (item.toString().contains(target)) {
                                ItemStack targetItem = f0.getInventory().removeItem(i, 1);
                                if (!targetItem.isEmpty()) {
                                    toEntity.setItemSlot(EquipmentSlotType.MAINHAND, targetItem);
                                    f0.waveHands(Hand.MAIN_HAND, false);
                                    f0.playTalkSound();
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

//    private static void respondToPotentialActionResponse(String originalMsg, Entity toEntity, NPCVillagerEntity f0) {
//        Pattern pattern = Pattern.compile("\\(.*\\)");
//        boolean hasParentheses = pattern.matcher(originalMsg).find();
//        f0.waveHands(Hand.MAIN_HAND, false);
//        f0.waveHands(Hand.OFF_HAND, false);
//        if (toEntity instanceof PlayerEntity) {
//            PlayerEntity player = (PlayerEntity) toEntity;
//            MinecraftServer server = player.getServer();
//            if (server != null) {
//                ServerPlayerEntity sp = server.getPlayerList().getPlayer(player.getUUID());
//                sp.hurt(DamageSource.mobAttack(f0), 10.5f);
//            }
//            for (ItemStack item : f0.getHandSlots()) {
//                if (!item.isEmpty()) {
//                    f0.getItemInHand(Hand.MAIN_HAND)
//                }
//            }
//            f0.getInventory().getItem(0).use(toEntity.getCommandSenderWorld(), player, Hand.MAIN_HAND);
//            player.hurt(DamageSource.mobAttack(f0), 0.5f);
//        }
//        if (hasParentheses) {
//            if (originalMsg.contains("(friendly pat)")) {
//                f0.waveHands(Hand.MAIN_HAND, false);
//                DamageSource d0 = new EntityDamageSource("mob", f0);
//                toEntity.hurt(d0, 0.5f);  }
//
//            if (originalMsg.contains("(wave hands)") || originalMsg.contains("(wave hand)")) {
//                f0.waveHands(Hand.MAIN_HAND, false);
//                f0.waveHands(Hand.OFF_HAND, false);
//                DamageSource d0 = new EntityDamageSource("mob", f0);
//                toEntity.hurt(d0, 0.5f);
//            }
//
//            if (originalMsg.contains("(punch)") || originalMsg.contains("(beat)")) {
//                f0.waveHands(Hand.MAIN_HAND,true);
//                DamageSource d0 = new EntityDamageSource("mob", f0);
//                toEntity.hurt(d0, 0.5f);
//            }
//        }
//    }

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
            event.setCanceled(true);
            return;
        }
        ITextComponent errorString =
                new StringTextComponent(originalMsg).setStyle(Style.EMPTY.withColor(TextFormatting.DARK_RED));
        event.setMessage(errorString);
        event.setCanceled(false);
    }

    @OnlyIn(Dist.CLIENT)
    private static TextComponent getTextComponent(String originalMsg, Entity toEntity, NPCVillagerEntity f0) {
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
}
